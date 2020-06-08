package org.intocps.maestro.plugin.InitializerNew;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.plugin.IMaestroUnfoldPlugin;
import org.intocps.maestro.plugin.IPluginConfiguration;
import org.intocps.maestro.plugin.InitializerNew.ConversionUtilities.BooleanUtils;
import org.intocps.maestro.plugin.InitializerNew.ConversionUtilities.ImmutableMapper;
import org.intocps.maestro.plugin.InitializerNew.ConversionUtilities.LongUtils;
import org.intocps.maestro.plugin.InitializerNew.Spec.StatementContainer;
import org.intocps.maestro.plugin.UnfoldException;
import org.intocps.maestro.plugin.env.ISimulationEnvironment;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.maestro.plugin.env.fmi2.ComponentInfo;
import org.intocps.maestro.plugin.env.fmi2.RelationVariable;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.topologicalsorting.TarjanGraph;
import org.intocps.topologicalsorting.data.AcyclicDependencyResult;
import org.intocps.topologicalsorting.data.CyclicDependencyResult;
import org.intocps.topologicalsorting.data.Edge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.jdk.CollectionConverters;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class InitializerNew implements IMaestroUnfoldPlugin {
    final static Logger logger = LoggerFactory.getLogger(InitializerNew.class);

    final AFunctionDeclaration f1 = MableAstFactory.newAFunctionDeclaration(new LexIdentifier("initialize", null),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("startTime")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("endTime"))), MableAstFactory.newAVoidType());

    private final HashSet<ModelDescription.ScalarVariable> portsAlreadySet = new HashSet<>();
    Config config;


    public InitializerNew() {
    }

    @Override
    public String getName() {
        return InitializerNew.class.getSimpleName();
    }

    @Override
    public String getVersion() {
        return "0.0.0";
    }

    @Override
    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return Stream.of(f1).collect(Collectors.toSet());
    }

    @Override
    public PStm unfold(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config, ISimulationEnvironment env,
            IErrorReporter errorReporter) throws UnfoldException {
        logger.info("Unfolding: {}", declaredFunction.toString());

        verifyArguments(formalArguments, env);
        final List<LexIdentifier> knownComponentNames = extractComponentNames(formalArguments);

        StatementContainer.reset();

        var sc = StatementContainer.getInstance();
        sc.startTime = formalArguments.get(1).clone();
        sc.endTime = formalArguments.get(2).clone();


        //Setup experiment for all components
        logger.debug("Setup experiment for all components");
        knownComponentNames.forEach(comp -> {
            sc.createSetupExperimentStatement(comp.getText(), false, 0.0, true);
        });

        //All connection - Only relations in the fashion InputToOutput is necessary since the OutputToInputs are just a dublicated of this
        Set<UnitRelationship.Relation> relations =
                env.getRelations(knownComponentNames).stream().filter(o -> o.getDirection() == UnitRelationship.Relation.Direction.OutputToInput)
                        .collect(Collectors.toSet());

        //Find the right order to instantiate dependentPorts and make sure where doesn't exist any cycles in the connections
        List<UnitRelationship.Variable> instantiationOrder = findInstantiationOrder(relations);

        instantiationOrder.forEach(o -> {
            var i = o.scalarVariable.scalarVariable.derivativesDependencies;
        });

        //Set variables for all components in IniPhase
        ManipulateComponentsVariables(env, knownComponentNames, sc, PhasePredicates.IniPhase(), false);

        //Enter initialization Mode
        logger.debug("Enter initialization Mode");
        knownComponentNames.forEach(comp -> {
            sc.enterInitializationMode(comp.getText());
        });

        //Set variables INPhase and IniePhase
        ManipulateComponentsVariables(env, knownComponentNames, sc, PhasePredicates.InPhase().or(PhasePredicates.IniePhase()), false);

        //Get variables INIThase
        ManipulateComponentsVariables(env, knownComponentNames, sc, PhasePredicates.InitPhase(), true);

        instantiationOrder.forEach(o -> {
            var useConverter = false;
            //This is safe because if the port is not present in a relation it would not have been included in the topological order
            var rel = relations.stream().filter(v -> v.getSource() == o || v.getTargets().containsValue(o)).findFirst().get();
            //the relation should be a one to one relation so just take the first one
            RelationVariable fromVar = rel.getTargets().values().iterator().next().scalarVariable;
            useConverter = (rel.getSource().scalarVariable.getScalarVariable().getType().type != fromVar.getScalarVariable().getType().type);
            setPort(o, sc, useConverter);
        });

        //Exit initialization Mode
        knownComponentNames.forEach(comp -> {
            sc.exitInitializationMode(comp.getText());
        });
        //sc.setInputOutputMapping();

        /*
        Set<UnitRelationship.Relation> outputRelations = relations.stream()
                .filter(r -> r.getDirection() == UnitRelationship.Relation.Direction.OutputToInput).collect(Collectors.toSet());


        Map<LexIdentifier, Map<ModelDescription.Types, List<ModelDescription.ScalarVariable>>> outputs = outputRelations.stream()
                .map(r -> r.getSource().scalarVariable.instance).distinct().collect(Collectors.toMap(Function.identity(),
                        s -> outputRelations.stream().filter(r -> r.getSource().scalarVariable.instance.equals(s))
                                .map(r -> r.getSource().scalarVariable.getScalarVariable()).collect(Collectors.groupingBy(sv -> sv.getType().type))));


        Set<UnitRelationship.Relation> inputRelations = relations.stream()
                .filter(r -> r.getDirection() == UnitRelationship.Relation.Direction.InputToOutput).collect(Collectors.toSet());

        Map<LexIdentifier, Map<ModelDescription.Types, List<ModelDescription.ScalarVariable>>> inputs = inputRelations.stream()
                .map(r -> r.getSource().scalarVariable.instance).distinct().collect(Collectors.toMap(Function.identity(),
                        s -> inputRelations.stream().filter(r -> r.getSource().scalarVariable.instance.equals(s))
                                .map(r -> r.getSource().scalarVariable.getScalarVariable()).collect(Collectors.groupingBy(sv -> sv.getType().type))));

        Consumer<List<PStm>> setAll = (list) ->
                //set inputs
                inputs.forEach((comp, map) -> map.forEach((type, vars) -> {
                    list.add(newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier("status")), newACallExp(
                            newADotExp(newAIdentifierExp((LexIdentifier) comp.clone()), newAIdentifierExp(getFmiGetName(type, UsageType.In))),
                            Arrays.asList(newAIdentifierExp(getVrefName(comp, type, UsageType.In)), newAIntLiteralExp(vars.size()),
                                    newAIdentifierExp(getBufferName(comp, type, UsageType.In))))));
                    checkStatus.accept(list);
                }));

        //get outputs
        Consumer<List<PStm>> getAll = (list) -> outputs.forEach((comp, map) -> map.forEach((type, vars) -> {
            list.add(newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier("status")),
                    newACallExp(newADotExp(newAIdentifierExp((LexIdentifier) comp.clone()), newAIdentifierExp(getFmiGetName(type, UsageType.Out))),
                            Arrays.asList(newAIdentifierExp(getVrefName(comp, type, UsageType.Out)), newAIntLiteralExp(vars.size()),
                                    newAIdentifierExp(getBufferName(comp, type, UsageType.Out))))));
        }));

        Consumer<List<PStm>> exchangeData = (list) -> inputRelations.forEach(r -> {

            int toIndex = inputs.get(r.getSource().scalarVariable.instance).get(r.getSource().scalarVariable.getScalarVariable().getType().type)
                    .stream().map(ModelDescription.ScalarVariable::getName).collect(Collectors.toList())
                    .indexOf(r.getSource().scalarVariable.scalarVariable.getName());

            AArrayStateDesignator to = newAArayStateDesignator(newAIdentifierStateDesignator(
                    getBufferName(r.getSource().scalarVariable.instance, r.getSource().scalarVariable.getScalarVariable().getType().type,
                            UsageType.In)), newAIntLiteralExp(toIndex));

            //the relation should be a one to one relation so just take the first one
            RelationVariable fromVar = r.getTargets().values().iterator().next().scalarVariable;
            PExp from = newAArrayIndexExp(newAIdentifierExp(getBufferName(fromVar.instance, fromVar.getScalarVariable().type.type, UsageType.Out)),
                    Collections.singletonList(newAIntLiteralExp(outputs.get(fromVar.instance).get(fromVar.getScalarVariable().getType().type).stream()
                            .map(ModelDescription.ScalarVariable::getName).collect(Collectors.toList()).indexOf(fromVar.scalarVariable.getName()))));

            if (r.getSource().scalarVariable.getScalarVariable().getType().type != fromVar.getScalarVariable().getType().type) {
                //ok the types are not matching, lets use a converter

                AArrayIndexExp toAsExp = newAArrayIndexExp(newAIdentifierExp(
                        getBufferName(r.getSource().scalarVariable.instance, r.getSource().scalarVariable.getScalarVariable().getType().type,
                                UsageType.In)), Arrays.asList(newAIntLiteralExp(toIndex)));

                list.add(newExternalStm(newACallExp(newAIdentifierExp(newAIdentifier(
                        "convert" + fromVar.getScalarVariable().getType().type + "2" + r.getSource().scalarVariable.getScalarVariable()
                                .getType().type)), Arrays.asList(from, toAsExp))));


            } else {
                list.add(newAAssignmentStm(to, from));
            }

        });
*/

        var statements = sc.getStatements();
        return newABlockStm(statements);
    }

    //Graph doesn't contain any loops and the ports gets passed in a topological sorted order
    private void setPort(UnitRelationship.Variable port, StatementContainer sc, boolean useConverter) {
        long[] scalarValueIndices = new long[]{port.scalarVariable.scalarVariable.getValueReference()};
        if (port.scalarVariable.scalarVariable.causality == ModelDescription.Causality.Output) {
            getValueFromPort(sc, port.scalarVariable.getInstance(), port.scalarVariable.scalarVariable.getType().type, scalarValueIndices);
            /*if (useConverter) {
                sc.
            }*/
            return;
        }
        setValueOnPort(sc, port.scalarVariable.getInstance(), port.scalarVariable.scalarVariable.getType().type,
                Collections.singletonList(port.scalarVariable.scalarVariable), scalarValueIndices);
    }

    private List<UnitRelationship.Variable> findInstantiationOrder(Set<UnitRelationship.Relation> relations) throws UnfoldException {
        var edges = relations.stream().map(o -> new Edge<UnitRelationship.Variable, UnitRelationship.Relation.InternalOrExternal>(o.getSource(),
                ImmutableMapper.convertSet(new HashSet<UnitRelationship.Variable>(o.getTargets().values())), o.getOrigin()))
                .collect(Collectors.toList());

        var graphSolver = new TarjanGraph(CollectionConverters.IterableHasAsScala(edges).asScala());

        var topologicalOrderToInstantiate = graphSolver.topologicalSort();
        if (topologicalOrderToInstantiate instanceof CyclicDependencyResult) {
            CyclicDependencyResult cycles = (CyclicDependencyResult) topologicalOrderToInstantiate;
            throw new UnfoldException("Cycles are present in the systems: " + cycles.cycle());
        }

        return scala.jdk.javaapi.CollectionConverters.asJava(((AcyclicDependencyResult) topologicalOrderToInstantiate).totalOrder());
    }

    private void ManipulateComponentsVariables(ISimulationEnvironment env, List<LexIdentifier> knownComponentNames, StatementContainer sc,
            Predicate<ModelDescription.ScalarVariable> predicate, Boolean isGet) {
        knownComponentNames.forEach(comp -> {
            //Maybe start time and end Time could be passed to the class
            ComponentInfo info = env.getUnitInfo(comp, Framework.FMI2);
            try {
                var variablesToInitialize =
                        info.modelDescription.getScalarVariables().stream().filter(predicate.and(o -> !portsAlreadySet.contains(o))).collect(Collectors.groupingBy(o -> o.getType().type));
                if (!variablesToInitialize.isEmpty()) {
                    variablesToInitialize.forEach((t, l) -> {
                        long[] scalarValueIndices = l.stream().map(o -> o.getValueReference()).map(Long.class::cast).collect(LongUtils.TO_LONG_ARRAY);
                        portsAlreadySet.addAll(l);
                        if (isGet) {
                            getValueFromPort(sc, comp, t, scalarValueIndices);
                        } else {
                            setValueOnPort(sc, comp, t, l, scalarValueIndices);
                        }
                    });
                }
            } catch (XPathExpressionException | IllegalAccessException | InvocationTargetException e) {
                logger.error(e.getMessage());
            }
        });
    }

    private void setValueOnPort(StatementContainer sc, LexIdentifier comp, ModelDescription.Types t, List<ModelDescription.ScalarVariable> l,
            long[] scalarValueIndices) {
        switch (t) {
            case Boolean: {
                boolean[] values = l.stream().map(o -> o.getType().start).map(Boolean.class::cast).collect(BooleanUtils.TO_BOOLEAN_ARRAY);
                sc.setBooleans(comp.getText(), scalarValueIndices, values);
            }
            break;
            case Real:
                double[] values = l.stream().mapToDouble(o -> Double.parseDouble(o.getType().start.toString())).toArray();
                sc.setReals(comp.getText(), scalarValueIndices, values);
                break;
            default:
                break;
        }
    }

    private void getValueFromPort(StatementContainer sc, LexIdentifier comp, ModelDescription.Types t, long[] scalarValueIndices) {
        switch (t) {
            case Boolean:
                sc.getBooleans(comp.getText(), scalarValueIndices);
                break;
            case Real:
                sc.getReals(comp.getText(), scalarValueIndices);
                break;
            default:
                break;
        }
    }

    private List<LexIdentifier> extractComponentNames(List<PExp> formalArguments) throws UnfoldException {
        List<LexIdentifier> knownComponentNames = null;
        if (formalArguments.get(0) instanceof AIdentifierExp) {
            LexIdentifier name = ((AIdentifierExp) formalArguments.get(0)).getName();
            ABlockStm containingBlock = formalArguments.get(0).getAncestor(ABlockStm.class);

            Optional<AVariableDeclaration> compDecl =
                    containingBlock.getBody().stream().filter(ALocalVariableStm.class::isInstance).map(ALocalVariableStm.class::cast)
                            .map(ALocalVariableStm::getDeclaration)
                            .filter(decl -> decl.getName().equals(name) && decl.getIsArray() && decl.getInitializer() != null).findFirst();

            if (compDecl.isEmpty()) {
                throw new UnfoldException("Could not find names for comps");
            }

            AArrayInitializer initializer = (AArrayInitializer) compDecl.get().getInitializer();

            knownComponentNames = initializer.getExp().stream().filter(AIdentifierExp.class::isInstance).map(AIdentifierExp.class::cast)
                    .map(AIdentifierExp::getName).collect(Collectors.toList());
        }

        if (knownComponentNames == null || knownComponentNames.isEmpty()) {
            throw new UnfoldException("No components found cannot fixed step with 0 components");
        }

        return knownComponentNames;
    }

    private void verifyArguments(List<PExp> formalArguments, ISimulationEnvironment env) throws UnfoldException {
        //maybe some of these tests are not necessary - but they are in my unit test
        if (formalArguments == null || formalArguments.size() != f1.getFormals().size()) {
            throw new UnfoldException("Invalid args");
        }
        if (env == null) {
            throw new UnfoldException("Simulation environment must not be null");
        }
    }

    @Override
    public boolean requireConfig() {
        return true;
    }

    @Override
    public IPluginConfiguration parseConfig(InputStream is) throws IOException {
        JsonNode root = new ObjectMapper().readTree(is);
        JsonNode configuration = root.get("configuration");
        JsonNode start_message = root.get("start_message");
        Config conf = new Config(configuration, start_message);
        return conf;
    }

    public static class Config implements IPluginConfiguration {

        public Config(JsonNode configuration, JsonNode start_message) {
            this.configuration = configuration;
            this.start_message = start_message;
        }

        private JsonNode configuration;

        private JsonNode start_message;

        public JsonNode getConfiguration() {
            return configuration;
        }

        public JsonNode getStart_message() {
            return start_message;
        }

    }


}



