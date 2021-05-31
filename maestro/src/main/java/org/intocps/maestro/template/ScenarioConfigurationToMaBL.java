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
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.WhileMaBLScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.*;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import scala.jdk.javaapi.CollectionConverters;

import javax.xml.xpath.XPathExpressionException;
import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;

public class ScenarioConfigurationToMaBL implements IMaBLTemplateGenerator {
    private final String START_TIME_NAME = "START_TIME";
    private final String END_TIME_NAME = "END_TIME";
    private final String STEP_SIZE_NAME = "STEP_SIZE";
    private final String FMI2COMPONENT_TYPE = "FMI2Component";
    private final String COMPONENTS_ARRAY_NAME = "components";
    private final String SCENARIO_MODEL_FMU_INSTANCE_DELIMITER = "_";
    private final String MULTI_MODEL_FMU_INSTANCE_DELIMITER = ".";
    private final List<ConnectionModel> connections;
    private final MasterModel masterModel;
    private final Fmi2SimulationEnvironment simulationEnvironment;
    private final Map<String, Object> parameters;
    private final Double convergenceAbsTol;
    private final Double convergenceRelTol;
    private final int convergenceAttempts;
    private MathBuilderFmi2Api mathModule;
    private LoggerFmi2Api loggerModule;
    private BooleanBuilderFmi2Api booleanLogicModule;
    private DynamicActiveBuilderScope dynamicScope;

    public ScenarioConfigurationToMaBL(ScenarioConfiguration configuration) {
        connections = CollectionConverters.asJava(configuration.getMasterModel().scenario().connections());
        masterModel = configuration.getMasterModel();
        simulationEnvironment = configuration.getSimulationEnvironment();
        parameters = configuration.getParameters();
        convergenceAbsTol = configuration.getConvergenceAbsoluteTolerance();
        convergenceRelTol = configuration.getConvergenceRelativeTolerance();
        convergenceAttempts = configuration.getConvergenceAttempts();
    }

    @Override
    public ASimulationSpecificationCompilationUnit generateTemplate() throws AnalysisException {

        //TODO: A Scenario and a multi-model does not agree on the format of identifying a FMU/instance.
        // E.g: FMU in a multi-model is defined as: "{<fmu-name>}" where in a scenario no curly braces are used i.e. "<fmu-name>". Furthermore an
        // instance in a multi-model is uniquely identified as: "{<fmu-name>}.<instance-name>" where instances are not really considered in scenarios
        // but could be expressed as: "<fmu-name>_<instance_name>".

        if(!modelsMatch()){
            //TODO: Handle models not matching
        }

        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        MablApiBuilder builder = new MablApiBuilder(settings, false);
        dynamicScope = builder.getDynamicScope();

        // GENERATE MaBL
        mathModule = builder.getMathBuilder();
        loggerModule = builder.getLogger();
        booleanLogicModule = builder.getBooleanBuilder();
        DataWriter.DataWriterInstance dataWriterInstance = builder.getDataWriter().createDataWriterInstance();
        DoubleVariableFmi2Api coSimEndTime = dynamicScope.store(END_TIME_NAME, 10.0);
        DoubleVariableFmi2Api coSimStartTime = dynamicScope.store(START_TIME_NAME, 0.0);

        List<FmuVariableFmi2Api> fmus = simulationEnvironment.getFmusWithModelDescriptions().stream()
                .filter(entry -> simulationEnvironment.getUriFromFMUName(entry.getKey()) != null).map(entry -> {
                    try {
                        return dynamicScope
                                .createFMU(removeBraces(entry.getKey()), entry.getValue(), simulationEnvironment.getUriFromFMUName(entry.getKey()));
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to create FMU variable: " + e.getLocalizedMessage());
                    }
                }).collect(Collectors.toList());

        // Generate fmu instances with identifying names from the mater model.
        Map<String, ComponentVariableFmi2Api> fmuInstances =
                CollectionConverters.asJava(masterModel.scenario().fmus()).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    Optional<FmuVariableFmi2Api> fmuFromScenario =
                            fmus.stream().filter(fmu -> fmu.getName().contains(entry.getKey().split(SCENARIO_MODEL_FMU_INSTANCE_DELIMITER)[0]))
                                    .findAny();
                    if (fmuFromScenario.isEmpty()) {
                        throw new RuntimeException("Unable to match fmu from multi model with fmu from master model");
                    }
                    return fmuFromScenario.get().instantiate(entry.getKey());
                }));

        //        Map<String, ComponentVariableFmi2Api> fmuInstances = fmus.stream()
        //                .collect(Collectors.toMap(fmu -> fmu.getName().replaceAll("[{}]", ""), fmu -> fmu.instantiate(fmu.getName().replaceAll("[{}]", ""))));

        dynamicScope.add(createComponentsArray(COMPONENTS_ARRAY_NAME,
                fmuInstances.values().stream().map(ComponentVariableFmi2Api::getName).collect(Collectors.toSet())));

        dataWriterInstance.initialize(new ArrayList<>(
                connections.stream().map(connection -> fmuInstances.get(connection.srcPort().fmu()).getPort(connection.srcPort().port()))
                        .collect(Collectors.toSet())));

        // Generate setup experiment section
        fmuInstances.values().forEach(instance -> instance.setupExperiment(coSimStartTime, coSimEndTime, convergenceRelTol));

        // Generate set parameters section
        setParameters(fmuInstances);

        // Generate initialization section
        mapInitializationActionsToMaBL(CollectionConverters.asJava(masterModel.initialization()), fmuInstances);

        // Generate step loop section
        //TODO: Should "get calls" from initialization section be available in the step loop section?
        DoubleVariableFmi2Api currentCommunicationPoint = dynamicScope.store("current_communication_point", 0.0);
        currentCommunicationPoint.setValue(coSimStartTime);
        //TODO: Is maxPossibleStepSize == defaultCommunicationStepSize??
        //DoubleVariableFmi2Api stepSize = dynamicScope.store("step_size", (double) masterModel.scenario().maxPossibleStepSize());
        DoubleVariableFmi2Api stepSize = dynamicScope.store(STEP_SIZE_NAME, 0.1);

        dataWriterInstance.log(currentCommunicationPoint);

        WhileMaBLScope coSimStepLoop = dynamicScope.enterWhile(currentCommunicationPoint.toMath().lessThan(coSimEndTime));

        mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(masterModel.cosimStep()), fmuInstances, currentCommunicationPoint, stepSize,
                new HashSet<>());

        currentCommunicationPoint.setValue(currentCommunicationPoint.toMath().addition(stepSize));
        dataWriterInstance.log(currentCommunicationPoint);
        coSimStepLoop.leave();

        fmus.forEach(fmu -> fmu.unload(dynamicScope));

        //org.intocps.maestro.ast.display.PrettyPrinter.print(builder.build());

        return builder.build();
    }

    private boolean modelsMatch() {
        boolean mismatch = false;
        Map<String, FmuModel> fmuInstancesInMasterModel = CollectionConverters.asJava(masterModel.scenario().fmus());
        // Verify that fmu identifiers and fmu instance names matches between the multi model and master model and that reject step and get-set
        // state agrees.
        for(Map.Entry<String, ComponentInfo> fmuInstanceFromMultiModel : simulationEnvironment.getInstances()){
            boolean instanceMatch = false;
            for(String masterModelFmuInstanceIdentifier : fmuInstancesInMasterModel.keySet()){
                if(masterModelFmuInstanceIdentifier.contains(removeBraces(fmuInstanceFromMultiModel.getValue().fmuIdentifier)) &&
                        masterModelFmuInstanceIdentifier.contains(fmuInstanceFromMultiModel.getKey())){
                    try {
                        if (simulationEnvironment.getModelDescription(fmuInstanceFromMultiModel.getValue().fmuIdentifier).getCanGetAndSetFmustate() !=
                                fmuInstancesInMasterModel.get(masterModelFmuInstanceIdentifier).canRejectStep()) {
                            //TODO: Log mismatch between reject step and get-set state.
                            mismatch = true;
                        }
                    } catch (XPathExpressionException e) {
                        throw new RuntimeException("Unable to access model description for fmu: " + fmuInstanceFromMultiModel.getValue().fmuIdentifier);
                    }
                    instanceMatch = true;
                    break;
                }
            }
            if(!instanceMatch){
                //TODO: Log mismatch between identifiers.
                mismatch = true;
            }
        }

        return mismatch;
    }

    private void setParameters(Map<String, ComponentVariableFmi2Api> fmuInstances) {
        fmuInstances.forEach((instanceName, fmuInstance) -> {
            Map<String, Object> portToValue =
                    parameters.entrySet().stream().filter(entry -> removeBraces(entry.getKey()).contains(masterMRepToMultiMRep(instanceName)))
                            .collect(Collectors.toMap(e -> e.getKey().split("\\" + MULTI_MODEL_FMU_INSTANCE_DELIMITER)[2], Map.Entry::getValue));

            Map<? extends Fmi2Builder.Port, ? extends Fmi2Builder.ExpressionValue> PortToExpressionValue =
                    portToValue.entrySet().stream().collect(Collectors.toMap(e -> fmuInstance.getPort(e.getKey()), e -> {
                        if (e.getValue() instanceof Double) {
                            return DoubleExpressionValue.of((Double) e.getValue());
                        }
                        if (e.getValue() instanceof Integer) {
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

    private void mapInitializationActionsToMaBL(List<InitializationInstruction> initializationInstructions,
            Map<String, ComponentVariableFmi2Api> fmuInstances) {

        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsToGet = new ArrayList<>();
        initializationInstructions.forEach(instruction -> {
            if (instruction instanceof InitGet) {
                handleGetInstruction(((InitGet) instruction).port(), portsToGet, fmuInstances);
            } else if (instruction instanceof InitSet) {
                handleSetInstruction(((InitSet) instruction).port(), portsToGet, fmuInstances);
            } else if (instruction instanceof AlgebraicLoopInit) {
                handleAlgebraicLoopInitializationInstruction((AlgebraicLoopInit) instruction, fmuInstances);
            } else if (instruction instanceof EnterInitMode) {
                fmuInstances.get(((EnterInitMode) instruction).fmu()).enterInitializationMode();
            } else if (instruction instanceof ExitInitMode) {
                fmuInstances.get(((ExitInitMode) instruction).fmu()).exitInitializationMode();
            } else {
                throw new RuntimeException("Unknown initialization instruction: " + instruction.toString());
            }
        });
    }

    private Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> mapCoSimStepInstructionsToMaBL(
            List<CosimStepInstruction> coSimStepInstructions, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DoubleVariableFmi2Api currentCommunicationPoint, DoubleVariableFmi2Api stepSize, Set<Fmi2Builder.StateVariable<PStm>> fmuStates) {

        //TODO: Handle tentative instructions
        Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuToStep = new HashMap<>();
        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsToGet = new ArrayList<>();
        for (CosimStepInstruction instruction : coSimStepInstructions) {
            if (instruction instanceof core.Set) {
                handleSetInstruction(((core.Set) instruction).port(), portsToGet, fmuInstances);
            } else if (instruction instanceof Get) {
                handleGetInstruction(((Get) instruction).port(), portsToGet, fmuInstances);
            } else if (instruction instanceof GetTentative) {

            } else if (instruction instanceof SetTentative) {

            } else if (instruction instanceof Step) {
                handleStepInstruction((Step) instruction, fmuInstances, fmuToStep, currentCommunicationPoint, stepSize);

            } else if (instruction instanceof SaveState) {
                String instanceName = ((SaveState) instruction).fmu();
                try {
                    fmuStates.add(fmuInstances.get(instanceName).getState());
                } catch (XPathExpressionException e) {
                    throw new RuntimeException("Could not get state for fmu instance: " + instanceName);
                }
            } else if (instruction instanceof RestoreState) {
                String restoreName = ((RestoreState) instruction).fmu();
                fmuStates.stream().filter(state -> state.getName().contains(restoreName)).findAny().ifPresent(Fmi2Builder.StateVariable::set);

            } else if (instruction instanceof AlgebraicLoop) {
                handleAlgebraicLoopCoSimStepInstruction((AlgebraicLoop) instruction, fmuInstances, currentCommunicationPoint, stepSize,
                        fmuStates);
            } else if (instruction instanceof StepLoop) {
                handleStepLoopCoSimStepInstruction((StepLoop) instruction, fmuInstances, currentCommunicationPoint, stepSize, fmuStates);
            } else {
                throw new RuntimeException("Unknown CoSimStep instruction: " + instruction.toString());
            }
        }

        return fmuToStep;
    }

    private void handleStepLoopCoSimStepInstruction(StepLoop instruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DoubleVariableFmi2Api currentCommunicationPoint, DoubleVariableFmi2Api stepSize, Set<Fmi2Builder.StateVariable<PStm>> fmuStates) {

        ArrayVariableFmi2Api<Double> fmuCommunicationPoints =
                dynamicScope.store("fmu_communication_points", new Double[fmuInstances.entrySet().size()]);

        BooleanVariableFmi2Api allAcceptedStep = dynamicScope.store("all_accepted_step", false);
        ScopeFmi2Api stepAcceptedScope = dynamicScope.enterWhile(allAcceptedStep.toPredicate().not());

        Map<ComponentVariableFmi2Api, Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>>> fmuToStep =
                mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(instruction.iterate()), fmuInstances, currentCommunicationPoint, stepSize,
                        new HashSet<>());

        allAcceptedStep.setValue(booleanLogicModule.allTrue("all_fmus_accepted_step_size", fmuToStep.entrySet().stream()
                .filter(entry -> CollectionConverters.asJava(instruction.untilStepAccept()).stream()
                        .anyMatch(name -> entry.getKey().getName().contains(name))).map(entry -> entry.getValue().getKey())
                .collect(Collectors.toList())));

        dynamicScope.enterIf(allAcceptedStep.toPredicate().not()).enterThen();
        {
            mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(instruction.ifRetryNeeded()), fmuInstances, currentCommunicationPoint,
                    stepSize, fmuStates);

            // Set the step size to the lowest accepted step-size
            List<Fmi2Builder.DoubleVariable<PStm>> stepSizes = fmuToStep.values().stream().map(Map.Entry::getValue).collect(Collectors.toList());
            for (int i = 0; i < stepSizes.size(); i++) {
                fmuCommunicationPoints.items().get(i).setValue(new DoubleExpressionValue(stepSizes.get(i).getExp()));
            }
            stepSize.setValue(mathModule.minRealFromArray(fmuCommunicationPoints).toMath().subtraction(currentCommunicationPoint));
        }
        dynamicScope.leave();
        stepAcceptedScope.leave();
    }

    private void handleStepInstruction(Step instruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
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

    private void handleAlgebraicLoopCoSimStepInstruction(AlgebraicLoop instruction, Map<String, ComponentVariableFmi2Api> fmuInstances,
            DoubleVariableFmi2Api currentCommunicationPoint, DoubleVariableFmi2Api defaultCommunicationStepSize,
            Set<Fmi2Builder.StateVariable<PStm>> fmuStates) {

        //TODO: Fix hardcoded values
        DoubleVariableFmi2Api stepAbsTol = dynamicScope.store("coSimStep_absolute_tolerance", convergenceAbsTol);
        DoubleVariableFmi2Api stepRelTol = dynamicScope.store("coSimStep_relative_tolerance", convergenceRelTol);
        BooleanVariableFmi2Api convergenceReached = dynamicScope.store("coSimStep_has_converged", false);
        IntVariableFmi2Api stepConvergeAttempts = dynamicScope.store("converge_attempts", convergenceAttempts);
        ScopeFmi2Api convergenceScope = dynamicScope
                .enterWhile(convergenceReached.toPredicate().not().and(stepConvergeAttempts.toMath().greaterThan(IntExpressionValue.of(0))));


        mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(instruction.iterate()), fmuInstances, currentCommunicationPoint,
                defaultCommunicationStepSize, new HashSet<>());

        generateCheckForConvergenceMaBLSection(stepAbsTol, stepRelTol, convergenceReached, CollectionConverters.asJava(instruction.untilConverged()),
                fmuInstances);


        dynamicScope.enterIf(convergenceReached.toPredicate().not()).enterThen();
        {
            mapCoSimStepInstructionsToMaBL(CollectionConverters.asJava(instruction.ifRetryNeeded()), fmuInstances, currentCommunicationPoint,
                    defaultCommunicationStepSize, fmuStates);
            stepConvergeAttempts.decrement();
            dynamicScope.leave();
        }
        convergenceScope.leave();
    }

    private void handleAlgebraicLoopInitializationInstruction(AlgebraicLoopInit instruction, Map<String, ComponentVariableFmi2Api> fmuInstances) {

        DoubleVariableFmi2Api initAbsTol = dynamicScope.store("initialization_absolute_tolerance", convergenceAbsTol);
        DoubleVariableFmi2Api initRelTol = dynamicScope.store("initialization_relative_tolerance", convergenceRelTol);
        BooleanVariableFmi2Api convergenceReached = dynamicScope.store("initialization_has_converged", false);
        IntVariableFmi2Api initConvergenceAttempts = dynamicScope.store("converge_attempts", convergenceAttempts);
        ScopeFmi2Api convergenceScope = dynamicScope
                .enterWhile(convergenceReached.toPredicate().not().and(initConvergenceAttempts.toMath().greaterThan(IntExpressionValue.of(0))));

        mapInitializationActionsToMaBL(CollectionConverters.asJava(instruction.iterate()), fmuInstances);

        generateCheckForConvergenceMaBLSection(initAbsTol, initRelTol, convergenceReached, CollectionConverters.asJava(instruction.untilConverged()),
                fmuInstances);

        initConvergenceAttempts.decrement();
        convergenceScope.leave();
    }

    private void generateCheckForConvergenceMaBLSection(DoubleVariableFmi2Api absTol, DoubleVariableFmi2Api relTol,
            BooleanVariableFmi2Api convergenceReached, List<PortRef> portRefs, Map<String, ComponentVariableFmi2Api> fmuInstances) {

        List<BooleanVariableFmi2Api> convergenceVariables = new ArrayList<>();
        for (PortRef portRef : portRefs) {
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

    private void handleSetInstruction(PortRef portRef, List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsWithGet,
            Map<String, ComponentVariableFmi2Api> fmuInstances) {
        Optional<ConnectionModel> connectionModel = connections.stream()
                .filter(connection -> connection.trgPort().port().equals(portRef.port()) && connection.trgPort().fmu().equals(portRef.fmu()))
                .findAny();

        if (connectionModel.isPresent()) {
            Optional<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> sourcePortWithValue = portsWithGet.stream().filter(entry ->
                    entry.getKey().getLogScalarVariableName().contains(masterMRepToMultiMRep(connectionModel.get().srcPort().fmu())) &&
                            entry.getKey().getLogScalarVariableName().contains(connectionModel.get().srcPort().port())).findAny();

            if (sourcePortWithValue.isPresent()) {
                ComponentVariableFmi2Api instance = fmuInstances.get(portRef.fmu());
                PortFmi2Api targetPort = instance.getPort(portRef.port());
                instance.set(targetPort, sourcePortWithValue.get().getKey().getSharedAsVariable());
            }
        }
    }

    private void handleGetInstruction(PortRef portRef, List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> portsToGet,
            Map<String, ComponentVariableFmi2Api> fmuInstances) {
        ComponentVariableFmi2Api instance = fmuInstances.get(portRef.fmu());
        Map<PortFmi2Api, VariableFmi2Api<Object>> portMap = instance.get(portRef.port());
        instance.share(portMap);

        Optional<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> oldPortToGet = portsToGet.stream()
                .filter(entry -> entry.getKey().getLogScalarVariableName().contains(portRef.fmu()) &&
                        entry.getKey().getLogScalarVariableName().contains(portRef.port())).findAny();

        oldPortToGet.ifPresent(portsToGet::remove);
        portsToGet.add(portMap.entrySet().iterator().next());
    }

    private String masterMRepToMultiMRep(String instanceRepresentation) {
        return instanceRepresentation.replace(SCENARIO_MODEL_FMU_INSTANCE_DELIMITER, MULTI_MODEL_FMU_INSTANCE_DELIMITER);
    }

    private String removeBraces(String toRemove) {
        return toRemove.replaceAll("[{}]", "");
    }

    private PStm createComponentsArray(String lexName, Set<String> keySet) {
        return MableAstFactory.newALocalVariableStm(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier(lexName),
                MableAstFactory.newAArrayType(MableAstFactory.newANameType(FMI2COMPONENT_TYPE)), keySet.size(), MableAstFactory.newAArrayInitializer(
                        keySet.stream().map(x -> MableAstFactory.newAIdentifierExp(MableAstFactory.newAIdentifier(x)))
                                .collect(Collectors.toList()))));
    }
}
