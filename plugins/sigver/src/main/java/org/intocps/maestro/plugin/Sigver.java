package org.intocps.maestro.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.AModuleDeclaration;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.AImportedModuleCompilationUnit;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.*;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.WhileMaBLScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.*;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.jdk.javaapi.CollectionConverters;
import org.intocps.verification.scenarioverifier.core.masterModel.*;
import org.intocps.verification.scenarioverifier.core.*;
import org.intocps.verification.scenarioverifier.core.FMI3.AdaptiveModel;
import org.intocps.verification.scenarioverifier.synthesizer.LoopStrategy;
import org.intocps.verification.scenarioverifier.synthesizer.SynthesizerSimple;

import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;

@SimulationFramework(framework = Framework.FMI2)
public class Sigver extends BasicMaestroExpansionPlugin {
    public static final String MASTER_MODEL_FMU_INSTANCE_DELIMITER = "_";
    public static final String MULTI_MODEL_FMU_INSTANCE_DELIMITER = ".";
    public static final String EXECUTE_ALGORITHM_FUNCTION_NAME = "executeAlgorithm";
    final static Logger logger = LoggerFactory.getLogger(Sigver.class);
    private final AFunctionDeclaration func = newAFunctionDeclaration(newAIdentifier(EXECUTE_ALGORITHM_FUNCTION_NAME),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("stepSize")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("startTime")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("endTime")),
                    newAFormalParameter(newBoleanType(), newAIdentifier("endTimeDefined"))), newAVoidType());
    private MathBuilderFmi2Api mathModule;
    private LoggerFmi2Api loggerModule;
    private BooleanBuilderFmi2Api booleanLogicModule;
    private DynamicActiveBuilderScope dynamicScope;
    private Double relTol;
    private Double absTol;
    private Integer convAtt;
    private AdaptiveModel adaptiveModel;

    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return Stream.of(func).collect(Collectors.toSet());
    }

    @Override
    public <R> RuntimeConfigAddition<R> expandWithRuntimeAddition(AFunctionDeclaration declaredFunction,
            Fmi2Builder<PStm, ASimulationSpecificationCompilationUnit, PExp, ?> providedBuilder, List<Fmi2Builder.Variable<PStm, ?>> formalArguments,
            IPluginConfiguration config, ISimulationEnvironment envIn, IErrorReporter errorReporter) throws ExpandException {

        //TODO: A Scenario and a multi-model does not agree on the format of identifying a FMU/instance.
        // E.g: FMU in a multi-model is defined as: "{<fmu-name>}" where in a scenario no curly braces are used i.e. "<fmu-name>". Furthermore an
        // instance in a multi-model is uniquely identified as: "{<fmu-name>}.<instance-name>" where instances are not really considered in scenarios
        // but is currently expected to be expressed as: "<fmu-name>_<instance_name>".
        // This is not optimal and should be changed to the same format.
        // Also should fmu, instance and port identifiers be case sensitive?

        logger.info("Unfolding with scenario verifier: {}", declaredFunction.toString());

        if (!getDeclaredUnfoldFunctions().contains(declaredFunction)) {
            throw new ExpandException("Unknown function declaration");
        }

        if (envIn == null) {
            throw new ExpandException("Simulation environment must not be null");
        }

        if (formalArguments == null || formalArguments.size() != func.getFormals().size()) {
            throw new ExpandException("Invalid args");
        }

        if (!(providedBuilder instanceof MablApiBuilder)) {
            throw new ExpandException("Not supporting the given builder type. Expecting " + MablApiBuilder.class.getSimpleName() + " got " +
                    providedBuilder.getClass().getSimpleName());
        }

        DoubleVariableFmi2Api stepSize = (DoubleVariableFmi2Api) formalArguments.get(1);
        DoubleVariableFmi2Api startTime = (DoubleVariableFmi2Api) formalArguments.get(2);
        DoubleVariableFmi2Api endTime = (DoubleVariableFmi2Api) formalArguments.get(3);
        BooleanVariableFmi2Api endTimeDefined = (BooleanVariableFmi2Api) formalArguments.get(4);

        SigverConfig configuration = (SigverConfig) config;
        relTol = configuration.relTol;
        absTol = configuration.absTol;
        convAtt = configuration.convergenceAttempts;

        // GENERATE MaBL
        try {
            MasterModelFMI2 masterModel = ScenarioLoaderFMI2.load(new ByteArrayInputStream(configuration.masterModel.getBytes()));
            FMI2ScenarioModel scenarioModel = masterModel.scenario();
            adaptiveModel = scenarioModel.config();

            MablApiBuilder builder = (MablApiBuilder) providedBuilder;
            dynamicScope = builder.getDynamicScope();
            mathModule = builder.getMathBuilder();
            loggerModule = builder.getLogger();
            booleanLogicModule = builder.getBooleanBuilder();
            DataWriter.DataWriterInstance dataWriterInstance = builder.getDataWriter().createDataWriterInstance();

            // Get FMU instances
            // use LinkedHashMap to preserve added order
            Map<String, ComponentVariableFmi2Api> fmuInstances =
                    ((List<ComponentVariableFmi2Api>) ((Fmi2Builder.ArrayVariable) formalArguments.get(0)).items()).stream().collect(
                            Collectors.toMap(ComponentVariableFmi2Api::getEnvironmentName, Function.identity(), (u, v) -> u, LinkedHashMap::new));

            // Rename FMU instance-name keys to master model format: <fmu-name>_<instance-name>
            Set<String> fmuKeys = new HashSet<>(fmuInstances.keySet());
            fmuKeys.forEach(key -> {
                ComponentVariableFmi2Api fmuInstance = fmuInstances.get(key);
                String fmuIdentifier = fmuInstance.getOwner().getFmuIdentifier();
                String newKey = fmuIdentifier.substring(1, fmuIdentifier.length() - 1) + MASTER_MODEL_FMU_INSTANCE_DELIMITER + key;
                fmuInstances.remove(key);
                fmuInstances.put(newKey, fmuInstance);
            });

            // If the initialization section is missing use the SynthesizerSimple to generate it from the scenario model.
            if (masterModel.initialization().isEmpty()) {
                SynthesizerSimple synthesizer = new SynthesizerSimple(scenarioModel, LoopStrategy.maximum());
                masterModel.initialization().concat(synthesizer.synthesizeInitialization());
            }
            dataWriterInstance.initialize(fmuInstances.values().stream().flatMap(x -> x.getVariablesToLog().stream()).collect(Collectors.toList()));

            //TODO: Instantiate section from master model contains instantiate and setup experiment instruction, but these are ignored for now.

            // Generate setup experiment section
            fmuInstances.values().forEach(instance -> instance.setupExperiment(startTime, endTime, endTimeDefined, configuration.relTol));

            // Generate set parameters section
            setSEA(fmuInstances, configuration.parameters);

            // Generate initialization section
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> sharedPortVars =
                    mapInitializationActionsToMaBL(CollectionConverters.asJava(masterModel.initialization()), fmuInstances,
                            CollectionConverters.asJava(scenarioModel.connections()));

            // Generate step loop section
            DoubleVariableFmi2Api currentCommunicationPoint = dynamicScope.store("current_communication_point", 0.0);
            currentCommunicationPoint.setValue(startTime);
            DoubleVariableFmi2Api currentStepSize = dynamicScope.store("current_step_size", 0.0);

            // Get and share logging variables that are not yet shared
            fmuInstances.values().forEach(instance -> instance.getAndShare(
                    instance.getVariablesToLog().stream().filter(var -> var.getSharedAsVariable() == null).map(PortFmi2Api::getName)
                            .toArray(String[]::new)));

            dataWriterInstance.log(currentCommunicationPoint);
            WhileMaBLScope coSimStepLoop = dynamicScope.enterWhile(currentCommunicationPoint.toMath().addition(stepSize).lessThan(endTime));

            currentStepSize.setValue(stepSize);

            Map<String, List<CosimStepInstruction>> coSimStepsMap = CollectionConverters.asJava(masterModel.cosimStep()).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> CollectionConverters.asJava(e.getValue())));

            mapCoSimStepInstructionsToMaBL(coSimStepsMap, fmuInstances, currentCommunicationPoint, new HashSet<>(),
                    CollectionConverters.asJava(scenarioModel.connections()), sharedPortVars, currentStepSize);

            // Get and share variables that are not connected
            fmuInstances.values().forEach(instance -> instance.getAndShare(
                    instance.getVariablesToLog().stream().filter(var -> var.getTargetPorts().size() < 1).map(PortFmi2Api::getName)
                            .toArray(String[]::new)));

            currentCommunicationPoint.setValue(currentCommunicationPoint.toMath().addition(currentStepSize));
            dataWriterInstance.log(currentCommunicationPoint);
            coSimStepLoop.leave();
            dataWriterInstance.close();

            return new EmptyRuntimeConfig<>();

        } catch (Exception e) {
            throw new ExpandException("Error occurred during plugin expansion: " + e);
        }
    }

    @Override
    public boolean requireConfig() {
        return true;
    }

    @Override
    public IPluginConfiguration parseConfig(InputStream is) throws IOException {
        return (new ObjectMapper().readValue(is, SigverConfig.class));
    }

    @Override
    public AImportedModuleCompilationUnit getDeclaredImportUnit() {
        AImportedModuleCompilationUnit unit = new AImportedModuleCompilationUnit();
        unit.setImports(Stream.of("FMI2", "TypeConverter", "Math", "Logger", "DataWriter", "BooleanLogic").map(MableAstFactory::newAIdentifier)
                .collect(Collectors.toList()));
        AModuleDeclaration module = new AModuleDeclaration();
        module.setName(newAIdentifier(getName()));
        module.setFunctions(new ArrayList<>(getDeclaredUnfoldFunctions()));
        unit.setModule(module);
        return unit;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }

    private void setSEA(Map<String, ComponentVariableFmi2Api> fmuInstances, Map<String, Object> parameters) {
        fmuInstances.forEach((instanceName, fmuInstance) -> {
            Map<String, Object> portNameToValueMap =
                    parameters.entrySet().stream().filter(entry -> entry.getKey().contains(masterMRepresentationToMultiMRepresentation(instanceName)))
                            .collect(Collectors.toMap(e -> {
                                String[] instanceNameSplit = e.getKey().split("\\" + MULTI_MODEL_FMU_INSTANCE_DELIMITER);
                                return String.join(MULTI_MODEL_FMU_INSTANCE_DELIMITER,
                                        Arrays.copyOfRange(instanceNameSplit, 2, instanceNameSplit.length));
                            }, Map.Entry::getValue));


            // Map each parameter to matching expression value but only for ports with the causality of parameter which are tunable
            Map<? extends Fmi2Builder.Port, ? extends Fmi2Builder.ExpressionValue> PortToExpressionValue =
                    portNameToValueMap.entrySet().stream().filter(e -> {
                        PortFmi2Api port = fmuInstance.getPort(e.getKey());
                        boolean isTunableParameter = port.scalarVariable.variability.equals(Fmi2ModelDescription.Variability.Tunable) &&
                                port.scalarVariable.causality.equals(Fmi2ModelDescription.Causality.Parameter);
                        boolean isEligibleVariable = port.scalarVariable.variability != Fmi2ModelDescription.Variability.Constant &&
                                (port.scalarVariable.initial.equals(Fmi2ModelDescription.Initial.Exact) ||
                                        port.scalarVariable.initial.equals(Fmi2ModelDescription.Initial.Approx));
                        //TODO: log/notify if port in 'parameters' either does not have the causality of parameter or is not tunable.
                        return isTunableParameter || isEligibleVariable;
                    }).collect(Collectors.toMap(e -> fmuInstance.getPort(e.getKey()), e -> {
                        if (e.getValue() instanceof Double) {
                            return DoubleExpressionValue.of((Double) e.getValue());
                        } else if (e.getValue() instanceof Integer) {
                            return IntExpressionValue.of((Integer) e.getValue());
                        } else if (e.getValue() instanceof Boolean) {
                            return BooleanExpressionValue.of((Boolean) e.getValue());
                        } else if (e.getValue() instanceof String) {
                            return StringExpressionValue.of((String) e.getValue());
                        } else {
                            throw new RuntimeException("Unable to set parameter of class: " + e.getValue().getClass().toString());
                        }
                    }));
            fmuInstance.set(new PortValueExpresssionMapImpl(PortToExpressionValue));
        });
    }

    private List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> mapInitializationActionsToMaBL(
            List<InitializationInstruction> initializationInstructions, Map<String, ComponentVariableFmi2Api> fmuInstances,
            List<ConnectionModel> connections) {

        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> sharedPortVars = new ArrayList<>();
        // Loop over initialization instructions and map them to MaBL
        initializationInstructions.forEach(instruction -> {
            if (instruction instanceof InitGet) {
                Map<PortFmi2Api, VariableFmi2Api<Object>> portsWithGets = mapGetInstruction(((InitGet) instruction).port(), fmuInstances);
                portsWithGets.forEach((key, value) -> sharedPortVars.stream()
                        .filter(entry -> entry.getKey().getMultiModelScalarVariableName().contains(key.getMultiModelScalarVariableName())).findAny()
                        .ifPresentOrElse(item -> {
                        }, () -> sharedPortVars.add(Map.entry(key, value))));
            } else if (instruction instanceof InitSet) {
                mapSetInstruction(((InitSet) instruction).port(), sharedPortVars, fmuInstances, connections);
            } else if (instruction instanceof AlgebraicLoopInit) {
                mapAlgebraicLoopInitializationInstruction((AlgebraicLoopInit) instruction, fmuInstances, connections, sharedPortVars);
            } else if (instruction instanceof EnterInitMode) {
                fmuInstances.get(((EnterInitMode) instruction).fmu()).enterInitializationMode();
            } else if (instruction instanceof ExitInitMode) {
                fmuInstances.get(((ExitInitMode) instruction).fmu()).exitInitializationMode();
            } else {
                throw new RuntimeException("Unknown initialization instruction: " + instruction.toString());
            }
        });
        return sharedPortVars;
    }

    private Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> mapCoSimStepInstructionsToMaBL(
            Map<String, List<CosimStepInstruction>> coSimStepInstructionsMap, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DoubleVariableFmi2Api currentCommunicationPoint, Set<Fmi2Builder.StateVariable<PStm>> fmuStates, List<ConnectionModel> connections,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> sharedPortVars, DoubleVariableFmi2Api currentStepSize) {

        Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuInstanceWithStepVar =
                new HashMap<>();
        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> tentativePortVars = new ArrayList<>();

        if (!adaptiveModel.configurations().isEmpty() || coSimStepInstructionsMap.values().size() > 1) {
            throw new RuntimeException("Support for multiple algorithms is not yet supported in Maestro!");
        }
        // For now, we only handle a single co-sim step algorithm.
        String algorithmIdentifier = coSimStepInstructionsMap.keySet().iterator().next();
        List<CosimStepInstruction> coSimStepInstructions = coSimStepInstructionsMap.get(algorithmIdentifier);

        // Loop over step instructions and map them to MaBL
        coSimStepInstructions.forEach(instruction -> {
            if (instruction instanceof org.intocps.verification.scenarioverifier.core.Set) {
                mapSetInstruction(((org.intocps.verification.scenarioverifier.core.Set) instruction).port(), sharedPortVars, fmuInstances, connections);
            } else if (instruction instanceof Get) {
                Map<PortFmi2Api, VariableFmi2Api<Object>> portsWithGets = mapGetInstruction(((Get) instruction).port(), fmuInstances);
                portsWithGets.forEach((key, value) -> sharedPortVars.stream()
                        .filter(entry -> entry.getKey().getMultiModelScalarVariableName().contains(key.getMultiModelScalarVariableName())).findAny()
                        .ifPresentOrElse(item -> {
                        }, () -> sharedPortVars.add(Map.entry(key, value))));
            } else if (instruction instanceof Step) {
                Map.Entry<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> instanceWithStep =
                        mapStepInstruction((Step) instruction, fmuInstances, fmuInstanceWithStepVar, currentCommunicationPoint, currentStepSize);
                fmuInstanceWithStepVar.put(instanceWithStep.getKey(), instanceWithStep.getValue());
            } else if (instruction instanceof SaveState) {
                String MasterModelInstanceName = ((SaveState) instruction).fmu();
                try {
                    fmuStates.add(fmuInstances.get(MasterModelInstanceName).getState());
                } catch (XPathExpressionException e) {
                    throw new RuntimeException("Could not get state for fmu instance: " + MasterModelInstanceName);
                }
            } else if (instruction instanceof RestoreState) {
                fmuStates.stream().filter(state -> state.getName()
                                .contains(((RestoreState) instruction).fmu().toLowerCase(Locale.ROOT).split(MASTER_MODEL_FMU_INSTANCE_DELIMITER)[1]))
                        .findAny().ifPresent(Fmi2Builder.StateVariable::set);
            } else if (instruction instanceof AlgebraicLoop) {
                mapAlgebraicLoopCoSimStepInstruction(algorithmIdentifier, (AlgebraicLoop) instruction, fmuInstances, currentCommunicationPoint,
                        fmuStates, connections, sharedPortVars, currentStepSize).forEach(fmuInstanceWithStepVar::putIfAbsent);
            } else if (instruction instanceof StepLoop) {
                mapStepLoopCoSimStepInstruction(algorithmIdentifier, (StepLoop) instruction, fmuInstances, currentCommunicationPoint, fmuStates,
                        connections, sharedPortVars, currentStepSize);
            } else if (instruction instanceof GetTentative) {
                Map<PortFmi2Api, VariableFmi2Api<Object>> portsWithGets =
                        mapGetTentativeInstruction(((GetTentative) instruction).port(), fmuInstances);
                portsWithGets.forEach((port, portValue) -> tentativePortVars.stream()
                        .filter(entry -> entry.getKey().getMultiModelScalarVariableName().contains(port.getMultiModelScalarVariableName())).findAny()
                        .ifPresentOrElse(item -> {
                        }, () -> tentativePortVars.add(Map.entry(port, portValue))));
            } else if (instruction instanceof SetTentative) {
                mapSetTentativeInstruction(((SetTentative) instruction).port(), tentativePortVars, fmuInstances, connections);
            } else {
                throw new RuntimeException("Unknown CoSimStep instruction: " + instruction.toString());
            }
        });

        // Share any tentative port values and include them in shared port vars
        tentativePortVars.forEach(tentativePortMapEntry -> {
            fmuInstances.get(tentativePortMapEntry.getKey().getMultiModelScalarVariableName().split("\\.")[1])
                    .share(Map.ofEntries(tentativePortMapEntry));

            sharedPortVars.stream().filter(portMapVarEntry -> portMapVarEntry.getKey().getMultiModelScalarVariableName()
                    .contains(tentativePortMapEntry.getKey().getMultiModelScalarVariableName())).findAny().ifPresentOrElse(item -> {
            }, () -> sharedPortVars.add(Map.entry(tentativePortMapEntry.getKey(), tentativePortMapEntry.getValue())));
        });

        return fmuInstanceWithStepVar;
    }

    private void mapStepLoopCoSimStepInstruction(String parentAlgorithmIdentifier, StepLoop instruction,
            Map<String, ComponentVariableFmi2Api> fmuInstances, DoubleVariableFmi2Api currentCommunicationPoint,
            Set<Fmi2Builder.StateVariable<PStm>> fmuStates, List<ConnectionModel> connections,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet, DoubleVariableFmi2Api currentStepSize) {

        ArrayVariableFmi2Api<Double> fmuCommunicationPoints =
                dynamicScope.store("fmu_communication_points", new Double[fmuInstances.entrySet().size()]);

        BooleanVariableFmi2Api stepAcceptedPredicate = dynamicScope.store("step_accepted_predicate", false);
        ScopeFmi2Api stepAcceptedScope = dynamicScope.enterWhile(stepAcceptedPredicate.toPredicate().not());

        // Map iterate instructions to MaBL
        Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuInstanceWithStepVar =
                mapCoSimStepInstructionsToMaBL(Map.of(parentAlgorithmIdentifier, CollectionConverters.asJava(instruction.iterate())), fmuInstances,
                        currentCommunicationPoint, fmuStates, connections, portsWithGet, currentStepSize);

        // Get step accepted boolean from each fmu instance of interest.
        List<Fmi2Builder.BoolVariable<PStm>> acceptedStepVariables = new ArrayList<>();
        List<String> acceptFmuRefs = CollectionConverters.asJava(instruction.untilStepAccept());
        fmuInstanceWithStepVar.forEach((fmuInstance, stepWithAccept) -> {
            if (acceptFmuRefs.stream().anyMatch(name -> name.toLowerCase(Locale.ROOT).contains(fmuInstance.getName()))) {
                acceptedStepVariables.add(stepWithAccept.getKey());
                dynamicScope.enterIf(stepWithAccept.getKey().toPredicate().not());
                {
                    loggerModule.trace("## FMU: '%s' DISCARDED step at sim-time: %f for step-size: %f and proposed sim-time: %.15f",
                            fmuInstance.getName(), currentCommunicationPoint, currentStepSize,
                            new VariableFmi2Api<>(null, stepWithAccept.getValue().getType(), dynamicScope, dynamicScope, null,
                                    stepWithAccept.getValue().getExp()));
                }
                dynamicScope.leave();
            }
        });

        stepAcceptedPredicate.setValue(booleanLogicModule.allTrue("all_fmus_accepted_step_size", acceptedStepVariables));

        dynamicScope.enterIf(stepAcceptedPredicate.toPredicate().not());
        {
            // Map retry instructions to MaBL
            mapCoSimStepInstructionsToMaBL(Map.of(parentAlgorithmIdentifier, CollectionConverters.asJava(instruction.ifRetryNeeded())), fmuInstances,
                    currentCommunicationPoint, fmuStates, connections, portsWithGet, currentStepSize);

            // Set the step size to the lowest accepted step-size
            List<Fmi2Builder.DoubleVariable<PStm>> stepSizes =
                    fmuInstanceWithStepVar.values().stream().map(Map.Entry::getValue).collect(Collectors.toList());
            for (int i = 0; i < stepSizes.size(); i++) {
                fmuCommunicationPoints.items().get(i).setValue(new DoubleExpressionValue(stepSizes.get(i).getExp()));
            }
            currentStepSize.setValue(mathModule.minRealFromArray(fmuCommunicationPoints).toMath().subtraction(currentCommunicationPoint));

            loggerModule.trace("## Step size was not accepted by every FMU! It has been changed to the smallest accepted step size of: %f",
                    currentStepSize);
        }
        dynamicScope.leave();
        stepAcceptedScope.leave();
    }

    private Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> mapAlgebraicLoopCoSimStepInstruction(
            String parentAlgorithmIdentifier, AlgebraicLoop algebraicLoopInstruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DoubleVariableFmi2Api currentCommunicationPoint, Set<Fmi2Builder.StateVariable<PStm>> fmuStates, List<ConnectionModel> connections,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portMapVars, DoubleVariableFmi2Api currentStepSize) {

        Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuWithStep = new HashMap<>();
        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> tentativePortMapVars = new ArrayList<>();
        List<PortRef> convergedPortRefs = CollectionConverters.asJava(algebraicLoopInstruction.untilConverged());
        List<BooleanVariableFmi2Api> convergedVariables = new ArrayList<>();


        BooleanVariableFmi2Api convergencePredicate = dynamicScope.store("coSimStep_convergence_predicate", false);
        IntVariableFmi2Api convergenceAttempts = dynamicScope.store("coSimStep_convergence_attempts", convAtt);
        DoubleVariableFmi2Api relTolVar = dynamicScope.store("coSimStep_relative_tolerance", relTol);
        DoubleVariableFmi2Api absTolVar = dynamicScope.store("coSimStep_absolute_tolerance", absTol);

        // Enter while loop
        ScopeFmi2Api convergenceScope = dynamicScope.enterWhile(
                convergencePredicate.toPredicate().not().and(convergenceAttempts.toMath().greaterThan(IntExpressionValue.of(0))));

        // Handle and map each iterate instruction to MaBL
        for (CosimStepInstruction instruction : CollectionConverters.asJava(algebraicLoopInstruction.iterate())) {
            if (instruction instanceof GetTentative) {
                Map<PortFmi2Api, VariableFmi2Api<Object>> portsWithGets =
                        mapGetTentativeInstruction(((GetTentative) instruction).port(), fmuInstances);
                portsWithGets.forEach((port, portValue) -> {
                    tentativePortMapVars.stream()
                            .filter(entry -> entry.getKey().getMultiModelScalarVariableName().contains(port.getMultiModelScalarVariableName()))
                            .findAny().ifPresentOrElse(item -> {
                            }, () -> tentativePortMapVars.add(Map.entry(port, portValue)));

                    // Check for convergence
                    convergedPortRefs.stream().filter(ref -> portRefMatch(ref, port.aMablFmi2ComponentAPI.getEnvironmentName(), port.getName()))
                            .findAny().ifPresent(portRef -> convergedVariables.add(
                                    createCheckConvergenceSection(Map.entry(port, portValue), portRef, absTolVar, relTolVar)));
                });
            } else if (instruction instanceof SetTentative) {
                mapSetTentativeInstruction(((SetTentative) instruction).port(), tentativePortMapVars, fmuInstances, connections);
            } else {
                mapCoSimStepInstructionsToMaBL(Map.of(parentAlgorithmIdentifier, List.of(instruction)), fmuInstances, currentCommunicationPoint,
                        fmuStates, connections, portMapVars, currentStepSize).forEach(fmuWithStep::put);
            }
        }

        // Share any tentative port values and include them in shared port vars
        tentativePortMapVars.forEach(tentativePortMapEntry -> {
            String fmuName = tentativePortMapEntry.getKey().aMablFmi2ComponentAPI.getOwner().getFmuIdentifier();
            String instanceName = fmuName.substring(1, fmuName.length() - 1) + MASTER_MODEL_FMU_INSTANCE_DELIMITER +
                    tentativePortMapEntry.getKey().aMablFmi2ComponentAPI.getEnvironmentName();
            fmuInstances.get(instanceName).share(Map.ofEntries(tentativePortMapEntry));

            portMapVars.stream().filter(portMapVarEntry -> portMapVarEntry.getKey().getMultiModelScalarVariableName()
                    .contains(tentativePortMapEntry.getKey().getMultiModelScalarVariableName())).findAny().ifPresentOrElse(item -> {
            }, () -> portMapVars.add(Map.entry(tentativePortMapEntry.getKey(), tentativePortMapEntry.getValue())));
        });

        // Check if all instances have converged
        convergencePredicate.setValue(booleanLogicModule.allTrue("converged", convergedVariables));
        dynamicScope.enterIf(convergencePredicate.toPredicate().not());
        {
            // Map retry instructions to MaBL
            mapCoSimStepInstructionsToMaBL(Map.of(parentAlgorithmIdentifier, CollectionConverters.asJava(algebraicLoopInstruction.ifRetryNeeded())),
                    fmuInstances, currentCommunicationPoint, fmuStates, connections, portMapVars, currentStepSize);

            loggerModule.trace("## Convergence was not reached at sim-time: %f with step size: %f... %d convergence attempts remaining",
                    currentCommunicationPoint, currentStepSize, convergenceAttempts);

            convergenceAttempts.decrement();
        }
        dynamicScope.leave();
        convergenceScope.leave();
        return fmuWithStep;
    }

    private void mapAlgebraicLoopInitializationInstruction(AlgebraicLoopInit instruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            List<ConnectionModel> connections, List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> sharedPortVars) {
        BooleanVariableFmi2Api convergencePredicate = dynamicScope.store("initialization_convergence_predicate", false);
        IntVariableFmi2Api convergenceAttempts = dynamicScope.store("initialization_converge_attempts", convAtt);
        DoubleVariableFmi2Api relTolVar = dynamicScope.store("initialization_relative_tolerance", relTol);
        DoubleVariableFmi2Api absTolVar = dynamicScope.store("initialization_absolute_tolerance", absTol);
        ScopeFmi2Api convergenceScope = dynamicScope.enterWhile(
                convergencePredicate.toPredicate().not().and(convergenceAttempts.toMath().greaterThan(IntExpressionValue.of(0))));

        // Map iterate instructions to MaBL
        mapInitializationActionsToMaBL(CollectionConverters.asJava(instruction.iterate()), fmuInstances, connections);

        // Generate convergence section
        List<BooleanVariableFmi2Api> convergedVariables = new ArrayList<>();
        for (PortRef portRef : CollectionConverters.asJava(instruction.untilConverged())) {
            sharedPortVars.stream()
                    .filter(entry -> portRefMatch(portRef, entry.getKey().aMablFmi2ComponentAPI.getEnvironmentName(), entry.getKey().getName()))
                    .findAny().ifPresent(portMap -> convergedVariables.add(createCheckConvergenceSection(portMap, portRef, absTolVar, relTolVar)));
        }


        convergencePredicate.setValue(booleanLogicModule.allTrue("converged", convergedVariables));
        dynamicScope.enterIf(convergencePredicate.toPredicate().not());
        {
            loggerModule.trace("## Convergence was not reached during initialization... %d convergence attempts remaining", convergenceAttempts);
            convergenceAttempts.decrement();
        }
        convergenceScope.leave();
    }

    private BooleanVariableFmi2Api createCheckConvergenceSection(Map.Entry<PortFmi2Api, VariableFmi2Api<Object>> portMap, PortRef portRef,
            DoubleVariableFmi2Api absTolVar, DoubleVariableFmi2Api relTolVar) {
        VariableFmi2Api oldVariable = portMap.getKey().getSharedAsVariable();
        VariableFmi2Api<Object> newVariable = portMap.getValue();
        BooleanVariableFmi2Api isClose = dynamicScope.store("isClose", false);

        // Check for convergence and log if not close
        isClose.setValue(mathModule.checkConvergence(oldVariable, newVariable, absTolVar, relTolVar));
        dynamicScope.enterIf(isClose.toPredicate().not());
        {
            loggerModule.trace("Unstable signal %s = %.15E during algebraic loop",
                    masterMRepresentationToMultiMRepresentation(portRef.fmu()) + MULTI_MODEL_FMU_INSTANCE_DELIMITER + portRef.port(),
                    portMap.getValue());
            dynamicScope.leave();
        }
        return isClose;
    }

    private Map.Entry<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> mapStepInstruction(
            Step instruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuWithStep,
            DoubleVariableFmi2Api currentCommunicationPoint, DoubleVariableFmi2Api defaultCommunicationStepSize) {

        ComponentVariableFmi2Api instance = fmuInstances.get(instruction.fmu());
        Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>> step;

        // Step with the proper step size according to the instruction
        if (instruction.by() instanceof AbsoluteStepSize) {
            step = instance.step(currentCommunicationPoint,
                    new DoubleVariableFmi2Api(null, null, null, null, DoubleExpressionValue.of(((AbsoluteStepSize) instruction.by()).H()).getExp()));
        } else if (instruction.by() instanceof RelativeStepSize) {
            ComponentVariableFmi2Api fmuInstance = fmuInstances.get(((RelativeStepSize) instruction.by()).fmu());
            step = instance.step(currentCommunicationPoint, fmuWithStep.get(fmuInstance).getValue());
        } else {
            step = instance.step(currentCommunicationPoint, defaultCommunicationStepSize);
        }
        return Map.entry(instance, step);
    }

    private void mapSetInstruction(PortRef portRef, List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portMapVars,
            Map<String, ComponentVariableFmi2Api> fmuInstances, List<ConnectionModel> connections) {
        Map.Entry<PortFmi2Api, VariableFmi2Api<Object>> sourcePortWithValue = getSourcePortFromPortRef(portRef, portMapVars, connections);

        // Create set call only if a source port with a prior get call is present.
        if (sourcePortWithValue != null) {
            Map.Entry<ComponentVariableFmi2Api, PortFmi2Api> instanceWithPort = getInstanceWithPortFromPortRef(portRef, fmuInstances);
            // Set value from the shared value buffer.
            instanceWithPort.getKey().set(instanceWithPort.getValue(), sourcePortWithValue.getKey().getSharedAsVariable());
        }
    }

    private void mapSetTentativeInstruction(PortRef portRef, List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet,
            Map<String, ComponentVariableFmi2Api> fmuInstances, List<ConnectionModel> connections) {
        Map.Entry<PortFmi2Api, VariableFmi2Api<Object>> sourcePortWithValue = getSourcePortFromPortRef(portRef, portsWithGet, connections);

        // Create set call only if a source port with a prior get call is present.
        if (sourcePortWithValue != null) {
            Map.Entry<ComponentVariableFmi2Api, PortFmi2Api> instanceWithPort = getInstanceWithPortFromPortRef(portRef, fmuInstances);
            // Set value from the IO value buffer.
            instanceWithPort.getKey().set(instanceWithPort.getValue(), sourcePortWithValue.getValue());
        }
    }

    private Map.Entry<ComponentVariableFmi2Api, PortFmi2Api> getInstanceWithPortFromPortRef(PortRef portRef,
            Map<String, ComponentVariableFmi2Api> fmuInstances) {
        ComponentVariableFmi2Api instance = fmuInstances.get(portRef.fmu());
        PortFmi2Api port = instance.getPort(portRef.port());
        return Map.entry(instance, port);
    }

    private Map.Entry<PortFmi2Api, VariableFmi2Api<Object>> getSourcePortFromPortRef(PortRef portRef,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portMapVars, List<ConnectionModel> connections) {
        Optional<ConnectionModel> connectionModel = connections.stream()
                .filter(connection -> connection.trgPort().port().equals(portRef.port()) && connection.trgPort().fmu().equals(portRef.fmu()))
                .findAny();

        if (connectionModel.isPresent()) {
            Optional<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portWithValue = portMapVars.stream()
                    .filter(entry -> portRefMatch(connectionModel.get().srcPort(), entry.getKey().aMablFmi2ComponentAPI.getEnvironmentName(),
                            entry.getKey().getName())).findAny();

            if (portWithValue.isPresent()) {
                return portWithValue.get();
            }
        }
        return null;
    }

    private Map<PortFmi2Api, VariableFmi2Api<Object>> mapGetTentativeInstruction(PortRef portRef,
            Map<String, ComponentVariableFmi2Api> fmuInstances) {
        return fmuInstances.get(portRef.fmu()).getTentative(dynamicScope, portRef.port());
    }

    private Map<PortFmi2Api, VariableFmi2Api<Object>> mapGetInstruction(PortRef portRef, Map<String, ComponentVariableFmi2Api> fmuInstances) {
        ComponentVariableFmi2Api instance = fmuInstances.get(portRef.fmu());
        Map<PortFmi2Api, VariableFmi2Api<Object>> portMapVar = instance.get(portRef.port());
        instance.share(portMapVar);
        return portMapVar;
    }

    private boolean portRefMatch(PortRef portRef, String multiModelInstanceName, String multiModelPortName) {
        return portRef.fmu().toLowerCase(Locale.ROOT).contains(multiModelInstanceName.toLowerCase(Locale.ROOT)) &&
                portRef.port().toLowerCase(Locale.ROOT).contains(multiModelPortName.toLowerCase(Locale.ROOT));
    }

    private String masterMRepresentationToMultiMRepresentation(String masterModelRepresentation) {
        String[] interMediateRepresentation = masterModelRepresentation.split(MASTER_MODEL_FMU_INSTANCE_DELIMITER);
        return "{" + interMediateRepresentation[0] + "}" + MULTI_MODEL_FMU_INSTANCE_DELIMITER +
                String.join(MULTI_MODEL_FMU_INSTANCE_DELIMITER, Arrays.copyOfRange(interMediateRepresentation, 1, interMediateRepresentation.length));
    }
}
