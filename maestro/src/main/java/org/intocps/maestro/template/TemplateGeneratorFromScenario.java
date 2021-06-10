package org.intocps.maestro.template;

import core.*;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.*;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.WhileMaBLScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.*;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import scala.jdk.javaapi.CollectionConverters;
import synthesizer.LoopStrategy;
import synthesizer.SynthesizerSimple;

import javax.xml.xpath.XPathExpressionException;
import java.util.Set;
import java.util.*;
import java.util.stream.Collectors;

public class TemplateGeneratorFromScenario {
    private static final String START_TIME_NAME = "START_TIME";
    private static final String END_TIME_NAME = "END_TIME";
    private static final String STEP_SIZE_NAME = "STEP_SIZE";
    private static final String SCENARIO_MODEL_FMU_INSTANCE_DELIMITER = "_";
    private static final String MULTI_MODEL_FMU_INSTANCE_DELIMITER = ".";
    private static MathBuilderFmi2Api mathModule;
    private static LoggerFmi2Api loggerModule;
    private static BooleanBuilderFmi2Api booleanLogicModule;
    private static DynamicActiveBuilderScope dynamicScope;

    public static ASimulationSpecificationCompilationUnit generateTemplate(ScenarioConfiguration configuration) throws Exception {
        //TODO: A Scenario and a multi-model does not agree on the format of identifying a FMU/instance.
        // E.g: FMU in a multi-model is defined as: "{<fmu-name>}" where in a scenario no curly braces are used i.e. "<fmu-name>". Furthermore an
        // instance in a multi-model is uniquely identified as: "{<fmu-name>}.<instance-name>" where instances are not really considered in scenarios
        // but is currently expected to be expressed as: "<fmu-name>_<instance_name>".
        // This is not optimal and should be changed to the same format.

        //TODO: This method just throws on any mismatch but it could potentially be handled using a settings flag instead.
        modelDataMatches(configuration.getMasterModel(), configuration.getSimulationEnvironment());

        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        //TODO: Error handling on or off -> settings flag?
        settings.fmiErrorHandlingEnabled = false;
        MablApiBuilder builder = new MablApiBuilder(settings, false);
        dynamicScope = builder.getDynamicScope();

        // GENERATE MaBL
        mathModule = builder.getMathBuilder();
        loggerModule = builder.getLogger();
        booleanLogicModule = builder.getBooleanBuilder();
        DataWriter.DataWriterInstance dataWriterInstance = builder.getDataWriter().createDataWriterInstance();
        DoubleVariableFmi2Api coSimEndTime = dynamicScope.store(END_TIME_NAME, configuration.getExecutionParameters().getEndTime());
        DoubleVariableFmi2Api coSimStartTime = dynamicScope.store(START_TIME_NAME, configuration.getExecutionParameters().getStartTime());

        List<FmuVariableFmi2Api> fmus = configuration.getSimulationEnvironment().getFmusWithModelDescriptions().stream()
                .filter(entry -> configuration.getSimulationEnvironment().getUriFromFMUName(entry.getKey()) != null).map(entry -> {
                    try {
                        return dynamicScope.createFMU(removeBraces(entry.getKey()), entry.getValue(),
                                configuration.getSimulationEnvironment().getUriFromFMUName(entry.getKey()));
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to create FMU variable: " + e);
                    }
                }).collect(Collectors.toList());

        // Generate fmu instances with identifying names from the mater model.
        Map<String, ComponentVariableFmi2Api> fmuInstances =
                CollectionConverters.asJava(configuration.getMasterModel().scenario().fmus()).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                            Optional<FmuVariableFmi2Api> fmuFromScenario = fmus.stream().filter(fmu -> fmu.getName()
                                    .contains(entry.getKey().split(SCENARIO_MODEL_FMU_INSTANCE_DELIMITER)[0].toLowerCase(Locale.ROOT))).findAny();
                            if (fmuFromScenario.isEmpty()) {
                                throw new RuntimeException("Unable to match fmu from multi model with fmu from master model");
                            }
                            return fmuFromScenario.get().instantiate(entry.getKey());
                        }));

        // If the initialization section is missing use the SynthesizerSimple to generate it from the scenario model.
        if (configuration.getMasterModel().initialization().length() == 0) {
            SynthesizerSimple synthesizer = new SynthesizerSimple(configuration.getMasterModel().scenario(), LoopStrategy.maximum());
            configuration.getMasterModel().initialization().appendedAll(synthesizer.synthesizeInitialization());
        }

        dataWriterInstance.initialize(new ArrayList<>(CollectionConverters.asJava(configuration.getMasterModel().scenario().connections()).stream()
                .map(connection -> fmuInstances.get(connection.srcPort().fmu()).getPort(connection.srcPort().port())).collect(Collectors.toSet())));

        // Generate setup experiment section
        fmuInstances.values().forEach(instance -> instance
                .setupExperiment(coSimStartTime, coSimEndTime, configuration.getExecutionParameters().getConvergenceRelativeTolerance()));

        // Generate set parameters section
        setParameters(fmuInstances, configuration.getParameters());

        // Generate initialization section
        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> sharedPortVars =
                mapInitializationActionsToMaBL(CollectionConverters.asJava(configuration.getMasterModel().initialization()), fmuInstances,
                        configuration.getExecutionParameters(), CollectionConverters.asJava(configuration.getMasterModel().scenario().connections()));

        // Generate step loop section
        DoubleVariableFmi2Api currentCommunicationPoint =
                dynamicScope.store("current_communication_point", configuration.getExecutionParameters().getStartTime());
        currentCommunicationPoint.setValue(coSimStartTime);
        DoubleVariableFmi2Api stepSize = dynamicScope.store(STEP_SIZE_NAME, configuration.getExecutionParameters().getStepSize());
        DoubleVariableFmi2Api currentStepSize = dynamicScope.store("current_step_size", configuration.getExecutionParameters().getStepSize());
        dataWriterInstance.log(currentCommunicationPoint);
        WhileMaBLScope coSimStepLoop = dynamicScope.enterWhile(currentCommunicationPoint.toMath().addition(stepSize).lessThan(coSimEndTime));
        currentStepSize.setValue(stepSize);

        mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(configuration.getMasterModel().cosimStep()), fmuInstances,
                currentCommunicationPoint, new HashSet<>(), configuration.getExecutionParameters(),
                CollectionConverters.asJava(configuration.getMasterModel().scenario().connections()), sharedPortVars, currentStepSize);

        currentCommunicationPoint.setValue(currentCommunicationPoint.toMath().addition(currentStepSize));
        dataWriterInstance.log(currentCommunicationPoint);
        coSimStepLoop.leave();
        dataWriterInstance.close();

        fmus.forEach(fmu -> fmu.unload(dynamicScope));

        return builder.build();
    }

    private static void modelDataMatches(MasterModel masterModel, Fmi2SimulationEnvironment simulationEnvironment) {
        Map<String, FmuModel> fmuInstancesInMasterModel = CollectionConverters.asJava(masterModel.scenario().fmus());

        if (fmuInstancesInMasterModel.size() != simulationEnvironment.getInstances().size()) {
            throw new RuntimeException("The multi model and master model do not agree on the number of fmu instances.");
        }

        // Verify that fmu identifiers and fmu instance names matches between the multi model and master model and that reject step and get-set
        // state agrees.
        for (Map.Entry<String, ComponentInfo> fmuInstanceFromMultiModel : simulationEnvironment.getInstances()) {
            boolean instanceMatch = false;
            // Find a match between identifiers
            for (String masterModelFmuInstanceIdentifier : fmuInstancesInMasterModel.keySet()) {
                if (masterModelFmuInstanceIdentifier.contains(removeBraces(fmuInstanceFromMultiModel.getValue().fmuIdentifier)) &&
                        masterModelFmuInstanceIdentifier.contains(fmuInstanceFromMultiModel.getKey())) {
                    try {
                        if (simulationEnvironment.getModelDescription(fmuInstanceFromMultiModel.getValue().fmuIdentifier).getCanGetAndSetFmustate() !=
                                fmuInstancesInMasterModel.get(masterModelFmuInstanceIdentifier).canRejectStep()) {
                            throw new RuntimeException("The multi model and master model do not agree whether it is possible to reject a step");
                        }
                    } catch (XPathExpressionException e) {
                        throw new RuntimeException(
                                "Unable to access model description for fmu: " + fmuInstanceFromMultiModel.getValue().fmuIdentifier);
                    }
                    instanceMatch = true;
                    break;
                }
            }
            if (!instanceMatch) {
                throw new RuntimeException("The multi model and master model do not agree on instance identifiers");
            }
        }
    }

    private static void setParameters(Map<String, ComponentVariableFmi2Api> fmuInstances, Map<String, Object> parameters) {
        fmuInstances.forEach((instanceName, fmuInstance) -> {
            Map<String, Object> portToValue = parameters.entrySet().stream()
                    .filter(entry -> removeBraces(entry.getKey()).contains(masterMRepresentationToMultiMRepresentation(instanceName)))
                    .collect(Collectors.toMap(e -> e.getKey().split("\\" + MULTI_MODEL_FMU_INSTANCE_DELIMITER)[2], Map.Entry::getValue));
            // Map each parameter to matching expression value
            Map<? extends Fmi2Builder.Port, ? extends Fmi2Builder.ExpressionValue> PortToExpressionValue =
                    portToValue.entrySet().stream().collect(Collectors.toMap(e -> fmuInstance.getPort(e.getKey()), e -> {
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

    private static List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> mapInitializationActionsToMaBL(
            List<InitializationInstruction> initializationInstructions, Map<String, ComponentVariableFmi2Api> fmuInstances,
            ScenarioConfiguration.ExecutionParameters executionParameters, List<ConnectionModel> connections) {

        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> sharedPortVars = new ArrayList<>();
        // Loop over initialization instructions and map them to MaBL
        initializationInstructions.forEach(instruction -> {
            if (instruction instanceof InitGet) {
                Map<PortFmi2Api, VariableFmi2Api<Object>> portsWithGets = mapGetInstruction(((InitGet) instruction).port(), fmuInstances);
                portsWithGets.forEach((key, value) -> sharedPortVars.stream()
                        .filter(entry -> entry.getKey().getLogScalarVariableName().contains(key.getLogScalarVariableName())).findAny()
                        .ifPresentOrElse(item -> {
                        }, () -> sharedPortVars.add(Map.entry(key, value))));
            } else if (instruction instanceof InitSet) {
                mapSetInstruction(((InitSet) instruction).port(), sharedPortVars, fmuInstances, connections);
            } else if (instruction instanceof AlgebraicLoopInit) {
                mapAlgebraicLoopInitializationInstruction((AlgebraicLoopInit) instruction, fmuInstances, executionParameters, connections,
                        sharedPortVars);
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

    private static Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> mapCoSimStepInstructionsToMaBL(
            List<CosimStepInstruction> coSimStepInstructions, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DoubleVariableFmi2Api currentCommunicationPoint, Set<Fmi2Builder.StateVariable<PStm>> fmuStates,
            ScenarioConfiguration.ExecutionParameters executionParameters, List<ConnectionModel> connections,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> sharedPortVars, DoubleVariableFmi2Api currentStepSize) {

        Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuInstanceWithStepVar =
                new HashMap<>();
        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> tentativePortVars = new ArrayList<>();

        // Loop over step instructions and map them to MaBL
        coSimStepInstructions.forEach(instruction -> {
            if (instruction instanceof core.Set) {
                mapSetInstruction(((core.Set) instruction).port(), sharedPortVars, fmuInstances, connections);
            } else if (instruction instanceof Get) {
                Map<PortFmi2Api, VariableFmi2Api<Object>> portsWithGets = mapGetInstruction(((Get) instruction).port(), fmuInstances);
                portsWithGets.forEach((key, value) -> sharedPortVars.stream()
                        .filter(entry -> entry.getKey().getLogScalarVariableName().contains(key.getLogScalarVariableName())).findAny()
                        .ifPresentOrElse(item -> {
                        }, () -> sharedPortVars.add(Map.entry(key, value))));
            } else if (instruction instanceof Step) {
                Map.Entry<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> instanceWithStep =
                        mapStepInstruction((Step) instruction, fmuInstances, fmuInstanceWithStepVar, currentCommunicationPoint, currentStepSize);
                fmuInstanceWithStepVar.put(instanceWithStep.getKey(), instanceWithStep.getValue());
            } else if (instruction instanceof SaveState) {
                String instanceName = ((SaveState) instruction).fmu();
                try {
                    fmuStates.add(fmuInstances.get(instanceName).getState());
                } catch (XPathExpressionException e) {
                    throw new RuntimeException("Could not get state for fmu instance: " + instanceName);
                }
            } else if (instruction instanceof RestoreState) {
                fmuStates.stream().filter(state -> state.getName().contains(((RestoreState) instruction).fmu().toLowerCase(Locale.ROOT))).findAny()
                        .ifPresent(Fmi2Builder.StateVariable::set);
            } else if (instruction instanceof AlgebraicLoop) {
                mapAlgebraicLoopCoSimStepInstruction((AlgebraicLoop) instruction, fmuInstances, currentCommunicationPoint, fmuStates,
                        executionParameters, connections, sharedPortVars, currentStepSize).forEach(fmuInstanceWithStepVar::putIfAbsent);
            } else if (instruction instanceof StepLoop) {
                mapStepLoopCoSimStepInstruction((StepLoop) instruction, fmuInstances, currentCommunicationPoint, fmuStates, executionParameters,
                        connections, sharedPortVars, currentStepSize);
            } else if (instruction instanceof GetTentative) {
                Map<PortFmi2Api, VariableFmi2Api<Object>> portsWithGets =
                        mapGetTentativeInstruction(((GetTentative) instruction).port(), fmuInstances);
                portsWithGets.forEach((port, portValue) -> tentativePortVars.stream()
                        .filter(entry -> entry.getKey().getLogScalarVariableName().contains(port.getLogScalarVariableName())).findAny()
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
            fmuInstances.get(tentativePortMapEntry.getKey().getLogScalarVariableName().split("\\.")[1]).share(Map.ofEntries(tentativePortMapEntry));

            sharedPortVars.stream().filter(portMapVarEntry -> portMapVarEntry.getKey().getLogScalarVariableName()
                    .contains(tentativePortMapEntry.getKey().getLogScalarVariableName())).findAny().ifPresentOrElse(item -> {
            }, () -> sharedPortVars.add(Map.entry(tentativePortMapEntry.getKey(), tentativePortMapEntry.getValue())));
        });

        return fmuInstanceWithStepVar;
    }

    private static void mapStepLoopCoSimStepInstruction(StepLoop instruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DoubleVariableFmi2Api currentCommunicationPoint, Set<Fmi2Builder.StateVariable<PStm>> fmuStates,
            ScenarioConfiguration.ExecutionParameters executionParameters, List<ConnectionModel> connections,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet, DoubleVariableFmi2Api currentStepSize) {

        ArrayVariableFmi2Api<Double> fmuCommunicationPoints =
                dynamicScope.store("fmu_communication_points", new Double[fmuInstances.entrySet().size()]);

        BooleanVariableFmi2Api stepAcceptedPredicate = dynamicScope.store("step_accepted_predicate", false);
        ScopeFmi2Api stepAcceptedScope = dynamicScope.enterWhile(stepAcceptedPredicate.toPredicate().not());

        // Map iterate instructions to MaBL
        Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuInstanceWithStepVar =
                mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(instruction.iterate()), fmuInstances, currentCommunicationPoint, fmuStates,
                        executionParameters, connections, portsWithGet, currentStepSize);

        // Get step accepted boolean from each fmu instance of interest.
        List<Fmi2Builder.BoolVariable<PStm>> acceptedStepVariables = new ArrayList<>();
        List<String> acceptFmuRefs = CollectionConverters.asJava(instruction.untilStepAccept());
        fmuInstanceWithStepVar.forEach((fmuInstance, stepWithAccept) -> {
            if (acceptFmuRefs.stream().anyMatch(name -> fmuInstance.getName().contains(name.toLowerCase(Locale.ROOT)))) {
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
            mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(instruction.ifRetryNeeded()), fmuInstances, currentCommunicationPoint,
                    fmuStates, executionParameters, connections, portsWithGet, currentStepSize);

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

    private static Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> mapAlgebraicLoopCoSimStepInstruction(
            AlgebraicLoop algebraicLoopInstruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DoubleVariableFmi2Api currentCommunicationPoint, Set<Fmi2Builder.StateVariable<PStm>> fmuStates,
            ScenarioConfiguration.ExecutionParameters executionParameters, List<ConnectionModel> connections,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portMapVars, DoubleVariableFmi2Api currentStepSize) {

        Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuWithStep = new HashMap<>();
        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> tentativePortMapVars = new ArrayList<>();
        List<PortRef> convergedPortRefs = CollectionConverters.asJava(algebraicLoopInstruction.untilConverged());
        List<BooleanVariableFmi2Api> convergedVariables = new ArrayList<>();

        DoubleVariableFmi2Api absTol = dynamicScope.store("coSimStep_absolute_tolerance", executionParameters.getConvergenceAbsoluteTolerance());
        DoubleVariableFmi2Api relTol = dynamicScope.store("coSimStep_relative_tolerance", executionParameters.getConvergenceRelativeTolerance());
        BooleanVariableFmi2Api convergencePredicate = dynamicScope.store("coSimStep_convergence_predicate", false);
        IntVariableFmi2Api convergenceAttempts = dynamicScope.store("coSimStep_convergence_attempts", executionParameters.getConvergenceAttempts());

        // Enter while loop
        ScopeFmi2Api convergenceScope = dynamicScope
                .enterWhile(convergencePredicate.toPredicate().not().and(convergenceAttempts.toMath().greaterThan(IntExpressionValue.of(0))));

        // Handle and map each iterate instruction to MaBL
        for (CosimStepInstruction instruction : CollectionConverters.asJava(algebraicLoopInstruction.iterate())) {
            if (instruction instanceof GetTentative) {
                Map<PortFmi2Api, VariableFmi2Api<Object>> portsWithGets =
                        mapGetTentativeInstruction(((GetTentative) instruction).port(), fmuInstances);
                portsWithGets.forEach((port, portValue) -> {
                    tentativePortMapVars.stream().filter(entry -> entry.getKey().getLogScalarVariableName().contains(port.getLogScalarVariableName()))
                            .findAny().ifPresentOrElse(item -> {
                    }, () -> tentativePortMapVars.add(Map.entry(port, portValue)));

                    // Check for convergence
                    convergedPortRefs.stream().filter(ref -> port.aMablFmi2ComponentAPI.getName().contains(ref.fmu().toLowerCase(Locale.ROOT)) &&
                            port.getName().contains(ref.port().toLowerCase(Locale.ROOT))).findAny().ifPresent(
                            portRef -> convergedVariables.add(createCheckConvergenceSection(Map.entry(port, portValue), absTol, relTol, portRef)));
                });
            } else if (instruction instanceof SetTentative) {
                mapSetTentativeInstruction(((SetTentative) instruction).port(), tentativePortMapVars, fmuInstances, connections);
            } else {
                mapCoSimStepInstructionsToMaBL(List.of(instruction), fmuInstances, currentCommunicationPoint, fmuStates, executionParameters,
                        connections, portMapVars, currentStepSize).forEach(fmuWithStep::put);
            }
        }

        // Share any tentative port values and include them in shared port vars
        tentativePortMapVars.forEach(tentativePortMapEntry -> {
            fmuInstances.get(tentativePortMapEntry.getKey().getLogScalarVariableName().split("\\.")[1]).share(Map.ofEntries(tentativePortMapEntry));

            portMapVars.stream().filter(portMapVarEntry -> portMapVarEntry.getKey().getLogScalarVariableName()
                    .contains(tentativePortMapEntry.getKey().getLogScalarVariableName())).findAny().ifPresentOrElse(item -> {
            }, () -> portMapVars.add(Map.entry(tentativePortMapEntry.getKey(), tentativePortMapEntry.getValue())));
        });

        // Check if all instances have converged
        convergencePredicate.setValue(booleanLogicModule.allTrue("converged", convergedVariables));
        dynamicScope.enterIf(convergencePredicate.toPredicate().not());
        {
            // Map retry instructions to MaBL
            mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(algebraicLoopInstruction.ifRetryNeeded()), fmuInstances,
                    currentCommunicationPoint, fmuStates, executionParameters, connections, portMapVars, currentStepSize);

            loggerModule.trace("## Convergence was not reached at sim-time: %f with step size: %f... %d convergence attempts remaining",
                    currentCommunicationPoint, currentStepSize, convergenceAttempts);

            convergenceAttempts.decrement();
        }
        dynamicScope.leave();
        convergenceScope.leave();
        return fmuWithStep;
    }

    private static void mapAlgebraicLoopInitializationInstruction(AlgebraicLoopInit instruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            ScenarioConfiguration.ExecutionParameters executionParameters, List<ConnectionModel> connections,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> sharedPortVars) {

        DoubleVariableFmi2Api absTol = dynamicScope.store("initialization_absolute_tolerance", executionParameters.getConvergenceAbsoluteTolerance());
        DoubleVariableFmi2Api relTol = dynamicScope.store("initialization_relative_tolerance", executionParameters.getConvergenceRelativeTolerance());
        BooleanVariableFmi2Api convergencePredicate = dynamicScope.store("initialization_convergence_predicate", false);
        IntVariableFmi2Api convergenceAttempts = dynamicScope.store("initialization_converge_attempts", executionParameters.getConvergenceAttempts());
        ScopeFmi2Api convergenceScope = dynamicScope
                .enterWhile(convergencePredicate.toPredicate().not().and(convergenceAttempts.toMath().greaterThan(IntExpressionValue.of(0))));

        // Map iterate instructions to MaBL
        mapInitializationActionsToMaBL(CollectionConverters.asJava(instruction.iterate()), fmuInstances, executionParameters, connections);

        // Generate convergence section
        List<BooleanVariableFmi2Api> convergedVariables = new ArrayList<>();
        for (PortRef portRef : CollectionConverters.asJava(instruction.untilConverged())) {
            sharedPortVars.stream().filter(entry -> entry.getKey().aMablFmi2ComponentAPI.getName().contains(portRef.fmu().toLowerCase(Locale.ROOT)) &&
                    entry.getKey().getName().contains(portRef.port().toLowerCase(Locale.ROOT))).findAny()
                    .ifPresent(portMap -> convergedVariables.add(createCheckConvergenceSection(portMap, absTol, relTol, portRef)));
        }

        convergencePredicate.setValue(booleanLogicModule.allTrue("converged", convergedVariables));
        dynamicScope.enterIf(convergencePredicate.toPredicate().not());
        {
            loggerModule.trace("## Convergence was not reached during initialization... %d convergence attempts remaining", convergenceAttempts);
            convergenceAttempts.decrement();
        }
        convergenceScope.leave();
    }

    private static BooleanVariableFmi2Api createCheckConvergenceSection(Map.Entry<PortFmi2Api, VariableFmi2Api<Object>> portMap,
            DoubleVariableFmi2Api absTolVar, DoubleVariableFmi2Api relTolVar, PortRef portRef) {
        VariableFmi2Api oldVariable = portMap.getKey().getSharedAsVariable();
        VariableFmi2Api<Object> newVariable = portMap.getValue();
        BooleanVariableFmi2Api isClose = dynamicScope.store("isClose", false);
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

    private static Map.Entry<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> mapStepInstruction(
            Step instruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuWithStep,
            DoubleVariableFmi2Api currentCommunicationPoint, DoubleVariableFmi2Api defaultCommunicationStepSize) {

        ComponentVariableFmi2Api instance = fmuInstances.get(instruction.fmu());
        Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>> step;

        // Step with the correct step size according to the instruction
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

    private static void mapSetInstruction(PortRef portRef, List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portMapVars,
            Map<String, ComponentVariableFmi2Api> fmuInstances, List<ConnectionModel> connections) {
        Map.Entry<PortFmi2Api, VariableFmi2Api<Object>> sourcePortWithValue = getSourcePortFromPortRef(portRef, portMapVars, connections);
        if (sourcePortWithValue != null) {
            Map.Entry<ComponentVariableFmi2Api, PortFmi2Api> instanceWithPort = getInstanceWithPortFromPortRef(portRef, fmuInstances);
            instanceWithPort.getKey().set(instanceWithPort.getValue(), sourcePortWithValue.getKey().getSharedAsVariable());
        }
    }

    private static void mapSetTentativeInstruction(PortRef portRef, List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet,
            Map<String, ComponentVariableFmi2Api> fmuInstances, List<ConnectionModel> connections) {
        Map.Entry<PortFmi2Api, VariableFmi2Api<Object>> sourcePortWithValue = getSourcePortFromPortRef(portRef, portsWithGet, connections);
        if (sourcePortWithValue != null) {
            Map.Entry<ComponentVariableFmi2Api, PortFmi2Api> instanceWithPort = getInstanceWithPortFromPortRef(portRef, fmuInstances);
            instanceWithPort.getKey().set(instanceWithPort.getValue(), sourcePortWithValue.getValue());
        }
    }

    private static Map.Entry<ComponentVariableFmi2Api, PortFmi2Api> getInstanceWithPortFromPortRef(PortRef portRef,
            Map<String, ComponentVariableFmi2Api> fmuInstances) {
        ComponentVariableFmi2Api instance = fmuInstances.get(portRef.fmu());
        PortFmi2Api port = instance.getPort(portRef.port());
        return Map.entry(instance, port);
    }

    private static Map.Entry<PortFmi2Api, VariableFmi2Api<Object>> getSourcePortFromPortRef(PortRef portRef,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portMapVars, List<ConnectionModel> connections) {
        Optional<ConnectionModel> connectionModel = connections.stream()
                .filter(connection -> connection.trgPort().port().equals(portRef.port()) && connection.trgPort().fmu().equals(portRef.fmu()))
                .findAny();

        if (connectionModel.isPresent()) {
            Optional<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portWithValue = portMapVars.stream().filter(entry ->
                    entry.getKey().aMablFmi2ComponentAPI.getName().contains(connectionModel.get().srcPort().fmu().toLowerCase(Locale.ROOT)) &&
                            entry.getKey().getName().contains(connectionModel.get().srcPort().port().toLowerCase(Locale.ROOT))).findAny();

            if (portWithValue.isPresent()) {
                return portWithValue.get();
            }
        }

        return null;
    }

    private static Map<PortFmi2Api, VariableFmi2Api<Object>> mapGetTentativeInstruction(PortRef portRef,
            Map<String, ComponentVariableFmi2Api> fmuInstances) {
        return fmuInstances.get(portRef.fmu()).getTentative(dynamicScope, portRef.port());
    }

    private static Map<PortFmi2Api, VariableFmi2Api<Object>> mapGetInstruction(PortRef portRef, Map<String, ComponentVariableFmi2Api> fmuInstances) {
        ComponentVariableFmi2Api instance = fmuInstances.get(portRef.fmu());
        Map<PortFmi2Api, VariableFmi2Api<Object>> portMapVar = instance.get(portRef.port());
        instance.share(portMapVar);
        return portMapVar;
    }

    private static String masterMRepresentationToMultiMRepresentation(String instanceRepresentation) {
        return instanceRepresentation.replace(SCENARIO_MODEL_FMU_INSTANCE_DELIMITER, MULTI_MODEL_FMU_INSTANCE_DELIMITER);
    }

    private static String removeBraces(String toRemove) {
        return toRemove.replaceAll("[{}]", "");
    }

}
