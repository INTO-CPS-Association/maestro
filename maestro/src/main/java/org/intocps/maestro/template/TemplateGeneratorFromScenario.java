package org.intocps.maestro.template;

import core.*;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.*;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IfMaBlScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.WhileMaBLScope;
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

    public static ASimulationSpecificationCompilationUnit generateTemplate(ScenarioConfiguration configuration) throws AnalysisException {


        //TODO: A Scenario and a multi-model does not agree on the format of identifying a FMU/instance.
        // E.g: FMU in a multi-model is defined as: "{<fmu-name>}" where in a scenario no curly braces are used i.e. "<fmu-name>". Furthermore an
        // instance in a multi-model is uniquely identified as: "{<fmu-name>}.<instance-name>" where instances are not really considered in scenarios
        // but could be expressed as: "<fmu-name>_<instance_name>".

        if (!modelsMatch(configuration.getMasterModel(), configuration.getSimulationEnvironment())) {
            //TODO: Handle models not matching
        }

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

        //TODO: Use initializer expansion plugin if no initializer sequence is passed.
//        dynamicScope.add(createComponentsArray(COMPONENTS_ARRAY_NAME,
//                fmuInstances.values().stream().map(ComponentVariableFmi2Api::getName).collect(Collectors.toSet())));

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
                currentCommunicationPoint, stepSize, new HashSet<>(), dynamicScope, booleanLogicModule, mathModule, loggerModule,
                configuration.getExecutionParameters(), CollectionConverters.asJava(configuration.getMasterModel().scenario().connections()),
                portsWithGets, currentStepSize);

        currentCommunicationPoint.setValue(currentCommunicationPoint.toMath().addition(currentStepSize));
        dataWriterInstance.log(currentCommunicationPoint);
        coSimStepLoop.leave();
        dataWriterInstance.close();

        fmus.forEach(fmu -> fmu.unload(dynamicScope));

        return builder.build();
    }

    private static boolean modelsMatch(MasterModel masterModel, Fmi2SimulationEnvironment simulationEnvironment) {
        boolean mismatch = false;
        Map<String, FmuModel> fmuInstancesInMasterModel = CollectionConverters.asJava(masterModel.scenario().fmus());
        // Verify that fmu identifiers and fmu instance names matches between the multi model and master model and that reject step and get-set
        // state agrees.
        for (Map.Entry<String, ComponentInfo> fmuInstanceFromMultiModel : simulationEnvironment.getInstances()) {
            boolean instanceMatch = false;
            for (String masterModelFmuInstanceIdentifier : fmuInstancesInMasterModel.keySet()) {
                if (masterModelFmuInstanceIdentifier.contains(removeBraces(fmuInstanceFromMultiModel.getValue().fmuIdentifier)) &&
                        masterModelFmuInstanceIdentifier.contains(fmuInstanceFromMultiModel.getKey())) {
                    try {
                        if (simulationEnvironment.getModelDescription(fmuInstanceFromMultiModel.getValue().fmuIdentifier).getCanGetAndSetFmustate() !=
                                fmuInstancesInMasterModel.get(masterModelFmuInstanceIdentifier).canRejectStep()) {
                            //TODO: Log mismatch between reject step and get-set state.
                            mismatch = true;
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
                //TODO: Log mismatch between identifiers.
                mismatch = true;
            }
        }

        return mismatch;
    }

    private static void setParameters(Map<String, ComponentVariableFmi2Api> fmuInstances, Map<String, Object> parameters) {
        fmuInstances.forEach((instanceName, fmuInstance) -> {
            Map<String, Object> portToValue =
                    parameters.entrySet().stream().filter(entry -> removeBraces(entry.getKey()).contains(masterMRepToMultiMRep(instanceName)))
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

        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet = new ArrayList<>();
        initializationInstructions.forEach(instruction -> {
            if (instruction instanceof InitGet) {
                handleGetInstruction(((InitGet) instruction).port(), portsWithGet, fmuInstances, false);
            } else if (instruction instanceof InitSet) {
                handleSetInstruction(((InitSet) instruction).port(), portsWithGet, fmuInstances, connections, false);
            } else if (instruction instanceof AlgebraicLoopInit) {
                handleAlgebraicLoopInitializationInstruction((AlgebraicLoopInit) instruction, fmuInstances, dynamicScope, booleanLogicModule,
                        mathModule, loggerModule, executionParameters, connections);
            } else if (instruction instanceof EnterInitMode) {
                fmuInstances.get(((EnterInitMode) instruction).fmu()).enterInitializationMode();
            } else if (instruction instanceof ExitInitMode) {
                fmuInstances.get(((ExitInitMode) instruction).fmu()).exitInitializationMode();
            } else {
                throw new RuntimeException("Unknown initialization instruction: " + instruction.toString());
            }
        });
        return portsWithGet;
    }

    private static Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> mapCoSimStepInstructionsToMaBL(
            List<CosimStepInstruction> coSimStepInstructions, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DoubleVariableFmi2Api currentCommunicationPoint, DoubleVariableFmi2Api stepSize, Set<Fmi2Builder.StateVariable<PStm>> fmuStates,
            DynamicActiveBuilderScope dynamicScope, BooleanBuilderFmi2Api booleanLogicModule, MathBuilderFmi2Api mathModule,
            LoggerFmi2Api loggerModule, ScenarioConfiguration.ExecutionParameters executionParameters, List<ConnectionModel> connections,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet, DoubleVariableFmi2Api currentStepSize) {

        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> priorPortsWithGet = new ArrayList<>(portsWithGet);
        Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuWithStep = new HashMap<>();
        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithTentativeGet = new ArrayList<>();
        for (CosimStepInstruction instruction : coSimStepInstructions) {
            if (instruction instanceof core.Set) {
                handleSetInstruction(((core.Set) instruction).port(), priorPortsWithGet, fmuInstances, connections, false);
            } else if (instruction instanceof Get) {
                handleGetInstruction(((Get) instruction).port(), priorPortsWithGet, fmuInstances, false);
            } else if (instruction instanceof GetTentative) {
                handleGetInstruction(((GetTentative) instruction).port(), portsWithTentativeGet, fmuInstances, true);
            } else if (instruction instanceof SetTentative) {
                handleSetInstruction(((SetTentative) instruction).port(), priorPortsWithGet, fmuInstances, connections, true);
            } else if (instruction instanceof Step) {
                handleStepInstruction((Step) instruction, fmuInstances, fmuWithStep, currentCommunicationPoint, currentStepSize);

            } else if (instruction instanceof SaveState) {
                String instanceName = ((SaveState) instruction).fmu();
                try {
                    fmuStates.add(fmuInstances.get(instanceName).getState());
                } catch (XPathExpressionException e) {
                    throw new RuntimeException("Could not get state for fmu instance: " + instanceName);
                }
            } else if (instruction instanceof RestoreState) {
                String restoreName = ((RestoreState) instruction).fmu();
                fmuStates.stream().filter(state -> state.getName().contains(restoreName.toLowerCase(Locale.ROOT))).findAny()
                        .ifPresent(Fmi2Builder.StateVariable::set);

            } else if (instruction instanceof AlgebraicLoop) {
                handleAlgebraicLoopCoSimStepInstruction((AlgebraicLoop) instruction, fmuInstances, currentCommunicationPoint, currentStepSize,
                        fmuStates, dynamicScope, booleanLogicModule, mathModule, loggerModule, executionParameters, connections, priorPortsWithGet, currentStepSize);
            } else if (instruction instanceof StepLoop) {
                handleStepLoopCoSimStepInstruction((StepLoop) instruction, fmuInstances, currentCommunicationPoint, stepSize, fmuStates, dynamicScope,
                        booleanLogicModule, mathModule, loggerModule, executionParameters, connections, priorPortsWithGet, currentStepSize);
            } else {
                throw new RuntimeException("Unknown CoSimStep instruction: " + instruction.toString());
            }
        }
        portsWithTentativeGet.forEach(portMapEntry -> {
            String masterModelInstanceName =
                    removeBraces(portMapEntry.getKey().getLogScalarVariableName().split("\\.")[0]) + SCENARIO_MODEL_FMU_INSTANCE_DELIMITER +
                            portMapEntry.getKey().getLogScalarVariableName().split("\\.")[1];
            fmuInstances.get(masterModelInstanceName).share(Map.ofEntries(portMapEntry));

        });

        return fmuWithStep;
    }

    private static void handleStepLoopCoSimStepInstruction(StepLoop instruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DoubleVariableFmi2Api currentCommunicationPoint, DoubleVariableFmi2Api originalStepSize, Set<Fmi2Builder.StateVariable<PStm>> fmuStates,
            DynamicActiveBuilderScope dynamicScope, BooleanBuilderFmi2Api booleanLogicModule, MathBuilderFmi2Api mathModule,
            LoggerFmi2Api loggerModule, ScenarioConfiguration.ExecutionParameters executionParameters, List<ConnectionModel> connections,
            List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet, DoubleVariableFmi2Api currentStepSize) {

        ArrayVariableFmi2Api<Double> fmuCommunicationPoints =
                dynamicScope.store("fmu_communication_points", new Double[fmuInstances.entrySet().size()]);

        BooleanVariableFmi2Api allAcceptedStep = dynamicScope.store("all_accepted_step", false);
        ScopeFmi2Api stepAcceptedScope = dynamicScope.enterWhile(allAcceptedStep.toPredicate().not());

        Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmusWithStep =
                mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(instruction.iterate()), fmuInstances, currentCommunicationPoint,
                        currentStepSize, new HashSet<>(), dynamicScope, booleanLogicModule, mathModule, loggerModule, executionParameters,
                        connections, portsWithGet, currentStepSize);

        List<Fmi2Builder.BoolVariable<PStm>> accepted = fmusWithStep.entrySet().stream()
                .filter(entry -> CollectionConverters.asJava(instruction.untilStepAccept()).stream()
                        .anyMatch(name -> entry.getKey().getName().contains(name.toLowerCase(Locale.ROOT)))).map(entry -> entry.getValue().getKey())
                .collect(Collectors.toList());

        allAcceptedStep.setValue(booleanLogicModule.allTrue("all_fmus_accepted_step_size", accepted));

        IfMaBlScope allAcceptedScope = dynamicScope.enterIf(allAcceptedStep.toPredicate().not());
        {
            mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(instruction.ifRetryNeeded()), fmuInstances, currentCommunicationPoint,
                    currentStepSize, fmuStates, dynamicScope, booleanLogicModule, mathModule, loggerModule, executionParameters, connections,
                    portsWithGet, currentStepSize);

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

    private static void handleStepInstruction(Step instruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuToStep,
            DoubleVariableFmi2Api currentCommunicationPoint, DoubleVariableFmi2Api defaultCommunicationStepSize) {

        ComponentVariableFmi2Api instance = fmuInstances.get(instruction.fmu());
        Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>> step;

        if (instruction.by() instanceof AbsoluteStepSize) {
            step = instance.step(currentCommunicationPoint,
                    new DoubleVariableFmi2Api(null, null, null, null, DoubleExpressionValue.of(((AbsoluteStepSize) instruction.by()).H()).getExp()));
        } else if (instruction.by() instanceof RelativeStepSize) {
            ComponentVariableFmi2Api fmuInstance = fmuInstances.get(((RelativeStepSize) instruction.by()).fmu());
            step = instance.step(currentCommunicationPoint, fmuToStep.get(fmuInstance).getValue());
        } else {
            step = instance.step(currentCommunicationPoint, defaultCommunicationStepSize);
        }
        fmuToStep.put(instance, step);
    }

    private static void handleAlgebraicLoopCoSimStepInstruction(AlgebraicLoop instruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DoubleVariableFmi2Api currentCommunicationPoint, DoubleVariableFmi2Api defaultCommunicationStepSize,
            Set<Fmi2Builder.StateVariable<PStm>> fmuStates, DynamicActiveBuilderScope dynamicScope, BooleanBuilderFmi2Api booleanLogicModule,
            MathBuilderFmi2Api mathModule, LoggerFmi2Api loggerModule, ScenarioConfiguration.ExecutionParameters executionParameters,
            List<ConnectionModel> connections, List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet, DoubleVariableFmi2Api currentStepSize) {

        DoubleVariableFmi2Api stepAbsTol = dynamicScope.store("coSimStep_absolute_tolerance", executionParameters.getConvergenceAbsoluteTolerance());
        DoubleVariableFmi2Api stepRelTol = dynamicScope.store("coSimStep_relative_tolerance", executionParameters.getConvergenceRelativeTolerance());
        BooleanVariableFmi2Api convergenceReached = dynamicScope.store("coSimStep_has_converged", false);
        IntVariableFmi2Api stepConvergeAttempts = dynamicScope.store("converge_attempts", executionParameters.getConvergenceAttempts());
        ScopeFmi2Api convergenceScope = dynamicScope
                .enterWhile(convergenceReached.toPredicate().not().or(stepConvergeAttempts.toMath().greaterThan(IntExpressionValue.of(0))));


        mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(instruction.iterate()), fmuInstances, currentCommunicationPoint,
                defaultCommunicationStepSize, new HashSet<>(), dynamicScope, booleanLogicModule, mathModule, loggerModule, executionParameters,
                connections, portsWithGet, currentStepSize);

        generateCheckForConvergenceMaBLSection(stepAbsTol, stepRelTol, convergenceReached, CollectionConverters.asJava(instruction.untilConverged()),
                fmuInstances, dynamicScope, mathModule, loggerModule, booleanLogicModule);


        dynamicScope.enterIf(convergenceReached.toPredicate().not());
        {
            mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(instruction.ifRetryNeeded()), fmuInstances, currentCommunicationPoint,
                    defaultCommunicationStepSize, fmuStates, dynamicScope, booleanLogicModule, mathModule, loggerModule, executionParameters,
                    connections, portsWithGet, currentStepSize);
            stepConvergeAttempts.decrement();
        }
        dynamicScope.leave();
        convergenceScope.leave();
    }

    private static void handleAlgebraicLoopInitializationInstruction(AlgebraicLoopInit instruction,
            Map<String, ComponentVariableFmi2Api> fmuInstances, DynamicActiveBuilderScope dynamicScope, BooleanBuilderFmi2Api booleanLogicModule,
            MathBuilderFmi2Api mathModule, LoggerFmi2Api loggerModule, ScenarioConfiguration.ExecutionParameters executionParameters,
            List<ConnectionModel> connections) {

        DoubleVariableFmi2Api initAbsTol =
                dynamicScope.store("initialization_absolute_tolerance", executionParameters.getConvergenceAbsoluteTolerance());
        DoubleVariableFmi2Api initRelTol =
                dynamicScope.store("initialization_relative_tolerance", executionParameters.getConvergenceRelativeTolerance());
        BooleanVariableFmi2Api convergenceReached = dynamicScope.store("initialization_has_converged", false);
        IntVariableFmi2Api initConvergenceAttempts = dynamicScope.store("converge_attempts", executionParameters.getConvergenceAttempts());
        ScopeFmi2Api convergenceScope = dynamicScope
                .enterWhile(convergenceReached.toPredicate().not().or(initConvergenceAttempts.toMath().greaterThan(IntExpressionValue.of(0))));

        mapInitializationActionsToMaBL(CollectionConverters.asJava(instruction.iterate()), fmuInstances, dynamicScope, booleanLogicModule, mathModule,
                loggerModule, executionParameters, connections);

        generateCheckForConvergenceMaBLSection(initAbsTol, initRelTol, convergenceReached, CollectionConverters.asJava(instruction.untilConverged()),
                fmuInstances, dynamicScope, mathModule, loggerModule, booleanLogicModule);

        dynamicScope.enterIf(convergenceReached.toPredicate().not());
        {
            initConvergenceAttempts.decrement();
        }
        convergenceScope.leave();
    }

    private static void generateCheckForConvergenceMaBLSection(DoubleVariableFmi2Api absTol, DoubleVariableFmi2Api relTol,
            BooleanVariableFmi2Api convergenceReached, List<PortRef> untilConvergedPortRefs, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DynamicActiveBuilderScope dynamicScope, MathBuilderFmi2Api mathModule, LoggerFmi2Api loggerModule,
            BooleanBuilderFmi2Api booleanLogicModule) {

        List<BooleanVariableFmi2Api> convergenceVariables = new ArrayList<>();
        for (PortRef portRef : untilConvergedPortRefs) {
            Map<PortFmi2Api, VariableFmi2Api<Object>> portMap = fmuInstances.get(portRef.fmu()).get(portRef.port());
            VariableFmi2Api oldVariable = portMap.entrySet().iterator().next().getKey().getSharedAsVariable();
            VariableFmi2Api<Object> newVariable = portMap.entrySet().iterator().next().getValue();
            BooleanVariableFmi2Api isClose = dynamicScope.store("isClose", false);
            isClose.setValue(mathModule.checkConvergence(oldVariable, newVariable, absTol, relTol));
            dynamicScope.enterIf(isClose.toPredicate().not());
            {
                loggerModule.trace("Unstable signal %s = %.15E during algebraic loop",
                        masterMRepToMultiMRep(portRef.fmu()) + MULTI_MODEL_FMU_INSTANCE_DELIMITER + portRef.port(),
                        portMap.entrySet().iterator().next().getValue());
                dynamicScope.leave();
            }
            convergenceVariables.add(isClose);
        }

        convergenceReached.setValue(booleanLogicModule.allTrue("convergence", convergenceVariables));
    }

    private static void handleSetInstruction(PortRef portRef, List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet,
            Map<String, ComponentVariableFmi2Api> fmuInstances, List<ConnectionModel> connections, boolean isTentative) {
        Optional<ConnectionModel> connectionModel = connections.stream()
                .filter(connection -> connection.trgPort().port().equals(portRef.port()) && connection.trgPort().fmu().equals(portRef.fmu()))
                .findAny();

        if (connectionModel.isPresent()) {
            Optional<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> sourcePortWithValue = portsWithGet.stream().filter(entry ->
                    entry.getKey().aMablFmi2ComponentAPI.getName().contains(connectionModel.get().srcPort().fmu().toLowerCase(Locale.ROOT)) &&
                            entry.getKey().getName().contains(connectionModel.get().srcPort().port().toLowerCase(Locale.ROOT))).findAny();

            if (sourcePortWithValue.isPresent()) {
                ComponentVariableFmi2Api instance = fmuInstances.get(portRef.fmu());
                PortFmi2Api targetPort = instance.getPort(portRef.port());
                if (isTentative) {
                    instance.set(targetPort, sourcePortWithValue.get().getValue());
                } else {
                    instance.set(targetPort, sourcePortWithValue.get().getKey().getSharedAsVariable());
                }
            }
        }
    }

    private static void handleGetInstruction(PortRef portRef, List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet,
            Map<String, ComponentVariableFmi2Api> fmuInstances, boolean isTentative) {
        ComponentVariableFmi2Api instance = fmuInstances.get(portRef.fmu());
        Map<PortFmi2Api, VariableFmi2Api<Object>> portMap = instance.get(portRef.port());
        if (!isTentative) {
            instance.share(portMap);
        }
        Optional<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> oldPortToGet = portsWithGet.stream()
                .filter(entry -> entry.getKey().aMablFmi2ComponentAPI.getName().contains(portRef.fmu().toLowerCase(Locale.ROOT)) &&
                        entry.getKey().getName().contains(portRef.port().toLowerCase(Locale.ROOT))).findAny();

        oldPortToGet.ifPresent(portsWithGet::remove);
        portsWithGet.add(portMap.entrySet().iterator().next());
    }

    private static String masterMRepToMultiMRep(String instanceRepresentation) {
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
