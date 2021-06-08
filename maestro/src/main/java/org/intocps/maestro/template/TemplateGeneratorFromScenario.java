package org.intocps.maestro.template;

import core.*;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.*;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.*;
import org.intocps.maestro.framework.fmi2.api.mabl.values.*;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import scala.jdk.javaapi.CollectionConverters;

import javax.xml.xpath.XPathExpressionException;
import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;

public class TemplateGeneratorFromScenario {
    private static final String START_TIME_NAME = "START_TIME";
    private static final String END_TIME_NAME = "END_TIME";
    private static final String STEP_SIZE_NAME = "STEP_SIZE";
    private static final String FMI2COMPONENT_TYPE = "FMI2Component";
    private static final String SCENARIO_MODEL_FMU_INSTANCE_DELIMITER = "_";
    private static final String MULTI_MODEL_FMU_INSTANCE_DELIMITER = ".";

    public static ASimulationSpecificationCompilationUnit generateTemplate(ScenarioConfiguration configuration) throws Exception {


        //TODO: A Scenario and a multi-model does not agree on the format of identifying a FMU/instance.
        // E.g: FMU in a multi-model is defined as: "{<fmu-name>}" where in a scenario no curly braces are used i.e. "<fmu-name>". Furthermore an
        // instance in a multi-model is uniquely identified as: "{<fmu-name>}.<instance-name>" where instances are not really considered in scenarios
        // but could be expressed as: "<fmu-name>_<instance_name>".

        //TODO: This method just throws on any mismatch but it could potentially be handled using a settings flag.
        modelDataMatches(configuration.getMasterModel(), configuration.getSimulationEnvironment());

        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        MablApiBuilder builder = new MablApiBuilder(settings, false);
        DynamicActiveBuilderScope dynamicScope = builder.getDynamicScope();

        // GENERATE MaBL
        MathBuilderFmi2Api mathModule = builder.getMathBuilder();
        LoggerFmi2Api loggerModule = builder.getLogger();
        BooleanBuilderFmi2Api booleanLogicModule = builder.getBooleanBuilder();
        DataWriter.DataWriterInstance dataWriterInstance = builder.getDataWriter().createDataWriterInstance();
        DoubleVariableFmi2Api coSimEndTime = dynamicScope.store(END_TIME_NAME, configuration.getExecutionParameters().getEndTime());
        DoubleVariableFmi2Api coSimStartTime = dynamicScope.store(START_TIME_NAME, configuration.getExecutionParameters().getStartTime());

        List<FmuVariableFmi2Api> fmus = configuration.getSimulationEnvironment().getFmusWithModelDescriptions().stream()
                .filter(entry -> configuration.getSimulationEnvironment().getUriFromFMUName(entry.getKey()) != null).map(entry -> {
                    try {
                        return dynamicScope.createFMU(removeBraces(entry.getKey()), entry.getValue(),
                                configuration.getSimulationEnvironment().getUriFromFMUName(entry.getKey()));
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to create FMU variable: " + e.getLocalizedMessage());
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

        //TODO: Use initializer expansion plugin if no initializer sequence is passed. -> Just use the ScenarioVerifier

        dataWriterInstance.initialize(new ArrayList<>(CollectionConverters.asJava(configuration.getMasterModel().scenario().connections()).stream()
                .map(connection -> fmuInstances.get(connection.srcPort().fmu()).getPort(connection.srcPort().port())).collect(Collectors.toSet())));

        // Generate setup experiment section
        fmuInstances.values().forEach(instance -> instance
                .setupExperiment(coSimStartTime, coSimEndTime, configuration.getExecutionParameters().getConvergenceRelativeTolerance()));

        // Generate set parameters section
        setParameters(fmuInstances, configuration.getParameters());

        // Generate initialization section
        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGets =
                mapInitializationActionsToMaBL(CollectionConverters.asJava(configuration.getMasterModel().initialization()), fmuInstances,
                        dynamicScope, booleanLogicModule, mathModule, loggerModule, configuration.getExecutionParameters(),
                        CollectionConverters.asJava(configuration.getMasterModel().scenario().connections()));

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
                currentCommunicationPoint, new HashSet<>(), dynamicScope, booleanLogicModule, mathModule, loggerModule,
                configuration.getExecutionParameters(), CollectionConverters.asJava(configuration.getMasterModel().scenario().connections()),
                portsWithGets, currentStepSize);

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
            DynamicActiveBuilderScope dynamicScope, BooleanBuilderFmi2Api booleanLogicModule, MathBuilderFmi2Api mathModule,
            LoggerFmi2Api loggerModule, ScenarioConfiguration.ExecutionParameters executionParameters, List<ConnectionModel> connections) {

        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portMapVars = new ArrayList<>();
        initializationInstructions.forEach(instruction -> {
            if (instruction instanceof InitGet) {
                handleGetInstruction(((InitGet) instruction).port(), fmuInstances).forEach((key, value) -> portMapVars.stream()
                        .filter(entry -> entry.getKey().getLogScalarVariableName().contains(key.getLogScalarVariableName())).findAny()
                        .ifPresentOrElse(item -> {
                        }, () -> portMapVars.add(Map.entry(key, value))));
            } else if (instruction instanceof InitSet) {
                handleSetInstruction(((InitSet) instruction).port(), portMapVars, fmuInstances, connections);
            } else if (instruction instanceof AlgebraicLoopInit) {
                handleAlgebraicLoopInitializationInstruction((AlgebraicLoopInit) instruction, fmuInstances, dynamicScope, booleanLogicModule,
                        mathModule, loggerModule, executionParameters, connections, portMapVars);
            } else if (instruction instanceof EnterInitMode) {
                fmuInstances.get(((EnterInitMode) instruction).fmu()).enterInitializationMode();
            } else if (instruction instanceof ExitInitMode) {
                fmuInstances.get(((ExitInitMode) instruction).fmu()).exitInitializationMode();
            } else {
                throw new RuntimeException("Unknown initialization instruction: " + instruction.toString());
            }
        });
        return portMapVars;
    }

    private static Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> mapCoSimStepInstructionsToMaBL(
            List<CosimStepInstruction> coSimStepInstructions, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DoubleVariableFmi2Api currentCommunicationPoint, Set<Fmi2Builder.StateVariable<PStm>> fmuStates, DynamicActiveBuilderScope dynamicScope,
            BooleanBuilderFmi2Api booleanLogicModule, MathBuilderFmi2Api mathModule, LoggerFmi2Api loggerModule,
            ScenarioConfiguration.ExecutionParameters executionParameters, List<ConnectionModel> connections,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portMapVars, DoubleVariableFmi2Api currentStepSize) {

        Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuWithStep = new HashMap<>();
        for (CosimStepInstruction instruction : coSimStepInstructions) {
            if (instruction instanceof core.Set) {
                handleSetInstruction(((core.Set) instruction).port(), portMapVars, fmuInstances, connections);
            } else if (instruction instanceof Get) {
                handleGetInstruction(((Get) instruction).port(), fmuInstances).forEach((key, value) -> portMapVars.stream()
                        .filter(entry -> entry.getKey().getLogScalarVariableName().contains(key.getLogScalarVariableName())).findAny()
                        .ifPresentOrElse(item -> {
                        }, () -> portMapVars.add(Map.entry(key, value))));
            } else if (instruction instanceof Step) {
                Map.Entry<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> instanceWithStep =
                        handleStepInstruction((Step) instruction, fmuInstances, fmuWithStep, currentCommunicationPoint, currentStepSize);
                fmuWithStep.put(instanceWithStep.getKey(), instanceWithStep.getValue());
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
                handleAlgebraicLoopCoSimStepInstruction((AlgebraicLoop) instruction, fmuInstances, currentCommunicationPoint, fmuStates, dynamicScope,
                        booleanLogicModule, mathModule, loggerModule, executionParameters, connections, portMapVars, currentStepSize)
                        .forEach(fmuWithStep::putIfAbsent);
            } else if (instruction instanceof StepLoop) {
                handleStepLoopCoSimStepInstruction((StepLoop) instruction, fmuInstances, currentCommunicationPoint, fmuStates, dynamicScope,
                        booleanLogicModule, mathModule, loggerModule, executionParameters, connections, portMapVars, currentStepSize);
            } else {
                throw new RuntimeException("Unknown CoSimStep instruction: " + instruction.toString());
            }
        }

        return fmuWithStep;
    }

    private static void handleStepLoopCoSimStepInstruction(StepLoop instruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DoubleVariableFmi2Api currentCommunicationPoint, Set<Fmi2Builder.StateVariable<PStm>> fmuStates, DynamicActiveBuilderScope dynamicScope,
            BooleanBuilderFmi2Api booleanLogicModule, MathBuilderFmi2Api mathModule, LoggerFmi2Api loggerModule,
            ScenarioConfiguration.ExecutionParameters executionParameters, List<ConnectionModel> connections,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet, DoubleVariableFmi2Api currentStepSize) {

        ArrayVariableFmi2Api<Double> fmuCommunicationPoints =
                dynamicScope.store("fmu_communication_points", new Double[fmuInstances.entrySet().size()]);

        BooleanVariableFmi2Api allAcceptedStep = dynamicScope.store("all_accepted_step", false);
        ScopeFmi2Api stepAcceptedScope = dynamicScope.enterWhile(allAcceptedStep.toPredicate().not());

        Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmusWithStep =
                mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(instruction.iterate()), fmuInstances, currentCommunicationPoint, fmuStates,
                        dynamicScope, booleanLogicModule, mathModule, loggerModule, executionParameters, connections, portsWithGet, currentStepSize);

        List<Fmi2Builder.BoolVariable<PStm>> accepted = fmusWithStep.entrySet().stream()
                .filter(entry -> CollectionConverters.asJava(instruction.untilStepAccept()).stream()
                        .anyMatch(name -> entry.getKey().getName().contains(name.toLowerCase(Locale.ROOT)))).map(entry -> entry.getValue().getKey())
                .collect(Collectors.toList());

        allAcceptedStep.setValue(booleanLogicModule.allTrue("all_fmus_accepted_step_size", accepted));

        IfMaBlScope allAcceptedScope = dynamicScope.enterIf(allAcceptedStep.toPredicate().not());
        {
            mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(instruction.ifRetryNeeded()), fmuInstances, currentCommunicationPoint,
                    fmuStates, dynamicScope, booleanLogicModule, mathModule, loggerModule, executionParameters, connections, portsWithGet,
                    currentStepSize);

            // Set the step size to the lowest accepted step-size
            List<Fmi2Builder.DoubleVariable<PStm>> stepSizes = fmusWithStep.values().stream().map(Map.Entry::getValue).collect(Collectors.toList());
            for (int i = 0; i < stepSizes.size(); i++) {
                fmuCommunicationPoints.items().get(i).setValue(new DoubleExpressionValue(stepSizes.get(i).getExp()));
            }
            currentStepSize.setValue(mathModule.minRealFromArray(fmuCommunicationPoints).toMath().subtraction(currentCommunicationPoint));
        }
        allAcceptedScope.leave();
        stepAcceptedScope.leave();
    }

    private static Map.Entry<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> handleStepInstruction(
            Step instruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuWithStep,
            DoubleVariableFmi2Api currentCommunicationPoint, DoubleVariableFmi2Api defaultCommunicationStepSize) {

        ComponentVariableFmi2Api instance = fmuInstances.get(instruction.fmu());
        Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>> step;

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

    private static Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> handleAlgebraicLoopCoSimStepInstruction(
            AlgebraicLoop algebraicLoopInstruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DoubleVariableFmi2Api currentCommunicationPoint, Set<Fmi2Builder.StateVariable<PStm>> fmuStates, DynamicActiveBuilderScope dynamicScope,
            BooleanBuilderFmi2Api booleanLogicModule, MathBuilderFmi2Api mathModule, LoggerFmi2Api loggerModule,
            ScenarioConfiguration.ExecutionParameters executionParameters, List<ConnectionModel> connections,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portMapVars, DoubleVariableFmi2Api currentStepSize) {

        DoubleVariableFmi2Api absTol = dynamicScope.store("coSimStep_absolute_tolerance", executionParameters.getConvergenceAbsoluteTolerance());
        DoubleVariableFmi2Api relTol = dynamicScope.store("coSimStep_relative_tolerance", executionParameters.getConvergenceRelativeTolerance());
        BooleanVariableFmi2Api convergenceReached = dynamicScope.store("coSimStep_has_converged", false);
        IntVariableFmi2Api stepConvergeAttempts = dynamicScope.store("converge_attempts", executionParameters.getConvergenceAttempts());
        ScopeFmi2Api convergenceScope = dynamicScope
                .enterWhile(convergenceReached.toPredicate().not().and(stepConvergeAttempts.toMath().greaterThan(IntExpressionValue.of(0))));

        Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuWithStep = new HashMap<>();
        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> tentativePortMapVars = new ArrayList<>();
        List<PortRef> convergedPortRefs = CollectionConverters.asJava(algebraicLoopInstruction.untilConverged());
        List<BooleanVariableFmi2Api> convergenceVariables = new ArrayList<>();

        for (CosimStepInstruction instruction : CollectionConverters.asJava(algebraicLoopInstruction.iterate())) {
            if (instruction instanceof GetTentative) {
                Map<PortFmi2Api, VariableFmi2Api<Object>> portsWithGets =
                        handleGetTentativeInstruction(((GetTentative) instruction).port(), fmuInstances, dynamicScope);
                portsWithGets.forEach((port, portValue) -> {
                    tentativePortMapVars.stream().filter(entry -> entry.getKey().getLogScalarVariableName().contains(port.getLogScalarVariableName()))
                            .findAny().ifPresentOrElse(item -> {
                    }, () -> tentativePortMapVars.add(Map.entry(port, portValue)));

                    convergedPortRefs.stream().filter(ref -> port.aMablFmi2ComponentAPI.getName().contains(ref.fmu().toLowerCase(Locale.ROOT)) &&
                            port.getName().contains(ref.port().toLowerCase(Locale.ROOT))).findAny().ifPresent(portRef -> {
                        BooleanVariableFmi2Api isClose = dynamicScope.store("isClose", false);
                        isClose.setValue(mathModule.checkConvergence(port.getSharedAsVariable(), portValue, absTol, relTol));
                        dynamicScope.enterIf(isClose.toPredicate().not());
                        {
                            loggerModule.trace("Unstable signal %s = %.15E during algebraic loop",
                                    masterMRepresentationToMultiMRepresentation(portRef.fmu()) + MULTI_MODEL_FMU_INSTANCE_DELIMITER + portRef.port(),
                                    portValue);
                            dynamicScope.leave();
                        }
                        convergenceVariables.add(isClose);
                    });
                });
            } else if (instruction instanceof SetTentative) {
                handleSetTentativeInstruction(((SetTentative) instruction).port(), tentativePortMapVars, fmuInstances, connections);
            } else {
                fmuWithStep = mapCoSimStepInstructionsToMaBL(List.of(instruction), fmuInstances, currentCommunicationPoint, fmuStates, dynamicScope,
                        booleanLogicModule, mathModule, loggerModule, executionParameters, connections, portMapVars, currentStepSize);
            }
        }

        tentativePortMapVars.forEach(tentativePortMapEntry -> {
            fmuInstances.get(tentativePortMapEntry.getKey().getLogScalarVariableName().split("\\.")[1]).share(Map.ofEntries(tentativePortMapEntry));

            portMapVars.stream().filter(portMapVarEntry -> portMapVarEntry.getKey().getLogScalarVariableName()
                    .contains(tentativePortMapEntry.getKey().getLogScalarVariableName())).findAny().ifPresentOrElse(item -> {
            }, () -> portMapVars.add(Map.entry(tentativePortMapEntry.getKey(), tentativePortMapEntry.getValue())));
        });


        convergenceReached.setValue(booleanLogicModule.allTrue("convergence", convergenceVariables));

        dynamicScope.enterIf(convergenceReached.toPredicate().not());
        {
            mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(algebraicLoopInstruction.ifRetryNeeded()), fmuInstances,
                    currentCommunicationPoint, fmuStates, dynamicScope, booleanLogicModule, mathModule, loggerModule, executionParameters,
                    connections, portMapVars, currentStepSize);
            stepConvergeAttempts.decrement();
        }
        dynamicScope.leave();
        convergenceScope.leave();
        return fmuWithStep;
    }

    private static void handleAlgebraicLoopInitializationInstruction(AlgebraicLoopInit instruction,
            Map<String, ComponentVariableFmi2Api> fmuInstances, DynamicActiveBuilderScope dynamicScope, BooleanBuilderFmi2Api booleanLogicModule,
            MathBuilderFmi2Api mathModule, LoggerFmi2Api loggerModule, ScenarioConfiguration.ExecutionParameters executionParameters,
            List<ConnectionModel> connections, List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet) {

        DoubleVariableFmi2Api initAbsTol =
                dynamicScope.store("initialization_absolute_tolerance", executionParameters.getConvergenceAbsoluteTolerance());
        DoubleVariableFmi2Api initRelTol =
                dynamicScope.store("initialization_relative_tolerance", executionParameters.getConvergenceRelativeTolerance());
        BooleanVariableFmi2Api convergenceReached = dynamicScope.store("initialization_has_converged", false);
        IntVariableFmi2Api initConvergenceAttempts = dynamicScope.store("converge_attempts", executionParameters.getConvergenceAttempts());
        ScopeFmi2Api convergenceScope = dynamicScope
                .enterWhile(convergenceReached.toPredicate().not().and(initConvergenceAttempts.toMath().greaterThan(IntExpressionValue.of(0))));

        mapInitializationActionsToMaBL(CollectionConverters.asJava(instruction.iterate()), fmuInstances, dynamicScope, booleanLogicModule, mathModule,
                loggerModule, executionParameters, connections);

        generateCheckForConvergenceMaBLSection(initAbsTol, initRelTol, convergenceReached, CollectionConverters.asJava(instruction.untilConverged()),
                dynamicScope, mathModule, loggerModule, booleanLogicModule, portsWithGet);

        dynamicScope.enterIf(convergenceReached.toPredicate().not());
        {
            initConvergenceAttempts.decrement();
        }
        convergenceScope.leave();
    }

    private static void generateCheckForConvergenceMaBLSection(DoubleVariableFmi2Api absTol, DoubleVariableFmi2Api relTol,
            BooleanVariableFmi2Api convergenceReached, List<PortRef> untilConvergedPortRefs, DynamicActiveBuilderScope dynamicScope,
            MathBuilderFmi2Api mathModule, LoggerFmi2Api loggerModule, BooleanBuilderFmi2Api booleanLogicModule,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portMapVars) {

        List<BooleanVariableFmi2Api> convergenceVariables = new ArrayList<>();
        for (PortRef portRef : untilConvergedPortRefs) {
            portMapVars.stream().filter(entry -> entry.getKey().aMablFmi2ComponentAPI.getName().contains(portRef.fmu().toLowerCase(Locale.ROOT)) &&
                    entry.getKey().getName().contains(portRef.port().toLowerCase(Locale.ROOT))).findAny().ifPresent(portMap -> {
                VariableFmi2Api oldVariable = portMap.getKey().getSharedAsVariable();
                VariableFmi2Api<Object> newVariable = portMap.getValue();
                BooleanVariableFmi2Api isClose = dynamicScope.store("isClose", false);
                isClose.setValue(mathModule.checkConvergence(oldVariable, newVariable, absTol, relTol));
                dynamicScope.enterIf(isClose.toPredicate().not());
                {
                    loggerModule.trace("Unstable signal %s = %.15E during algebraic loop",
                            masterMRepresentationToMultiMRepresentation(portRef.fmu()) + MULTI_MODEL_FMU_INSTANCE_DELIMITER + portRef.port(),
                            portMap.getValue());
                    dynamicScope.leave();
                }
                convergenceVariables.add(isClose);
            });
        }
        convergenceReached.setValue(booleanLogicModule.allTrue("convergence", convergenceVariables));
    }

    private static void handleSetInstruction(PortRef portRef, List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet,
            Map<String, ComponentVariableFmi2Api> fmuInstances, List<ConnectionModel> connections) {
        Map.Entry<PortFmi2Api, VariableFmi2Api<Object>> sourcePortWithValue = getSourcePortFromPortRef(portRef, portsWithGet, connections);
        if (sourcePortWithValue != null) {
            Map.Entry<ComponentVariableFmi2Api, PortFmi2Api> instanceWithPort = getInstanceWithPortFromPortRef(portRef, fmuInstances);
            instanceWithPort.getKey().set(instanceWithPort.getValue(), sourcePortWithValue.getKey().getSharedAsVariable());
        }
    }

    private static void handleSetTentativeInstruction(PortRef portRef, List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet,
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

    private static Map<PortFmi2Api, VariableFmi2Api<Object>> handleGetTentativeInstruction(PortRef portRef,
            Map<String, ComponentVariableFmi2Api> fmuInstances, DynamicActiveBuilderScope scope) {
        return fmuInstances.get(portRef.fmu()).getTentative(scope, portRef.port());
    }

    private static Map<PortFmi2Api, VariableFmi2Api<Object>> handleGetInstruction(PortRef portRef,
            Map<String, ComponentVariableFmi2Api> fmuInstances) {
        ComponentVariableFmi2Api instance = fmuInstances.get(portRef.fmu());
        Map<PortFmi2Api, VariableFmi2Api<Object>> portMapVar;
        portMapVar = instance.get(portRef.port());
        instance.share(portMapVar);
        return portMapVar;
    }

    private static String masterMRepresentationToMultiMRepresentation(String instanceRepresentation) {
        return instanceRepresentation.replace(SCENARIO_MODEL_FMU_INSTANCE_DELIMITER, MULTI_MODEL_FMU_INSTANCE_DELIMITER);
    }

    private static String removeBraces(String toRemove) {
        return toRemove.replaceAll("[{}]", "");
    }

    //TODO: check that this can not be done directly in the builder if not create issue.
    private static PStm createComponentsArray(String lexName, Set<String> keySet) {
        return MableAstFactory.newALocalVariableStm(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier(lexName),
                MableAstFactory.newAArrayType(MableAstFactory.newANameType(FMI2COMPONENT_TYPE)), keySet.size(), MableAstFactory.newAArrayInitializer(
                        keySet.stream().map(x -> MableAstFactory.newAIdentifierExp(MableAstFactory.newAIdentifier(x)))
                                .collect(Collectors.toList()))));
    }
}
