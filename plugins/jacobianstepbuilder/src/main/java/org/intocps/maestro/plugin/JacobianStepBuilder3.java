package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.LexLocation;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.dto.StepAlgorithm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.fmi.fmi3.Fmi3Variable;
import org.intocps.maestro.framework.core.FrameworkUnitInfo;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.*;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IfMaBlScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.values.BooleanExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.DoubleExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.framework.fmi2.api.mabl.PortFmi3Api.PortFilters.*;
import static org.intocps.maestro.framework.fmi2.api.mabl.variables.InstanceVariableFmi3Api.hasEventMode;
import static org.intocps.maestro.plugin.JacobianStepBuilder.ARG_INDEX.*;

@SuppressWarnings("deprecation")
@SimulationFramework(framework = Framework.FMI2)
public class JacobianStepBuilder3 extends JacobianStepBuilder {


    final static Logger logger = LoggerFactory.getLogger(JacobianStepBuilder3.class);

    final IndexedFunctionDeclarationContainer<ARG_INDEX> fixedStep3Func = IndexedFunctionDeclarationContainer.newBuilder("fixedStep3Size", ARG_INDEX.class)
            .addArg(FMI2_INSTANCES, "component", newAArrayType(newANameType("FMI2Component")))
            .addArg(FMI3_INSTANCES, "component", newAArrayType(newANameType("FMI3Instance"))).addArg(STEP_SIZE, "stepSize", newARealNumericPrimitiveType())
            .addArg(START_TIME, "startTime", newARealNumericPrimitiveType()).addArg(END_TIME, "endTime", newARealNumericPrimitiveType())
            .addArg(END_TIME_DEFINED, "endTimeDefined", newBoleanType()).build();


    public JacobianStepBuilder3() {
        imports.add("FMI3");
    }

    private static DoubleVariableFmi2Api calculateNextStepSize(MablApiBuilder builder, JacobianInternalBuilder.Jacobian3Context ctxt,
                                                               Map<InstanceVariableFmi3Api, Stream<PortFmi3Api>> timeBasedInputClockPorts) {
        if (timeBasedInputClockPorts.isEmpty()) {

            return ctxt.stepSize;
        }


        // preconfigure the step size to the shift only if we have clocks
        int clockIndex = 1;
        ctxt.stepSizes.items().getFirst().setValue(ctxt.stepSize);
        builder.getLogger().trace("## StepSizes[0]: %s", ctxt.stepSizes.items().get(0));
        for (var instance : ctxt.fmu3Instances.entrySet()) {
            var clockTimePorts = instance.getValue().getPorts().stream().filter(isClockTimeBased).toList();
            for (var clockPort : clockTimePorts) {
                var stepSizeVar = ctxt.stepSizes.items().get(clockIndex++);

                //ok so we need step size from now and we need to consider if it ever ticked
                InstanceClocksFmi3 clocks = instance.getValue().getClocksUtil();

                stepSizeVar.setValue(clocks.getTimeToTick(ctxt.currentCommunicationTime.toMath(), clockPort));
                builder.getLogger().trace("## StepSizes[" + (clockIndex - 1) + "]: %s", stepSizeVar);
            }
        }
        builder.getLogger().trace("## current time: %s", ctxt.currentCommunicationTime);
        return builder.getMathBuilder().minRealFromArray(ctxt.stepSizes);


    }

    @NotNull
    private static Map<String, BooleanVariableFmi2Api> updateEventsInEventMode(MablApiBuilder builder,
                                                                               List<InstanceVariableFmi3Api> eventCapableInstances,
                                                                               Map<String, InstanceVariableFmi3Api> fmuInstances3,
                                                                               DataWriter.DataWriterInstance dataWriterInstance,
                                                                               JacobianInternalBuilder.Jacobian3Context ctxt, boolean firstCall) {
        if (!firstCall) {
            //all FMUs here have been created with eventModeUser and thus must be in event mode at initialization
            eventCapableInstances.forEach(InstanceVariableFmi3Api::enterEventMode);
        }
        ctxt.eventMode.setValue(BooleanExpressionValue.of(true));
        var eventUpdatingFlags = fmuInstances3.entrySet().stream().filter(map -> hasEventMode.test(map.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, map -> builder.getDynamicScope().store(map.getKey() + "ProcessEvents", true)));
        builder.getLogger().trace("EVENT - Get and Share clocks");
//            for (var instance : eventCapableInstances) {
//                var clockPorts = instance.getPorts().stream().filter(isClock).toList();
//                // initial get all output clocks
//                clockPorts.stream().filter(isCausalityOutput.and(isClockTriggered)).forEach(instance::getAndShare);
//            }


        //TODO we also need all the clocked variables that are not continuous

                /* now we need to handle the events. That is:
                     - set clocks
                     - set clock dependent none continuous scalar variables
                     - FMI3UpdateDiscreteStates until no updates are needed
                 */


        processEvents(builder, eventCapableInstances, eventUpdatingFlags, dataWriterInstance, fmuInstances3, ctxt);


        //we need to make sure every instance is ready for step mode
        eventCapableInstances.forEach(InstanceVariableFmi3Api::enterStepMode);
        ctxt.eventMode.setValue(BooleanExpressionValue.of(false));
        return eventUpdatingFlags;
    }

    private static void processEvents(MablApiBuilder builder, List<InstanceVariableFmi3Api> eventCapableInstances,
                                      Map<String, BooleanVariableFmi2Api> eventUpdatingFlags, DataWriter.DataWriterInstance dataWriterInstance,
                                      Map<String, InstanceVariableFmi3Api> fmuInstances3, JacobianInternalBuilder.Jacobian3Context ctxt) {
        var eventHandlingScope = builder.getDynamicScope().enterScope();

        updateTriggeredClocks(builder, eventCapableInstances);
        updateTimeBasedClocks(builder, eventCapableInstances, ctxt);

        updateEventStates(builder, eventUpdatingFlags, fmuInstances3, dataWriterInstance, ctxt);


        // Event update the initial event states not sure if we should set clocks before this
        // set shared clocks
        // set time based clocks that are not shifted yes
        //FMI3UpdateDiscreteStates
        deactivateAllClocks(builder, eventCapableInstances, ctxt);

        eventHandlingScope.leave();
    }

    private static void deactivateAllClocks(MablApiBuilder builder, List<InstanceVariableFmi3Api> eventCapableInstances,
                                            JacobianInternalBuilder.Jacobian3Context ctxt) {
        builder.getLogger().trace("EVENT - Deactivated all clocks");
        eventCapableInstances.forEach(
                instance -> instance.getPorts().stream().filter(isClock).filter(isCausalityOutput.and(isClockTriggered))
                        .forEach(port -> port.getSharedAsVariable().setValue(new BooleanExpressionValue(false))));

        eventCapableInstances.forEach(
                instance -> instance.getPorts().stream().filter(isClock).filter(isClockTimeBased).forEach(port ->
                        instance.getClocksUtil().deactivate(port)));
        //builder.getDynamicScope().add(new ADebugStm(Collections.singletonList(new AStringLiteralExp(null, "context"))));
    }

    private static void updateTriggeredClocks(MablApiBuilder builder, List<InstanceVariableFmi3Api> eventCapableInstances) {
        //get all triggered output clocks and make them available
        builder.getLogger().trace("Processing clock (triggered clocks)");
        //noinspection unchecked
        eventCapableInstances.forEach(
                instance -> {
                    var variables = instance.getPorts().stream().filter(isClock).filter(isCausalityOutput.and(isClockTriggered)).map(instance::getAndShare)
                            .toList();
                    // for each potentially triggered output clock we need to get, share and set linked
                    for (var variable : variables) {
                        for (var clockValueRef : variable.entrySet()) {
                            if (clockValueRef.getKey() instanceof PortFmi3Api clockPort && clockValueRef.getValue() instanceof VariableFmi2Api clockVar) {

                                var ifTriggeredScope = builder.getDynamicScope().enterIf(new PredicateFmi2Api(clockVar.getReferenceExp()));
                                synchronizeLinkedClockDependentVariables(instance, clockPort);
                                ifTriggeredScope.leave();
                            }
                        }
                    }
                });
        //set all triggered input clocks
        eventCapableInstances.forEach(
                instance -> instance.getPorts().stream().filter(isClock).filter(isCausalityInput.and(isClockTriggered)).forEach(instance::setLinked));
        //if a clock is triggered then we need to obtain all variables related to it and share them
        //TODO if a clock is triggered then we need to obtain all variables related to it and share them

    }

    private static void stepAllFmi2(MablApiBuilder builder, Map<ComponentVariableFmi2Api, VariableFmi2Api<Double>> fmuInstanceToCommunicationPoint,
                                    JacobianInternalBuilder.Jacobian3Context ctxt, ModelSwapBuilder.ModelSwapContext modelSwapContext,
                                    Fmi2SimulationEnvironment env, DynamicActiveBuilderScope dynamicScope, BooleanVariableFmi2Api anyDiscards) {
        fmuInstanceToCommunicationPoint.forEach((instance, communicationPoint) -> {

            DoubleVariableFmi2Api communicationTime = ctxt.currentCommunicationTime;

            Map.Entry<DoubleVariableFmi2Api, Optional<PredicateFmi2Api>> swapStep = ModelSwapBuilder.updateStep(modelSwapContext, env, instance,
                    communicationTime);

            Optional<PredicateFmi2Api> stepPredicate = swapStep.getValue();
            communicationTime = swapStep.getKey();

            stepPredicate.ifPresent(dynamicScope::enterIf);

            Map.Entry<FmiBuilder.BoolVariable<PStm>, FmiBuilder.DoubleVariable<PStm>> discard = instance.step(communicationTime,
                    ctxt.currentStepSize);

            communicationPoint.setValue(new DoubleExpressionValue(discard.getValue().getExp()));

            PredicateFmi2Api didDiscard = new PredicateFmi2Api(discard.getKey().getExp()).not();

            dynamicScope.enterIf(didDiscard);
            {
                builder.getLogger()
                        .trace("## FMU: '%s' DISCARDED step at sim-time: %f for step-size: %f and proposed sim-time: %.15f", instance.getName(),
                                communicationTime, ctxt.currentStepSize,
                                new VariableFmi2Api<>(null, discard.getValue().getType(), dynamicScope, dynamicScope, null,
                                        discard.getValue().getExp()));
                anyDiscards.setValue(new BooleanVariableFmi2Api(null, null, dynamicScope, null, anyDiscards.toPredicate().or(didDiscard).getExp()));
                dynamicScope.leave();
            }

            if (stepPredicate.isPresent()) {
                dynamicScope.leave();
            }
        });
    }

    private static void stepAllFmi3(MablApiBuilder builder, Map<InstanceVariableFmi3Api, VariableFmi2Api<Double>> fmuInstance3ToCommunicationPoint,
                                    JacobianInternalBuilder.Jacobian3Context ctxt, Map<String, InstanceVariableFmi3Api> fmuInstances3,
                                    Map<String, BooleanVariableFmi2Api> eventUpdatingFlags, DynamicActiveBuilderScope dynamicScope,
                                    BooleanVariableFmi2Api anyDiscards) {
        fmuInstance3ToCommunicationPoint.forEach((instance, communicationPoint) -> {

            DoubleVariableFmi2Api communicationTime = ctxt.currentCommunicationTime;

//                    Map.Entry<DoubleVariableFmi2Api, Optional<PredicateFmi2Api>> swapStep = ModelSwapBuilder.updateStep(modelSwapContext, env, instance,
//                            communicationTime);

//                    Optional<PredicateFmi2Api> stepPredicate = swapStep.getValue();
//                    communicationTime = swapStep.getKey();

//                    stepPredicate.ifPresent(dynamicScope::enterIf);

            var requireEventProcessing = fmuInstances3.entrySet().stream().filter(map -> map.getValue().equals(instance)).map(Map.Entry::getKey).map(
                    eventUpdatingFlags::get).findFirst().orElse(null);
            ABoolLiteralExp noSetFMUStatePriorToCurrentPoint = new ABoolLiteralExp(new LexLocation("", 0, 0), false);
            var stepResult = instance.step(builder.getDynamicScope(),
                    communicationTime, ctxt.currentStepSize,
                    noSetFMUStatePriorToCurrentPoint, new InstanceVariableFmi3Api.StepResult(requireEventProcessing, null, null, null));

            var stepReturnData = stepResult.getValue();

            communicationPoint.setValue(new DoubleExpressionValue(stepReturnData.getLastSuccessfulTime().getExp()));

            PredicateFmi2Api didDiscard = new PredicateFmi2Api(stepResult.getKey().getExp()).not();

            dynamicScope.enterIf(didDiscard);
            {
                builder.getLogger()
                        .trace("## FMU: '%s' DISCARDED step at sim-time: %f for step-size: %f and proposed sim-time: %.15f", instance.getName(),
                                communicationTime, ctxt.currentStepSize,
                                new VariableFmi2Api<>(null, stepReturnData.getLastSuccessfulTime().getType(), dynamicScope, dynamicScope, null,
                                        stepReturnData.getLastSuccessfulTime().getExp()));
                anyDiscards.setValue(new BooleanVariableFmi2Api(null, null, dynamicScope, null, anyDiscards.toPredicate().or(didDiscard).getExp()));
                dynamicScope.leave();
            }

//                    if (stepPredicate.isPresent()) {
//                        dynamicScope.leave();
//                    }
        });
    }

    private static void getStepOutputs(Map<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Object>>> componentsToPortsWithValues,
                                       Map<InstanceVariableFmi3Api, Map<PortFmi3Api, VariableFmi2Api<Object>>> instancesToPortsWithValues) {
        for (var entry : componentsToPortsWithValues.entrySet()) {
            var portsToValues = entry.getValue();
            portsToValues = entry.getKey().get(portsToValues.keySet().toArray(PortFmi2Api[]::new));
        }
        for (var entry : instancesToPortsWithValues.entrySet()) {
            var portsToValues = entry.getValue();

            InstanceVariableFmi3Api instance = entry.getKey();

            for (PortFmi3Api p : portsToValues.keySet().stream().filter(isClockedVariable.negate()).toArray(PortFmi3Api[]::new)) {
                var val = instance.get(p);
                instance.share(val);
            }

//                    portsToValues = entry.getKey().get(portsToValues.keySet().toArray(PortFmi3Api[]::new));
        }
    }

    private static void updateEventStates(MablApiBuilder builder, Map<String, BooleanVariableFmi2Api> eventUpdatingFlags,
                                          Map<String, InstanceVariableFmi3Api> fmuInstances3, DataWriter.DataWriterInstance dataWriterInstance,
                                          JacobianInternalBuilder.Jacobian3Context ctxt) {
        var dontCareBool = builder.getDynamicScope().store("dont_care_bool", false);
        var dontCareReal = builder.getDynamicScope().store("dont_care_real", 0.0);

        // keep looping while any of needs an update
        var updateWhile = builder.getDynamicScope()
                .enterWhile(eventUpdatingFlags.values().stream().map(BooleanVariableFmi2Api::toPredicate).reduce(PredicateFmi2Api::or).orElse(null));
        dataWriterInstance.log(ctxt.currentCommunicationTime);

        for (var entry : eventUpdatingFlags.entrySet()) {
            var k = entry.getKey();
            var processEvents = entry.getValue();
            var instance = fmuInstances3.get(k);
            var processEventsScope = builder.getDynamicScope().enterIf(processEvents.toPredicate());

            instance.updateDiscreteStates(builder.getDynamicScope(), processEvents, ctxt.terminateSimulation, dontCareBool, dontCareBool, dontCareBool,
                    dontCareReal);

            var ifNeedsProcessing = builder.getDynamicScope().enterIf(processEvents.toPredicate());
            builder.getLogger().trace("## FMU: " + instance.getName() + " EVENTS UPDATED");
            ifNeedsProcessing.leave();
            //TODO check for terminate simulation

            processEventsScope.leave();
        }
        updateWhile.leave();
    }

    private static void synchronizeLinkedClockDependentVariables(InstanceVariableFmi3Api instance, PortFmi3Api clockPort) {
        try {
            var dependedClockVariables = instance.getModelDescription().getModelVariables().stream()
                    .filter(sv -> sv.getClocksAsLong() != null && sv.getClocksAsLong().contains(clockPort.getPortReferenceValue())).map(
                            Fmi3Variable::getName).toList().toArray(String[]::new);
            instance.getAndShare(dependedClockVariables);
            instance.setLinked(dependedClockVariables);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    private static void updateTimeBasedClocks(MablApiBuilder builder, List<InstanceVariableFmi3Api> eventCapableInstances,
                                              JacobianInternalBuilder.Jacobian3Context ctxt) {
        for (var instance : eventCapableInstances) {
            var clockPorts = instance.getPorts().stream().filter(isClock).toList();


            //we do this in two stages triggered and times
//                instance.setLinked(clockPorts.stream().filter(isCausalityInput.and(isClockTriggered)).map(PortFmi3Api::getName).toArray(String[]::new));


            var timeBasedClockPorts = clockPorts.stream().filter(isCausalityInput.and(isClockTimeBased)).toList();
            if (timeBasedClockPorts.isEmpty()) {
                continue;
            }
            builder.getLogger().trace("Processing clock (time-based clocks) for " + instance.getName());
            timeBasedClockPorts.forEach(clockPort -> {

                var ifScope = builder.getDynamicScope()
                        .enterIf(instance.getClocksUtil().check(builder.getDynamicScope(), ctxt.currentCommunicationTime.toMath(), clockPort));
                builder.getLogger().trace("\tTimed clock of %s.%s TRIGGERED".formatted(instance.getName(), clockPort.getName()));
                instance.set(clockPort, BooleanExpressionValue.of(true));
                synchronizeLinkedClockDependentVariables(instance, clockPort);
                //builder.getDynamicScope().add(new ADebugStm(Collections.singletonList(new AStringLiteralExp(null, "context"))));
                ifScope.leave();

            });

        }
    }

    private static boolean isEveryFMUSupportsGetState
            (Map<String, ComponentVariableFmi2Api> fmuInstances, Map<String, InstanceVariableFmi3Api> fmuInstances3) {
        return fmuInstances.values().stream().allMatch(inst -> {
            try {
                return inst.getModelDescription().getCanGetAndSetFmustate();
            } catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
        }) && fmuInstances3.values().stream().allMatch(inst -> {
            try {
                return inst.getModelDescription().getCanGetAndSetFmustate();
            } catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected List<IndexedFunctionDeclarationContainer<ARG_INDEX>> getFunctions() {
        return Collections.singletonList(fixedStep3Func);
    }

    private String blockMessage(String msg) {
        String seperator = "#############################################";
        return seperator + "## " + msg + " " + seperator;
    }

    @Override
    public <R> RuntimeConfigAddition<R> expandWithRuntimeAddition(AFunctionDeclaration declaredFunction,
                                                                  FmiBuilder<PStm, ASimulationSpecificationCompilationUnit, PExp, ?> parentBuilder,
                                                                  List<FmiBuilder.Variable<PStm, ?>> formalArguments, IPluginConfiguration config,
                                                                  ISimulationEnvironment envIn, IErrorReporter errorReporter) throws ExpandException {


        logger.trace("Unfolding with jacobian step: {}", declaredFunction.toString());
        JacobianStepConfig jacobianStepConfig = config != null ? (JacobianStepConfig) config : new JacobianStepConfig();

        if (!getDeclaredUnfoldFunctions().contains(declaredFunction)) {
            throw new ExpandException("Unknown function declaration");
        }

        if (envIn == null) {
            throw new ExpandException("Simulation environment must not be null");
        }

        IndexedFunctionDeclarationContainer<ARG_INDEX> selectedFun = getFunctions().stream()
                .filter(f -> f.getDecl().getName().getText().equals(declaredFunction.getName().getText())).findFirst().orElse(null);

        StepAlgorithm algorithm = StepAlgorithm.FIXEDSTEP;


        if (selectedFun == variableStepFunc) {
            algorithm = StepAlgorithm.VARIABLESTEP;
            imports.add("VariableStep");
        } else if (selectedFun == fixedStepTransferFunc) {
            algorithm = StepAlgorithm.FIXEDSTEP;
            logger.trace("Activated mode transfer");
        }

        if (formalArguments == null || formalArguments.size() != selectedFun.getDecl().getFormals().size()) {
            throw new ExpandException("Invalid args");
        }


        Fmi2SimulationEnvironment env = (Fmi2SimulationEnvironment) envIn;

        boolean setGetDerivativesRestore = false;
        MablApiBuilder.MablSettings settings = null;
        try {
            if (parentBuilder.getSettings() instanceof MablApiBuilder.MablSettings) {
                //FIXME we should probably not do this in a plugin as it changes this for all once the builder is reused!
                settings = (MablApiBuilder.MablSettings) parentBuilder.getSettings();
                setGetDerivativesRestore = settings.setGetDerivatives;
                settings.setGetDerivatives = jacobianStepConfig.setGetDerivatives;
            }

            if (!(parentBuilder instanceof MablApiBuilder builder)) {
                throw new ExpandException(
                        "Not supporting the given builder type. Expecting " + MablApiBuilder.class.getSimpleName() + " got " + parentBuilder.getClass()
                                .getSimpleName());
            }

            DynamicActiveBuilderScope dynamicScope = builder.getDynamicScope();
            MathBuilderFmi2Api math = builder.getMablToMablAPI().getMathBuilder();
            BooleanBuilderFmi2Api booleanLogic = builder.getBooleanBuilder();

            RealTimeSlowDownBuilder.RealTimeSlowDownContext ctsCtxt = null;
            if (jacobianStepConfig.simulationProgramDelay) {
                ctsCtxt = RealTimeSlowDownBuilder.init(builder, imports);
            }

            // Convert raw MaBL to API
            var ctxt = JacobianInternalBuilder.buildBaseFmi3Ctxt(builder, selectedFun, formalArguments, dynamicScope);

            var fmuInstances = ctxt.fmuInstances;
            var fmuInstances3 = ctxt.fmu3Instances;

            var timeBasedInputClockPorts = fmuInstances3.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue,
                    m -> m.getValue().getPorts().stream().filter(isClock).filter(isCausalityInput.and(isClockTimeBased))));


            // Create the logging
            DataWriter dataWriter = builder.getDataWriter();
            DataWriter.DataWriterInstance dataWriterInstance = dataWriter.createDataWriterInstance();
            List<DataWriter.DataWriterInstance.LogEntry> logVariables = Stream.concat(fmuInstances.values().stream().flatMap(x -> x.getVariablesToLog().stream()
                    .map(xsv -> new DataWriter.DataWriterInstance.LogEntry(xsv.getMultiModelScalarVariableName(),
                            () -> xsv.getSharedAsVariable().getReferenceExp().clone()))), fmuInstances3.values().stream().flatMap(
                    x -> x.getVariablesToLog().stream().map(xsv -> new DataWriter.DataWriterInstance.LogEntry(xsv.getMultiModelScalarVariableName(),
                            () -> xsv.getSharedAsVariable().getReferenceExp().clone())))

            ).collect(Collectors.toList());
            var timeBasedClocksLogVariables = timeBasedInputClockPorts.entrySet().stream().flatMap(m -> m.getValue()
                    .map(clockPort -> new DataWriter.DataWriterInstance.LogEntry(clockPort.getMultiModelScalarVariableName(),
                            () -> m.getKey().getClocksUtil().getClockTriggeredState(clockPort).getReferenceExp().clone()))).toList();
            logVariables.addAll(timeBasedClocksLogVariables);
            logVariables.addFirst(new DataWriter.DataWriterInstance.LogEntry("eventMode", () -> ctxt.eventMode.getReferenceExp().clone()));
            dataWriterInstance.initialize(logVariables);

            // Create simulation control to allow for user interactive loop stopping
            SimulationControl simulationControl = builder.getSimulationControl();

            // Create the iteration predicate
            PredicateFmi2Api loopPredicate = ctxt.externalEndTimeDefined.toPredicate().not()
                    .or(ctxt.currentCommunicationTime.toMath().addition(ctxt.currentStepSize).lessThan(ctxt.endTime));


            // Get all variables related to outputs or logging.
            Map<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Object>>> componentsToPortsWithValues = JacobianVariableStepBuilder.getAllComponentPortsWithOutputOrLog(
                    fmuInstances, jacobianStepConfig, env);

            Map<InstanceVariableFmi3Api, Map<PortFmi3Api, VariableFmi2Api<Object>>> instancesToPortsWithValues = JacobianVariableStepBuilder.getAllInstancePortsWithOutputOrLog(
                    fmuInstances3, jacobianStepConfig, env);

            builder.getLogger().trace(blockMessage("Jaccobian start"));

            // Share
            componentsToPortsWithValues.forEach(ComponentVariableFmi2Api::share);
            instancesToPortsWithValues.forEach(InstanceVariableFmi3Api::share);

            // Build static FMU relations
            Map<StringVariableFmi2Api, ComponentVariableFmi2Api> fmuNamesToFmuInstances = new LinkedHashMap<>();

            ArrayVariableFmi2Api<Double> fmuCommunicationPoints = dynamicScope.store("fmu_communicationpoints",
                    new Double[fmuInstances.size() + fmuInstances3.size()]);

            for (ComponentVariableFmi2Api instance : fmuInstances.values()) {

                FrameworkUnitInfo v = env.getInstanceByLexName(instance.getEnvironmentName());
                if (v instanceof ComponentInfo) {
                    StringVariableFmi2Api fullyQualifiedFMUInstanceName = new StringVariableFmi2Api(null, null, null, null,
                            MableAstFactory.newAStringLiteralExp(((ComponentInfo) v).getFmuIdentifier() + "." + instance.getName()));
                    fmuNamesToFmuInstances.put(fullyQualifiedFMUInstanceName, instance);
                } else {
                    throw new RuntimeException("instance is not fmi2");
                }


            }

            AtomicInteger indexer = new AtomicInteger();
            var fmuInstanceToCommunicationPoint = fmuInstances.values().stream()
                    .collect(Collectors.toMap(inst -> inst, instance -> fmuCommunicationPoints.items().get(indexer.getAndIncrement())));

            var fmuInstance3ToCommunicationPoint = fmuInstances3.values().stream()
                    .collect(Collectors.toMap(inst -> inst, instance -> fmuCommunicationPoints.items().get(indexer.getAndIncrement())));


            // validate if all fmus can get state
            boolean everyFMUSupportsGetState = isEveryFMUSupportsGetState(fmuInstances, fmuInstances3);


            if (!everyFMUSupportsGetState && jacobianStepConfig.stabilisation) {
                throw new RuntimeException("Cannot use stabilisation as not every FMU supports rollback");
            }

            BooleanVariableFmi2Api allFMUsSupportGetState = dynamicScope.store("all_fmus_support_get_state", everyFMUSupportsGetState);

            JacobianVariableStepBuilder.JacobianVariableStepContext varStep = null;
            if (algorithm == StepAlgorithm.VARIABLESTEP) {
                varStep = JacobianVariableStepBuilder.init(ctxt, jacobianStepConfig, dynamicScope, builder, fmuNamesToFmuInstances);
            }

            // TODO: we need to handle the initial event loop for clocks

            // Event Step 0: create time based clocks. We need to obtain the specific timing variables from
//            fmuInstances3.values().stream().findFirst().get().


            // Event Init step 1: get clocks from others
            builder.getLogger().trace(blockMessage("Jaccobian Event handling before loop"));
            builder.getLogger().trace("EVENT - Before step event handling");
            var eventCapableInstances = fmuInstances3.values().stream().filter(hasEventMode).toList();
            var eventUpdatingFlags = updateEventsInEventMode(builder, eventCapableInstances, fmuInstances3, dataWriterInstance, ctxt, true);

            //TODO: OK now clocked variables must be filtered as they are only available in event mode

            // Log values at t = start time
            dataWriterInstance.log(ctxt.currentCommunicationTime);


            StabilisationBuilder.StabilisationContext stabilisationCtxt = null;
            if (jacobianStepConfig.stabilisation) {
                stabilisationCtxt = StabilisationBuilder.init(dynamicScope, jacobianStepConfig);
            }


            if (jacobianStepConfig.simulationProgramDelay) {
                RealTimeSlowDownBuilder.setStartTime(ctsCtxt, dynamicScope);

            }

            List<FmiBuilder.StateVariable<PStm>> fmuStates = new ArrayList<>();
            BooleanVariableFmi2Api anyDiscards = dynamicScope.store("any_discards", false);

            // Initialise swap and step condition variables
            ModelSwapBuilder.ModelSwapContext modelSwapContext = ModelSwapBuilder.buildContext(env, dynamicScope);

            builder.getLogger().trace(blockMessage("Jaccobian main loop"));
            ScopeFmi2Api scopeFmi2Api = dynamicScope.enterWhile(loopPredicate);
            {
                ScopeFmi2Api stoppingThenScope = scopeFmi2Api.enterIf(simulationControl.stopRequested().toPredicate()).enterThen();
                stoppingThenScope.add(new AErrorStm(newAStringLiteralExp("Simulation stopped by user")));
                stoppingThenScope.leave();

                //mark a safe point for a transfer to another specification
                dynamicScope.markTransferPoint();


                // Update all swap and step condition variables
                ModelSwapBuilder.updateSwapConditionVariables(modelSwapContext, dynamicScope, componentsToPortsWithValues);

                // Get fmu states
                if (everyFMUSupportsGetState) {
                    for (ComponentVariableFmi2Api instance : fmuInstances.values()) {
                        fmuStates.add(instance.getState());
                    }
                    for (InstanceVariableFmi3Api instance : fmuInstances3.values()) {
                        fmuStates.add(instance.getState());
                    }
                }

                if (jacobianStepConfig.stabilisation) {
                    StabilisationBuilder.step(stabilisationCtxt, dynamicScope);

                }

                // SET ALL LINKED VARIABLES
                // This has to be carried out regardless of stabilisation or not.
                ModelSwapBuilder.setWithModelSwapLinking(fmuInstances, env, dynamicScope, modelSwapContext);
                fmuInstances3.values().forEach(instance -> instance.setLinked(
                        instance.getPorts().stream().filter(InstanceVariableFmi3Api.isLinked.and(isClockedVariable.negate()).and(isClock.negate()))
                                .toArray(PortFmi3Api[]::new)));

                if (algorithm == StepAlgorithm.VARIABLESTEP) {
                    // Get variable step
                    JacobianVariableStepBuilder.updateCurrentStepTiming(ctxt, varStep, dynamicScope, anyDiscards);

                }

                anyDiscards.setValue(new BooleanVariableFmi2Api(null, null, dynamicScope, null, MableAstFactory.newABoolLiteralExp(false)));

                // STEP ALL FMI2
                stepAllFmi2(builder, fmuInstanceToCommunicationPoint, ctxt, modelSwapContext, env, dynamicScope, anyDiscards);

                // STEP ALL FMI3
                /**
                 * FMI 3 step mode stepping
                 * */
                stepAllFmi3(builder, fmuInstance3ToCommunicationPoint, ctxt, fmuInstances3, eventUpdatingFlags, dynamicScope, anyDiscards);

                // GET ALL LINKED OUTPUTS INCLUDING LOGGING OUTPUTS
                getStepOutputs(componentsToPortsWithValues, instancesToPortsWithValues);

                // CONVERGENCE
                if (jacobianStepConfig.stabilisation) {
                    // CONVERGENCE
                    StabilisationBuilder.convergence(dynamicScope, componentsToPortsWithValues, stabilisationCtxt, ctxt, builder, math, booleanLogic,
                            fmuStates);

                } else {
                    // NORMAL SHARE - VALUE EXCHANGE
                    componentsToPortsWithValues.forEach(ComponentVariableFmi2Api::share);
//                    instancesToPortsWithValues.forEach(InstanceVariableFmi3Api::share);
                }

                if (everyFMUSupportsGetState) {
                    // Discard
                    IfMaBlScope discardScope = dynamicScope.enterIf(anyDiscards.toPredicate());
                    {
                        // Rollback FMUs
                        fmuStates.forEach(FmiBuilder.StateVariable::set);

                        // Set step-size to lowest
                        ctxt.currentStepSize.setValue(math.minRealFromArray(fmuCommunicationPoints).toMath().subtraction(ctxt.currentCommunicationTime));

                        builder.getLogger().trace("## Discard occurred! FMUs are rolled back and step-size reduced to: %f", ctxt.currentStepSize);

                        dynamicScope.leave();
                    }

                    discardScope.enterElse();
                }
                {
                    if (algorithm == StepAlgorithm.VARIABLESTEP) {
                        // Validate step
                        JacobianVariableStepBuilder.step(ctxt, varStep, dynamicScope, builder, allFMUsSupportGetState, fmuStates, anyDiscards);
                    }

                    // Slow-down to real-time
                    if (jacobianStepConfig.simulationProgramDelay) {
                        RealTimeSlowDownBuilder.slowDown(ctsCtxt, dynamicScope, ctxt, builder);
                    }

                    if (everyFMUSupportsGetState) {
                        dynamicScope.leave();
                    }
                }

                dynamicScope.enterIf(anyDiscards.toPredicate().not());
                {
                    // Update currentCommunicationTime
                    ctxt.currentCommunicationTime.setValue(ctxt.currentCommunicationTime.toMath().addition(ctxt.currentStepSize));

                    ModelSwapBuilder.updateDiscardStepTime(modelSwapContext, dynamicScope, ctxt.currentStepSize);

                    // Log values at current communication point
                    dataWriterInstance.log(ctxt.currentCommunicationTime);
                    eventUpdatingFlags = updateEventsInEventMode(builder, eventCapableInstances, fmuInstances3, dataWriterInstance, ctxt, false);


                    ctxt.currentStepSize.setValue(calculateNextStepSize(builder, ctxt, timeBasedInputClockPorts));
                    var checkStepSizeScope = builder.getDynamicScope().enterIf(ctxt.currentStepSize.toMath().lessEqualTo(IntExpressionValue.of(0)));
                    var checkStepSizeScopeThen = checkStepSizeScope.enterThen();
                    builder.getLogger().trace("Step size must be positive: %f", ctxt.currentStepSize);
//                    checkStepSizeScopeThen.add(new AErrorStm(newAStringLiteralExp("Step size must be positive")));
                    checkStepSizeScope.leave();
                    builder.getLogger().trace("## Step size: %f", ctxt.currentStepSize);
                }

                scopeFmi2Api.leave();
            }

            dataWriterInstance.close();

            if (settings != null) {
                //restore previous state
                settings.setGetDerivatives = setGetDerivativesRestore;
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorReporter.report(0, e.toString(), null);
            throw new ExpandException("Internal error: ", e);
        }

        return new EmptyRuntimeConfig<>();
    }


}



