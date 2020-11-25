package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.AExpressionStm;
import org.intocps.maestro.ast.node.AIdentifierExp;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.RelationVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.call;
import static org.intocps.maestro.ast.MableBuilder.newVariable;

class DerivativesHandler {

    final static Logger logger = LoggerFactory.getLogger(DerivativesHandler.class);
    final BiPredicate<Fmi2SimulationEnvironment, Fmi2SimulationEnvironment.Variable> canInterpolateInputsFilter = (env, v) -> {
        try {
            return ((ComponentInfo) env.getUnitInfo(v.scalarVariable.instance, Framework.FMI2)).modelDescription.getCanInterpolateInputs();
        } catch (XPathExpressionException e) {
            return false;
        }
    };
    final BiFunction<Fmi2SimulationEnvironment, LexIdentifier, Integer> maxOutputDerivativeOrder = (env, id) -> {
        try {
            return ((ComponentInfo) env.getUnitInfo(id, Framework.FMI2)).modelDescription.getMaxOutputDerivativeOrder();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            return 0;
        }
    };
    String globalCacheName = "derivatives";
    String globalDerInputBuffer = "der_input_buffer";
    Map<LexIdentifier, GetDerivativesInfo> derivativesGetInfo = new HashMap<>();
    private boolean allocated = false;
    private boolean requireArrayUtilUnload = false;
    private Map<Map.Entry<LexIdentifier, List<Fmi2SimulationEnvironment.Relation>>, LinkedHashMap<Fmi2SimulationEnvironment.Variable, Map.Entry<Fmi2SimulationEnvironment.Variable, GetDerivativesInfo>>>
            resolvedInputData;

    public List<PStm> allocateMemory(List<LexIdentifier> componentNames, Set<Fmi2SimulationEnvironment.Relation> inputRelations,
            Fmi2SimulationEnvironment env) {
        allocated = true;


        Set<Map<LexIdentifier, Fmi2SimulationEnvironment.Variable>> tmp =
                inputRelations.stream().filter(r -> canInterpolateInputsFilter.test(env, r.getSource()))
                        .map(Fmi2SimulationEnvironment.Relation::getTargets).collect(Collectors.toSet());


        Map<LexIdentifier, List<Fmi2SimulationEnvironment.Variable>> vars = new HashMap<>();
        for (Map<LexIdentifier, Fmi2SimulationEnvironment.Variable> map : tmp) {
            for (Map.Entry<LexIdentifier, Fmi2SimulationEnvironment.Variable> entry : map.entrySet()) {
                vars.computeIfAbsent(entry.getKey(), key -> new Vector<>()).add(entry.getValue());
            }
        }

        for (Map.Entry<LexIdentifier, List<Fmi2SimulationEnvironment.Variable>> entry : vars.entrySet()) {
            entry.getValue().sort(Comparator.comparing(v -> v.getScalarVariable().getScalarVariable().valueReference));
        }


        if (vars.isEmpty()) {
            return new Vector<>();
        }

        List<PStm> statements = new Vector<>();
        List<Integer> perInstanceSizes = new Vector<>();


        for (LexIdentifier name : componentNames) {
            vars.entrySet().stream().filter(f -> f.getKey().getText().equals(name.getText())).findFirst().ifPresent(f -> {

                LexIdentifier id = f.getKey();
                int order = maxOutputDerivativeOrder.apply(env, id);
                GetDerivativesInfo varDerInfo = new GetDerivativesInfo();
                varDerInfo.varMaxOrder = order;

                for (int i = 0; i < f.getValue().size(); i++) {
                    Fmi2SimulationEnvironment.Variable var = f.getValue().get(i);
                    varDerInfo.varStartIndex.put(var, varDerInfo.varStartIndex.size() * order);
                }


                int size = f.getValue().size() * order;
                perInstanceSizes.add(size);

                varDerInfo.valueDestIdentifier = newAArrayIndexExp(newAIdentifierExp(newAIdentifier(globalCacheName)),
                        Arrays.asList(newAIntLiteralExp(componentNames.indexOf(name))));
                String orderArrayName = id.getText() + "_der_order";
                statements.add(newVariable(orderArrayName, newAIntNumericPrimitiveType(), IntStream.range(0, f.getValue().size())
                        .mapToObj(v -> IntStream.range(1, order + 1).mapToObj(MableAstFactory::newAIntLiteralExp)).flatMap(Function.identity())
                        .collect(Collectors.toList())));
                varDerInfo.orderArrayId = orderArrayName;
                String varSelectName = id.getText() + "_der_select";
                statements.add(newVariable(varSelectName, newUIntType(), f.getValue().stream().flatMap(v -> IntStream.range(1, order + 1)
                        .mapToObj(o -> newAIntLiteralExp((int) v.getScalarVariable().getScalarVariable().valueReference)))
                        .collect(Collectors.toList())));
                varDerInfo.valueSelectArrayId = varSelectName;
                derivativesGetInfo.put(id, varDerInfo);

            });
        }

        statements.add(0, newVariable(globalCacheName, newARealNumericPrimitiveType(), componentNames.size() - 1,
                perInstanceSizes.stream().mapToInt(i -> i).max().orElse(0)));


        //allocate for input
        statements.addAll(allocateForInput(inputRelations, env));


        return statements;
    }

    private List<PStm> allocateForInput(Set<Fmi2SimulationEnvironment.Relation> inputRelations, Fmi2SimulationEnvironment env) {
        resolvedInputData = inputRelations.stream().filter(r -> canInterpolateInputsFilter.test(env, r.getSource()))
                .collect(Collectors.groupingBy(s -> s.getSource().getScalarVariable().instance)).entrySet().stream().collect(Collectors
                        .toMap(Function.identity(), mapped -> mapped.getValue().stream()
                                .sorted(Comparator.comparing(map -> map.getSource().getScalarVariable().getScalarVariable().valueReference))
                                .collect(Collectors.toMap(Fmi2SimulationEnvironment.Relation::getSource, map -> {

                                    //the relation should be a one to one relation so just take the first one
                                    Fmi2SimulationEnvironment.Variable next = map.getTargets().values().iterator().next();
                                    RelationVariable fromVar = next.scalarVariable;

                                    GetDerivativesInfo fromVarDerivativeInfo = derivativesGetInfo.get(fromVar.instance);
                                    if (fromVarDerivativeInfo != null) {
                                        logger.trace("Derivative mapping {}.{} to {}.{}", fromVar.instance, fromVar.scalarVariable.name,
                                                map.getSource().getScalarVariable().instance,
                                                map.getSource().getScalarVariable().scalarVariable.name);
                                    }

                                    return Map.entry(next, fromVarDerivativeInfo);


                                }, (e1, e2) -> e1, LinkedHashMap::new))));


        //calculate input buffer size
        int size = resolvedInputData.values().stream()
                .mapToInt(variableEntryLinkedHashMap -> variableEntryLinkedHashMap.values().stream().mapToInt(v -> v.getValue().varMaxOrder).sum())
                .sum();


        List<PStm> allocationStatements = Stream.concat(Stream.of(newVariable("der_input_buffer", newARealNumericPrimitiveType(), size)),
                resolvedInputData.entrySet().stream().flatMap(map -> {

                    LinkedHashMap<Fmi2SimulationEnvironment.Variable, Map.Entry<Fmi2SimulationEnvironment.Variable, GetDerivativesInfo>> resolved =
                            map.getValue();

                    List<Integer> inputSelectIndices = resolved.entrySet().stream().flatMap(
                            m -> IntStream.range(1, m.getValue().getValue().varMaxOrder + 1)
                                    .mapToObj(i -> Long.valueOf(m.getKey().getScalarVariable().scalarVariable.valueReference).intValue()))
                            .collect(Collectors.toList());
                    List<Integer> inputOrders =
                            resolved.entrySet().stream().flatMap(m -> IntStream.range(1, m.getValue().getValue().varMaxOrder + 1).mapToObj(i -> i))
                                    .collect(Collectors.toList());

                    LexIdentifier name = map.getKey().getKey();

                    return Stream.of(newVariable("der_input_select_" + name.getText(), newAIntNumericPrimitiveType(),
                            inputSelectIndices.stream().map(MableAstFactory::newAIntLiteralExp).collect(Collectors.toList())),

                            newVariable("der_input_order" + "_" + name.getText(), newAIntNumericPrimitiveType(),
                                    inputOrders.stream().map(MableAstFactory::newAIntLiteralExp).collect(Collectors.toList())));

                })).collect(Collectors.toList());
        requireArrayUtilUnload = true;
        allocationStatements.add(0, newVariable("util", newANameType("ArrayUtil"), newALoadExp(Arrays.asList(newAStringLiteralExp("ArrayUtil")))));
        return allocationStatements;
    }

    public List<PStm> deallocate() {
        if (requireArrayUtilUnload) {
            return Collections.singletonList(newExpressionStm(newUnloadExp(Collections.singletonList(newAIdentifierExp("util")))));
        }
        return Collections.emptyList();
    }

    public List<PStm> get(String errorStateLocation) throws InstantiationException {
        if (!allocated) {
            throw new InstantiationException("Must be allocated first");
        }
        return this.get(errorStateLocation, null);
    }

    /**
     * @param errorStateLocation
     * @param componentNamesFilter null for all otherwise filter to only get from these
     * @return
     */
    public List<PStm> get(String errorStateLocation, List<LexIdentifier> componentNamesFilter) throws InstantiationException {
        if (!allocated) {
            throw new InstantiationException("Must be allocated first");
        }
        if (derivativesGetInfo == null) {
            return new Vector<>();
        }

        List<PStm> stmts = new Vector<>();

        for (Map.Entry<LexIdentifier, GetDerivativesInfo> map : derivativesGetInfo.entrySet()) {
            LexIdentifier id = map.getKey();
            if (componentNamesFilter != null && !componentNamesFilter.contains(id)) {
                continue;
            }
            GetDerivativesInfo info = map.getValue();


            AIdentifierExp object = newAIdentifierExp((LexIdentifier) id.clone());
            stmts.add(newExpressionStm(call(object, "getRealOutputDerivatives", newAIdentifierExp(info.valueSelectArrayId),
                    newAIntLiteralExp(info.varStartIndex.size() * info.varMaxOrder), newAIdentifierExp(info.orderArrayId),
                    info.valueDestIdentifier.clone())));
        }

        return stmts;
    }

    public List<PStm> set(String errorStateLocation) throws InstantiationException {
        return set(errorStateLocation, null);
    }

    public List<PStm> set(String errorStateLocation, List<LexIdentifier> componentNamesFilter) throws InstantiationException {
        if (!allocated) {
            throw new InstantiationException("Must be allocated first");
        }
        if (resolvedInputData == null) {
            return new Vector<>();
        }

        return resolvedInputData.entrySet().stream().filter(m -> componentNamesFilter == null || componentNamesFilter.contains(m.getKey().getKey()))
                .flatMap(map -> {

                    AtomicInteger inputOffset = new AtomicInteger(0);
                    LexIdentifier name = map.getKey().getKey();

                    List<Integer> inputOrders =
                            map.getValue().entrySet().stream().map(m -> IntStream.range(1, m.getValue().getValue().varMaxOrder + 1).boxed())
                                    .flatMap(Function.identity()).collect(Collectors.toList());

                    //copy from der[0],start, count
                    Stream<AExpressionStm> copyStatements = map.getValue().entrySet().stream().map(pair -> {

                        GetDerivativesInfo from = pair.getValue().getValue();
                        //copy from der[0],start, count
                        Integer index = from.varStartIndex.get(pair.getValue().getKey());
                        logger.debug("Copying {} from index {} in ders", pair.getValue().getKey(), index);
                        PExp c = call(newAIdentifierExp("util"), "copyRealArray", from.valueDestIdentifier, newAIntLiteralExp(index),
                                newAIntLiteralExp(from.varMaxOrder), newARefExp(newAIdentifierExp(globalDerInputBuffer)),
                                newAIntLiteralExp(inputOffset.getAndAdd(from.varMaxOrder)));
                        logger.debug("{}", c);
                        return newExpressionStm(c);
                    });

                    PExp set = call(newAIdentifierExp((LexIdentifier) map.getKey().getKey().clone()), "setRealInputDerivatives",
                            newAIdentifierExp("der_input_select_" + name.getText()), newAIntLiteralExp(inputOrders.size()),
                            newAIdentifierExp("der_input_order_" + name.getText()), newAIdentifierExp(globalDerInputBuffer));
                    return Stream.concat(copyStatements, Stream.of(newExpressionStm(set)));

                }).collect(Collectors.toList());


    }

    class GetDerivativesInfo {
        String orderArrayId;
        String valueSelectArrayId;
        PExp valueDestIdentifier;
        Map<Fmi2SimulationEnvironment.Variable, Integer> varStartIndex = new HashMap<>();
        Integer varMaxOrder;
    }
}
