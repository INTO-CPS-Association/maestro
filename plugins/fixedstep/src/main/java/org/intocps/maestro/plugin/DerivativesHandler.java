package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
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
import static org.intocps.maestro.ast.MableBuilder.*;

class DerivativesHandler {
    final static Logger logger = LoggerFactory.getLogger(DerivativesHandler.class);
    final BiPredicate<FmiSimulationEnvironment, FmiSimulationEnvironment.Variable> canInterpolateInputsFilter = (env, v) -> {
        try {
            return ((ComponentInfo) env.getUnitInfo(v.scalarVariable.instance, Framework.FMI2)).modelDescription.getCanInterpolateInputs();
        } catch (XPathExpressionException e) {
            return false;
        }
    };

    final BiFunction<FmiSimulationEnvironment, LexIdentifier, Integer> maxOutputDerivativeOrder = (env, id) -> {
        try {
            return ((ComponentInfo) env.getUnitInfo(id, Framework.FMI2)).modelDescription.getMaxOutputDerivativeOrder();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            return 0;
        }
    };
    String globalCacheName = "derivatives";
    String globalDerInputBuffer = "der_input_buffer";
    //  Map<LexIdentifier, List<UnitRelationship.Variable>> vars = null;

    Map<LexIdentifier, GetDerivativesInfo> derivativesGetInfo = new HashMap<>();
    private Map<Map.Entry<LexIdentifier, List<FmiSimulationEnvironment.Relation>>, LinkedHashMap<FmiSimulationEnvironment.Variable, Map.Entry<FmiSimulationEnvironment.Variable, GetDerivativesInfo>>>
            resolvedInputData;

    public List<PStm> allocateMemory(List<LexIdentifier> componentNames, Set<FmiSimulationEnvironment.Relation> inputRelations,
            FmiSimulationEnvironment env) {


        Set<Map<LexIdentifier, FmiSimulationEnvironment.Variable>> tmp =
                inputRelations.stream().filter(r -> canInterpolateInputsFilter.test(env, r.getSource()))
                        .map(FmiSimulationEnvironment.Relation::getTargets).collect(Collectors.toSet());


        Map<LexIdentifier, List<FmiSimulationEnvironment.Variable>> vars = new HashMap<>();
        for (Map<LexIdentifier, FmiSimulationEnvironment.Variable> map : tmp) {
            for (Map.Entry<LexIdentifier, FmiSimulationEnvironment.Variable> entry : map.entrySet()) {
                vars.computeIfAbsent(entry.getKey(), key -> new Vector<>()).add(entry.getValue());
            }
        }

        for (Map.Entry<LexIdentifier, List<FmiSimulationEnvironment.Variable>> entry : vars.entrySet()) {
            entry.getValue().sort(Comparator.comparing(v -> v.getScalarVariable().getScalarVariable().valueReference));
        }


        if (vars.isEmpty()) {
            return new Vector<>();
        }

        List<PStm> statements = new Vector<>();
        statements.add(newVariable(globalCacheName, newARealNumericPrimitiveType(), componentNames.size()));

        for (LexIdentifier name : componentNames) {
            vars.entrySet().stream().filter(f -> f.getKey().getText().equals(name.getText())).findFirst().ifPresent(f -> {

                LexIdentifier id = f.getKey();
                int order = maxOutputDerivativeOrder.apply(env, id);
                GetDerivativesInfo varDerInfo = new GetDerivativesInfo();
                varDerInfo.varMaxOrder = order;

                for (int i = 0; i < f.getValue().size(); i++) {
                    FmiSimulationEnvironment.Variable var = f.getValue().get(i);
                    varDerInfo.varStartIndex.put(var, varDerInfo.varStartIndex.size() * order);
                }


                int size = f.getValue().size() * order;

                List<PStm> scope = new Vector<>();
                scope.add(newVariable("tmp", newARealNumericPrimitiveType(), size));

                varDerInfo.valueDestIdentifier = newAArrayIndexExp(newAIdentifierExp(newAIdentifier(globalCacheName)),
                        Arrays.asList(newAIntLiteralExp(componentNames.indexOf(name))));
                scope.add(arraySet(globalCacheName, componentNames.indexOf(name), newAIdentifierExp("tmp")));
                String orderArrayName = id.getText() + "_der_order";
                statements.add(newVariable(orderArrayName, newAIntNumericPrimitiveType(), IntStream.range(0, f.getValue().size())
                        .mapToObj(v -> IntStream.range(1, order + 1).mapToObj(MableAstFactory::newAIntLiteralExp)).flatMap(Function.identity())
                        .collect(Collectors.toList())));
                varDerInfo.orderArrayId = orderArrayName;
                String varSelectName = id.getText() + "_der_select";
                statements.add(newVariable(varSelectName, newAIntNumericPrimitiveType(), f.getValue().stream().flatMap(
                        v -> IntStream.range(1, order + 1)
                                .mapToObj(o -> newAIntLiteralExp((int) v.getScalarVariable().getScalarVariable().valueReference)))
                        .collect(Collectors.toList())));
                varDerInfo.valueSelectArrayId = varSelectName;
                derivativesGetInfo.put(id, varDerInfo);

                statements.add(newABlockStm(scope));

            });
        }


        //allocate for input
        statements.addAll(allocateForInput(inputRelations, env));


        return statements;
    }

    private List<PStm> allocateForInput(Set<FmiSimulationEnvironment.Relation> inputRelations, FmiSimulationEnvironment env) {
        resolvedInputData = inputRelations.stream().filter(r -> canInterpolateInputsFilter.test(env, r.getSource()))
                .collect(Collectors.groupingBy(s -> s.getSource().getScalarVariable().instance)).entrySet().stream().collect(Collectors
                        .toMap(Function.identity(), mapped -> mapped.getValue().stream()
                                .sorted(Comparator.comparing(map -> map.getSource().getScalarVariable().getScalarVariable().valueReference))
                                .collect(Collectors.toMap(FmiSimulationEnvironment.Relation::getSource, map -> {

                                    //the relation should be a one to one relation so just take the first one
                                    FmiSimulationEnvironment.Variable next = map.getTargets().values().iterator().next();
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


        return Stream.concat(Stream.of(newVariable("der_input_buffer", newARealNumericPrimitiveType(), size)),
                resolvedInputData.entrySet().stream().map(map -> {

                    LinkedHashMap<FmiSimulationEnvironment.Variable, Map.Entry<FmiSimulationEnvironment.Variable, GetDerivativesInfo>> resolved =
                            map.getValue();

                    List<Integer> inputSelectIndices = resolved.entrySet().stream()
                            .map(m -> IntStream.range(1, m.getValue().getValue().varMaxOrder + 1)
                                    .mapToObj(i -> Long.valueOf(m.getKey().getScalarVariable().scalarVariable.valueReference).intValue()))
                            .flatMap(Function.identity()).collect(Collectors.toList());
                    List<Integer> inputOrders =
                            resolved.entrySet().stream().map(m -> IntStream.range(1, m.getValue().getValue().varMaxOrder + 1).mapToObj(i -> i))
                                    .flatMap(Function.identity()).collect(Collectors.toList());

                    LexIdentifier name = map.getKey().getKey();

                    return Stream.of(newVariable("der_input_select_" + name.getText(), newAIntNumericPrimitiveType(),
                            inputSelectIndices.stream().map(MableAstFactory::newAIntLiteralExp).collect(Collectors.toList())),

                            newVariable("der_input_order" + "_" + name.getText(), newAIntNumericPrimitiveType(),
                                    inputOrders.stream().map(MableAstFactory::newAIntLiteralExp).collect(Collectors.toList())));

                }).flatMap(Function.identity())).collect(Collectors.toList());


    }

    public List<PStm> get(String errorStateLocation) {
        return this.get(errorStateLocation, null);
    }

    /**
     * @param errorStateLocation
     * @param componentNamesFilter null for all otherwise filter to only get from these
     * @return
     */
    public List<PStm> get(String errorStateLocation, List<LexIdentifier> componentNamesFilter) {
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
                    (PExp) info.valueDestIdentifier.clone())));
        }

        return stmts;
    }

    public List<PStm> set(String errorStateLocation) {
        return set(errorStateLocation, null);
    }

    public List<PStm> set(String errorStateLocation, List<LexIdentifier> componentNamesFilter) {
        if (resolvedInputData == null) {
            return new Vector<>();
        }

        return resolvedInputData.entrySet().stream().filter(m -> componentNamesFilter == null || componentNamesFilter.contains(m.getKey().getKey()))
                .map(map -> {

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
                        PExp c = call("copy", from.valueDestIdentifier, newAIntLiteralExp(index), newAIntLiteralExp(from.varMaxOrder),
                                newAIdentifierExp(globalDerInputBuffer), newAIntLiteralExp(inputOffset.getAndAdd(from.varMaxOrder)));
                        logger.debug("{}", c);
                        return newExpressionStm(c);
                    });

                    PExp set = call(newAIdentifierExp((LexIdentifier) map.getKey().getKey().clone()), "setRealInputDerivatives",
                            newAIdentifierExp("der_input_select_" + name.getText()), newAIntLiteralExp(inputOrders.size()),
                            newAIdentifierExp("der_input_order_" + name.getText()), newAIdentifierExp(globalDerInputBuffer));
                    return Stream.concat(copyStatements, Stream.of(newExpressionStm(set)));

                }).flatMap(Function.identity()).collect(Collectors.toList());


    }

    class GetDerivativesInfo {
        String orderArrayId;
        String valueSelectArrayId;
        PExp valueDestIdentifier;
        Map<FmiSimulationEnvironment.Variable, Integer> varStartIndex = new HashMap<>();
        Integer varMaxOrder;
    }
}
