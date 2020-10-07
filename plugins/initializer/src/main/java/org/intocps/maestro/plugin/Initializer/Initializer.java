package org.intocps.maestro.plugin.Initializer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.plugin.ExpandException;
import org.intocps.maestro.plugin.IMaestroExpansionPlugin;
import org.intocps.maestro.plugin.IPluginConfiguration;
import org.intocps.maestro.plugin.Initializer.ConversionUtilities.LongUtils;
import org.intocps.maestro.plugin.Initializer.Spec.StatementGeneratorContainer;
import org.intocps.maestro.plugin.SimulationFramework;
import org.intocps.maestro.plugin.env.ISimulationEnvironment;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.maestro.plugin.env.UnitRelationship.Variable;
import org.intocps.maestro.plugin.env.fmi2.ComponentInfo;
import org.intocps.maestro.plugin.verificationsuite.PrologVerifier.InitializationPrologQuery;
import org.intocps.orchestration.coe.config.InvalidVariableStringException;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.config.ModelParameter;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;

@SimulationFramework(framework = Framework.FMI2)
public class Initializer implements IMaestroExpansionPlugin {
    final static Logger logger = LoggerFactory.getLogger(Initializer.class);

    final AFunctionDeclaration f1 = MableAstFactory.newAFunctionDeclaration(new LexIdentifier("initialize", null),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("startTime")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("endTime"))), MableAstFactory.newAVoidType());

    private final HashMap<ModelConnection.ModelInstance, HashSet<ModelDescription.ScalarVariable>> portsAlreadySet = new HashMap<>();
    private final TopologicalPlugin topologicalPlugin;
    private final InitializationPrologQuery initializationPrologQuery;

    Config config;
    List<ModelParameter> modelParameters;

    public Initializer() {
        this.initializationPrologQuery = new InitializationPrologQuery();
        this.topologicalPlugin = new TopologicalPlugin();
    }


    public Initializer(TopologicalPlugin topologicalPlugin, InitializationPrologQuery initializationPrologQuery) {
        this.topologicalPlugin = topologicalPlugin;
        this.initializationPrologQuery = initializationPrologQuery;
    }

    @Override
    public String getName() {
        return Initializer.class.getSimpleName();
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
    public List<PStm> expand(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config,
            ISimulationEnvironment env, IErrorReporter errorReporter) throws ExpandException {
        logger.debug("Unfolding: {}", declaredFunction.toString());

        verifyArguments(formalArguments, env);
        final List<LexIdentifier> knownComponentNames = extractComponentNames(formalArguments);
        StatementGeneratorContainer.reset();
        var sc = StatementGeneratorContainer.getInstance();

        setSCParameters(formalArguments, (Config) config, sc);
        List<PStm> statements = new Vector<>();
        AVariableDeclaration status =
                newAVariableDeclaration(new LexIdentifier("status", null), newAIntNumericPrimitiveType(), newAExpInitializer(newAIntLiteralExp(0)));
        //statements.add(newALocalVariableStm(status));
        statements.add(newALocalVariableStm(status));
        //Setup experiment for all components
        logger.debug("Setup experiment for all components");
        knownComponentNames.forEach(comp -> {
            statements.add(sc.createSetupExperimentStatement(comp.getText(), false, 0.0, true));
        });

        //All connections - Only relations in the fashion InputToOutput is necessary since the OutputToInputs are just a dublicated of this
        Set<UnitRelationship.Relation> relations =
                env.getRelations(knownComponentNames).stream().filter(o -> o.getDirection() == UnitRelationship.Relation.Direction.OutputToInput)
                        .collect(Collectors.toSet());

        //Find the right order to instantiate dependentPorts and make sure where doesn't exist any cycles in the connections
        List<Set<UnitRelationship.Variable>> instantiationOrder = topologicalPlugin.findInstantiationOrderStrongComponents(relations);

        //Set variables for all components in IniPhase
        statements.addAll(setComponentsVariables(env, knownComponentNames, sc, PhasePredicates.iniPhase()));

        if (this.config.verifyAgainstProlog && !initializationPrologQuery
                .initializationOrderIsValid(instantiationOrder.stream().flatMap(Collection::stream).collect(Collectors.toList()), relations)) {
            throw new ExpandException("The found initialization order is not correct");
        }

        //Enter initialization Mode
        logger.debug("Enter initialization Mode");
        knownComponentNames.forEach(comp -> {
            statements.add(sc.enterInitializationMode(comp.getText()));
        });

        var inputToOutputRelations =
                env.getRelations(knownComponentNames).stream().filter(RelationsPredicates.inputToOutput()).collect(Collectors.toList());

        var inputOutMapping = createInputOutputMapping(inputToOutputRelations, env);
        sc.setInputOutputMapping(inputOutMapping);

        //var optimizedOrder = optimizeInstantiationOrder(instantiationOrder);

        statements.addAll(initializeInterconnectedPorts(env, sc, instantiationOrder));

        //Exit initialization Mode
        knownComponentNames.forEach(comp -> {
            statements.add(sc.exitInitializationMode(comp.getText()));
        });

        return statements;
    }

    private void setSCParameters(List<PExp> formalArguments, Config config, StatementGeneratorContainer sc) {
        sc.startTime = formalArguments.get(1).clone();
        sc.endTime = formalArguments.get(2).clone();
        this.config = config;
        this.modelParameters = this.config.getModelParameters();
        sc.absoluteTolerance = this.config.absoluteTolerance;
        sc.relativeTolerance = this.config.relativeTolerance;
        sc.modelParameters = this.modelParameters;
    }

    private List<PStm> initializeInterconnectedPorts(ISimulationEnvironment env, StatementGeneratorContainer sc,
            List<Set<Variable>> instantiationOrder) throws ExpandException {
        var sccNumber = 0;
        List<PStm> stms = new Vector<>();
        if (config.Stabilisation && instantiationOrder.stream().noneMatch(v -> v.size() > 1)) {
            //The scenario does not contain any loops, but it should still be stabilized - the whole scenario will therefore be initialized in a loop
            stms.addAll(initializeUsingFixedPoint(instantiationOrder.stream().flatMap(Collection::stream).collect(Collectors.toList()), sc, env,
                    sccNumber));
        } else {
            //Initialize the ports in the correct order based on the topological sorting
            for (Set<Variable> variables : instantiationOrder) {//normal variable
                if (this.config.Stabilisation) {
                    //Algebraic loop - specially initialization strategy should be taken.
                    stms.addAll(initializeUsingFixedPoint(new ArrayList<>(variables), sc, env, sccNumber++));
                } else if (variables.size() == 1) {
                    try {
                        stms.addAll(initializePort(variables, sc, env));
                    } catch (ExpandException e) {
                        e.printStackTrace();
                    }
                } else if (variables.size() > 1) {
                    throw new ExpandException(
                            "The co-simulation scenario contains loops, but the initialization is not configured to handle these " + "scenarios");
                }
            }
        }
        return stms;
    }

    private List<PStm> initializeUsingFixedPoint(List<Variable> variables, StatementGeneratorContainer sc, ISimulationEnvironment env,
            int sccNumber) throws ExpandException {
        var optimizedOrder = optimizeInstantiationOrder(variables);

        return sc.createFixedPointIteration(variables, this.config.maxIterations, sccNumber, env);
    }

    private List<ModelDescription.ScalarVariable> getScalarVariables(Set<Variable> variables) {
        return variables.stream().map(o -> o.scalarVariable.getScalarVariable()).collect(Collectors.toList());
    }

    private List<Set<Variable>> optimizeInstantiationOrder(List<Variable> instantiationOrder) {
        List<Set<Variable>> optimizedOrder = new Vector<>();
        Variable previousVariable = instantiationOrder.get(0);
        Set<Variable> currentSet = new HashSet<>(Collections.singletonList(previousVariable));
        for (int i = 1; i < instantiationOrder.size(); i++) {
            Variable currentVariable = instantiationOrder.get(i);
            if (!canBeOptimized(currentVariable, previousVariable)) {
                optimizedOrder.add(currentSet);
                currentSet = new HashSet<>();
            }
            previousVariable = currentVariable;
            currentSet.add(previousVariable);
        }
        if (!currentSet.isEmpty()) {
            optimizedOrder.add(currentSet);
        }

        return optimizedOrder;
    }


    private boolean canBeOptimized(Variable variable1, Variable variable2) {
        return variable1.scalarVariable.getInstance() == variable2.scalarVariable.getInstance() &&
                variable2.scalarVariable.getScalarVariable().causality == variable1.scalarVariable.getScalarVariable().causality &&
                variable2.scalarVariable.getScalarVariable().getType().type == variable1.scalarVariable.getScalarVariable().getType().type;
    }

    private Map<ModelConnection.ModelInstance, Map<ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>>> createInputOutputMapping(
            List<UnitRelationship.Relation> relations, ISimulationEnvironment env) {
        Map<ModelConnection.ModelInstance, Map<ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>>>
                inputToOutputMapping = new HashMap<>();

        var relationsPerInstance = relations.stream().collect(Collectors.groupingBy(o -> o.getSource().scalarVariable.getInstance()));

        relationsPerInstance.forEach((instance, rel) -> {
            ComponentInfo infoSource = env.getUnitInfo(instance, Framework.FMI2);
            Map<ModelDescription.ScalarVariable, AbstractMap.SimpleEntry<ModelConnection.ModelInstance, ModelDescription.ScalarVariable>> entryMap =
                    new HashMap<>();
            rel.forEach(r -> {
                r.getTargets().values().forEach(v -> {
                    ComponentInfo infoTarget = env.getUnitInfo(v.scalarVariable.getInstance(), Framework.FMI2);
                    entryMap.put(r.getSource().scalarVariable.getScalarVariable(), new AbstractMap.SimpleEntry<>(
                            new ModelConnection.ModelInstance(infoTarget.fmuIdentifier, v.scalarVariable.getInstance().getText()),
                            v.scalarVariable.scalarVariable));

                });
            });
            inputToOutputMapping.put(new ModelConnection.ModelInstance(infoSource.fmuIdentifier, instance.getText()), entryMap);
        });

        return inputToOutputMapping;
    }

    //Graph doesn't contain any loops and the ports gets passed in a topological sorted order
    private List<PStm> initializePort(Set<Variable> ports, StatementGeneratorContainer sc, ISimulationEnvironment env) throws ExpandException {
        var scalarVariables = getScalarVariables(ports);
        var type = scalarVariables.iterator().next().getType().type;
        var instance = ports.stream().findFirst().get().scalarVariable.getInstance();
        var causality = scalarVariables.iterator().next().causality;
        long[] scalarValueIndices = GetValueRefIndices(scalarVariables);
        List<PStm> stms = new Vector<>();
        //All members of the same set has the same causality, type and comes from the same instance
        if (causality == ModelDescription.Causality.Output) {
            var statements = sc.getValueStm(instance.getText(), null, scalarValueIndices, type);
            stms.addAll(statements);
        } else {
            stms.addAll(sc.setValueOnPortStm(instance, type, scalarVariables, scalarValueIndices, env));
        }
        return stms;
    }


    private List<PStm> setComponentsVariables(ISimulationEnvironment env, List<LexIdentifier> knownComponentNames, StatementGeneratorContainer sc,
            Predicate<ModelDescription.ScalarVariable> predicate) {
        List<PStm> stms = new Vector<>();
        knownComponentNames.forEach(comp -> {
            ComponentInfo info = env.getUnitInfo(comp, Framework.FMI2);
            try {
                var variablesToInitialize =
                        info.modelDescription.getScalarVariables().stream().filter(predicate).collect(Collectors.groupingBy(o -> o.getType().type));
                if (!variablesToInitialize.isEmpty()) {
                    variablesToInitialize.forEach((type, variables) -> {

                        try {
                            stms.addAll(sc.setValueOnPortStm(comp, type, variables, GetValueRefIndices(variables), env));
                        } catch (ExpandException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (XPathExpressionException | IllegalAccessException | InvocationTargetException e) {
                logger.error(e.getMessage());
            }
        });
        return stms;
    }

    private long[] GetValueRefIndices(List<ModelDescription.ScalarVariable> variables) {
        return variables.stream().map(o -> o.getValueReference()).map(Long.class::cast).collect(LongUtils.TO_LONG_ARRAY);
    }

    private List<LexIdentifier> extractComponentNames(List<PExp> formalArguments) throws ExpandException {
        List<LexIdentifier> knownComponentNames = null;
        if (formalArguments.get(0) instanceof AIdentifierExp) {
            LexIdentifier name = ((AIdentifierExp) formalArguments.get(0)).getName();
            ABlockStm containingBlock = formalArguments.get(0).getAncestor(ABlockStm.class);

            Optional<AVariableDeclaration> compDecl =
                    containingBlock.getBody().stream().filter(ALocalVariableStm.class::isInstance).map(ALocalVariableStm.class::cast)
                            .map(ALocalVariableStm::getDeclaration)
                            .filter(decl -> decl.getName().equals(name) && decl.getIsArray() && decl.getInitializer() != null).findFirst();

            if (compDecl.isEmpty()) {
                throw new ExpandException("Could not find names for comps");
            }

            AArrayInitializer initializer = (AArrayInitializer) compDecl.get().getInitializer();

            knownComponentNames = initializer.getExp().stream().filter(AIdentifierExp.class::isInstance).map(AIdentifierExp.class::cast)
                    .map(AIdentifierExp::getName).collect(Collectors.toList());
        }

        if (knownComponentNames == null || knownComponentNames.isEmpty()) {
            throw new ExpandException("No components found cannot fixed step with 0 components");
        }

        return knownComponentNames;
    }

    private void verifyArguments(List<PExp> formalArguments, ISimulationEnvironment env) throws ExpandException {
        //maybe some of these tests are not necessary - but they are in my unit test
        if (formalArguments == null || formalArguments.size() != f1.getFormals().size()) {
            throw new ExpandException("Invalid args");
        }
        if (env == null) {
            throw new ExpandException("Simulation environment must not be null");
        }
    }

    @Override
    public boolean requireConfig() {
        return true;
    }

    @Override
    public IPluginConfiguration parseConfig(InputStream is) throws IOException {
        JsonNode root = new ObjectMapper().readTree(is);
        //We are only interested in one configuration, so in case it is an array we take the first one.
        if (root instanceof ArrayNode) {
            root = root.get(0);
        }

        JsonNode parameters = root.get("parameters");
        JsonNode verify = root.get("verifyAgainstProlog");
        JsonNode stabilisation = root.get("stabilisation");
        JsonNode fixedPointIteration = root.get("fixedPointIteration");
        JsonNode absoluteTolerance = root.get("absoluteTolerance");
        JsonNode relativeTolerance = root.get("relativeTolerance");

        Config conf = null;
        try {
            conf = new Config(parameters, verify, stabilisation, fixedPointIteration, absoluteTolerance, relativeTolerance);
        } catch (InvalidVariableStringException e) {
            e.printStackTrace();
        }
        return conf;
    }

    public static class Config implements IPluginConfiguration {

        public final boolean Stabilisation;
        private final List<ModelParameter> modelParameters;
        private final boolean verifyAgainstProlog;
        private final int maxIterations;
        private final double absoluteTolerance;
        private final double relativeTolerance;


        public Config(JsonNode parameters, JsonNode verify, JsonNode stabilisation, JsonNode fixedPointIteration, JsonNode absoluteTolerance,
                JsonNode relativeTolerance) throws InvalidVariableStringException {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> result = mapper.convertValue(parameters, new TypeReference<>() {
            });
            modelParameters = buildParameters(result);
            if (verify == null) {
                verifyAgainstProlog = false;
            } else {
                verifyAgainstProlog = verify.asBoolean(false);
            }

            if (stabilisation == null) {
                Stabilisation = false;
            } else {
                Stabilisation = stabilisation.asBoolean(false);
            }

            if (fixedPointIteration == null) {
                maxIterations = 5;
            } else {
                maxIterations = fixedPointIteration.asInt(5);
            }

            if (absoluteTolerance == null) {
                this.absoluteTolerance = 0.2;
            } else {
                this.absoluteTolerance = absoluteTolerance.asDouble(0.2);
            }

            if (relativeTolerance == null) {
                this.relativeTolerance = 0.1;
            } else {
                this.relativeTolerance = relativeTolerance.asDouble(0.1);
            }
        }

        public List<ModelParameter> getModelParameters() {
            return modelParameters;
        }

        private List<ModelParameter> buildParameters(Map<String, Object> parameters) throws InvalidVariableStringException {
            List<ModelParameter> list = new Vector<>();

            if (parameters != null) {
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    list.add(new ModelParameter(ModelConnection.Variable.parse(entry.getKey()), entry.getValue()));
                }
            }
            return list;
        }
    }
}





