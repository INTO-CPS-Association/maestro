package org.intocps.maestro.plugin.InitializerNew;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.plugin.IMaestroUnfoldPlugin;
import org.intocps.maestro.plugin.IPluginConfiguration;
import org.intocps.maestro.plugin.InitializerNew.ConversionUtilities.BooleanUtils;
import org.intocps.maestro.plugin.InitializerNew.ConversionUtilities.LongUtils;
import org.intocps.maestro.plugin.InitializerNew.Spec.StatementContainer;
import org.intocps.maestro.plugin.UnfoldException;
import org.intocps.maestro.plugin.env.ISimulationEnvironment;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.maestro.plugin.env.UnitRelationship.Variable;
import org.intocps.maestro.plugin.env.fmi2.ComponentInfo;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.topologicalsorting.TarjanGraph;
import org.intocps.topologicalsorting.data.AcyclicDependencyResult;
import org.intocps.topologicalsorting.data.CyclicDependencyResult;
import org.intocps.topologicalsorting.data.Edge11;
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

        //All connections - Only relations in the fashion InputToOutput is necessary since the OutputToInputs are just a dublicated of this
        Set<UnitRelationship.Relation> relations =
                env.getRelations(knownComponentNames).stream().filter(o -> o.getDirection() == UnitRelationship.Relation.Direction.OutputToInput)
                        .collect(Collectors.toSet());

        //Find the right order to instantiate dependentPorts and make sure where doesn't exist any cycles in the connections
        List<UnitRelationship.Variable> instantiationOrder = findInstantiationOrder(relations);

        this.config = (Config) config;

        //Set variables for all components in IniPhase
        ManipulateComponentsVariables(env, knownComponentNames, sc, PhasePredicates.IniPhase(), false);

        //Enter initialization Mode
        logger.debug("Enter initialization Mode");
        knownComponentNames.forEach(comp -> {
            sc.enterInitializationMode(comp.getText());
        });

        var inputToOutputRelations =
                env.getRelations(knownComponentNames).stream().filter(o -> o.getDirection() == UnitRelationship.Relation.Direction.InputToOutput)
                        .collect(Collectors.toList());

        var inputOutMapping = createInputOutputMapping(inputToOutputRelations, env);
        sc.setInputOutputMapping(inputOutMapping);

        //Initialize the ports in the correct order based on the topological sorting
        instantiationOrder.forEach(o -> { initializePort(o, sc); });

        //Exit initialization Mode
        knownComponentNames.forEach(comp -> {
            sc.exitInitializationMode(comp.getText());
        });

        var statements = sc.getStatements();
        return newABlockStm(statements);
    }

    private Map<ModelConnection.ModelInstance, Map<ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>>> createInputOutputMapping(
            List<UnitRelationship.Relation> relations, ISimulationEnvironment env) {
        Map<ModelConnection.ModelInstance, Map<ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>>>
                inputToOutputMapping = new HashMap<>();

        relations.forEach(o -> {
            ComponentInfo infoSource = env.getUnitInfo(o.getSource().scalarVariable.getInstance(), Framework.FMI2);
            Map<ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>> entryMap =
                    new HashMap<>();
            o.getTargets().values().forEach(v -> {
                ComponentInfo infoTarget = env.getUnitInfo(v.scalarVariable.getInstance(), Framework.FMI2);
                entryMap.put(o.getSource().scalarVariable.getScalarVariable(),
                        new AbstractMap.SimpleEntry<>(new ModelConnection.ModelInstance(infoTarget.fmuIdentifier, v.scalarVariable.getInstance().getText()),
                                v.scalarVariable.scalarVariable));
                inputToOutputMapping
                        .put(new ModelConnection.ModelInstance(infoSource.fmuIdentifier, o.getSource().scalarVariable.getInstance().getText()),
                                entryMap);
            });
        });

        return inputToOutputMapping;
    }

    //Graph doesn't contain any loops and the ports gets passed in a topological sorted order
    private void initializePort(UnitRelationship.Variable port, StatementContainer sc) {
        long[] scalarValueIndices =
                Stream.of(port.scalarVariable.scalarVariable.getValueReference()).map(Long.class::cast).collect(LongUtils.TO_LONG_ARRAY);

        if (port.scalarVariable.scalarVariable.causality == ModelDescription.Causality.Output) {
            getValueFromPort(sc, port.scalarVariable.getInstance(), port.scalarVariable.scalarVariable.getType().type, scalarValueIndices);
            return;
        }
        setValueOnPort(sc, port.scalarVariable.getInstance(), port.scalarVariable.scalarVariable.getType().type,
                Collections.singletonList(port.scalarVariable.scalarVariable), scalarValueIndices);
    }

    //This method find the right instantiation order using the topological sort plugin. The plugin is in scala so some mapping between java and
    // scala is needed
    private List<UnitRelationship.Variable> findInstantiationOrder(Set<UnitRelationship.Relation> relations) throws UnfoldException {
        var externalRelations =
                relations.stream().filter(o -> o.getOrigin() == UnitRelationship.Relation.InternalOrExternal.External).collect(Collectors.toList());
        var internalRelations =
                relations.stream().filter(o -> o.getOrigin() == UnitRelationship.Relation.InternalOrExternal.Internal).collect(Collectors.toList());

        var edges = new Vector<Edge11<Variable, UnitRelationship.Relation.InternalOrExternal>>();
        externalRelations.forEach(o -> o.getTargets().values().forEach(e -> {
            edges.add(new Edge11(o.getSource(), e, o.getOrigin()));
        }));
        internalRelations.forEach(o -> o.getTargets().values().forEach(e -> {
            edges.add(new Edge11(o.getTargets().values().iterator().next(), e, o.getOrigin()));
        }));

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
                        info.modelDescription.getScalarVariables().stream().filter(predicate.and(o -> !portsAlreadySet.contains(o)))
                                .collect(Collectors.groupingBy(o -> o.getType().type));
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



