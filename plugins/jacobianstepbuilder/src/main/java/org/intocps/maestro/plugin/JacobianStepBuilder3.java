package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.dto.StepAlgorithm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.FrameworkUnitInfo;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.*;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IfMaBlScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.values.DoubleExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.plugin.JacobianStepBuilder.ARG_INDEX.*;

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


    @Override
    protected List<IndexedFunctionDeclarationContainer<ARG_INDEX>> getFunctions() {
        return Collections.singletonList(fixedStep3Func);
    }


    @Override
    public <R> RuntimeConfigAddition<R> expandWithRuntimeAddition(AFunctionDeclaration declaredFunction,
                                                                  FmiBuilder<PStm, ASimulationSpecificationCompilationUnit, PExp, ?> parentBuilder,
                                                                  List<FmiBuilder.Variable<PStm, ?>> formalArguments, IPluginConfiguration config,
                                                                  ISimulationEnvironment envIn, IErrorReporter errorReporter) throws ExpandException {


        logger.info("Unfolding with jacobian step: {}", declaredFunction.toString());
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
            logger.debug("Activated mode transfer");
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

            if (!(parentBuilder instanceof MablApiBuilder)) {
                throw new ExpandException(
                        "Not supporting the given builder type. Expecting " + MablApiBuilder.class.getSimpleName() + " got " + parentBuilder.getClass()
                                .getSimpleName());
            }

            MablApiBuilder builder = (MablApiBuilder) parentBuilder;

            DynamicActiveBuilderScope dynamicScope = builder.getDynamicScope();
            MathBuilderFmi2Api math = builder.getMablToMablAPI().getMathBuilder();
            BooleanBuilderFmi2Api booleanLogic = builder.getBooleanBuilder();

            RealTimeSlowDownBuilder.RealTimeSlowDownContext ctsCtxt = null;
            if (jacobianStepConfig.simulationProgramDelay) {
                ctsCtxt = RealTimeSlowDownBuilder.init(builder, imports);
            }

            // Convert raw MaBL to API
            JacobianInternalBuilder.BaseJacobianContext ctxt = JacobianInternalBuilder.buildBaseCtxt(selectedFun, formalArguments, dynamicScope);

            Map<String, ComponentVariableFmi2Api> fmuInstances = ctxt.fmuInstances;
            Map<String, InstanceVariableFmi3Api> fmuInstances3 = ((List<InstanceVariableFmi3Api>) ((FmiBuilder.ArrayVariable) selectedFun.getArgumentValue(
                    formalArguments, FMI3_INSTANCES)).items()).stream()
                    .collect(Collectors.toMap(InstanceVariableFmi3Api::getName, Function.identity(), (u, v) -> u, LinkedHashMap::new));


            // Create the logging
            DataWriter dataWriter = builder.getDataWriter();
            DataWriter.DataWriterInstance dataWriterInstance = dataWriter.createDataWriterInstance();
            dataWriterInstance.initialize(Stream.concat(fmuInstances.values().stream().flatMap(x -> x.getVariablesToLog().stream()
                    .map(xsv -> new DataWriter.DataWriterInstance.LogEntry(xsv.getMultiModelScalarVariableName(),
                            () -> xsv.getSharedAsVariable().getReferenceExp().clone()))), fmuInstances3.values().stream().flatMap(
                    x -> x.getVariablesToLog().stream().map(xsv -> new DataWriter.DataWriterInstance.LogEntry(xsv.getMultiModelScalarVariableName(),
                            () -> xsv.getSharedAsVariable().getReferenceExp().clone())))

            ).collect(Collectors.toList()));

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


            // Share
            componentsToPortsWithValues.forEach(ComponentVariableFmi2Api::share);
            instancesToPortsWithValues.forEach(InstanceVariableFmi3Api::share);

            // Build static FMU relations
            Map<StringVariableFmi2Api, ComponentVariableFmi2Api> fmuNamesToFmuInstances = new LinkedHashMap<>();

            ArrayVariableFmi2Api<Double> fmuCommunicationPoints = dynamicScope.store("fmu_communicationpoints",
                    new Double[fmuInstances.entrySet().size() + fmuInstances3.entrySet().size()]);

//            int indexer = 0;
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
            Map<ComponentVariableFmi2Api, VariableFmi2Api<Double>> fmuInstanceToCommunicationPoint = fmuInstances.values().stream()
                    .collect(Collectors.toMap(inst -> inst, instance -> fmuCommunicationPoints.items().get(indexer.getAndIncrement())));

            Map<InstanceVariableFmi3Api, VariableFmi2Api<Double>> fmuInstance3ToCommunicationPoint = fmuInstances3.values().stream()
                    .collect(Collectors.toMap(inst -> inst, instance -> fmuCommunicationPoints.items().get(indexer.getAndIncrement())));


            // validate if all fmus can get state
            boolean everyFMUSupportsGetState = fmuInstances.values().stream().allMatch(inst -> {
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


            if (!everyFMUSupportsGetState && jacobianStepConfig.stabilisation) {
                throw new RuntimeException("Cannot use stabilisation as not every FMU supports rollback");
            }

            BooleanVariableFmi2Api allFMUsSupportGetState = dynamicScope.store("all_fmus_support_get_state", everyFMUSupportsGetState);

            JacobianVariableStepBuilder.JacobianVariableStepContext varStep = null;
            if (algorithm == StepAlgorithm.VARIABLESTEP) {
                varStep = JacobianVariableStepBuilder.init(ctxt, jacobianStepConfig, dynamicScope, builder, fmuNamesToFmuInstances);
            }

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
                fmuInstances3.values().forEach(instance -> instance.setLinked());

                if (algorithm == StepAlgorithm.VARIABLESTEP) {
                    // Get variable step
                    JacobianVariableStepBuilder.updateCurrentStepTiming(ctxt, varStep, dynamicScope, anyDiscards);

                }

                anyDiscards.setValue(new BooleanVariableFmi2Api(null, null, dynamicScope, null, MableAstFactory.newABoolLiteralExp(false)));

                // STEP ALL FMI2
                fmuInstanceToCommunicationPoint.forEach((instance, communicationPoint) -> {

                    DoubleVariableFmi2Api communicationTime = ctxt.currentCommunicationTime;

                    Map.Entry<DoubleVariableFmi2Api, Optional<PredicateFmi2Api>> swapStep = ModelSwapBuilder.updateStep(modelSwapContext, env, instance,
                            communicationTime);

                    Optional<PredicateFmi2Api> stepPredicate = swapStep.getValue();
                    communicationTime = swapStep.getKey();

                    stepPredicate.ifPresent(dynamicScope::enterIf);

                    Map.Entry<FmiBuilder.BoolVariable<PStm>, FmiBuilder.DoubleVariable<PStm>> discard = instance.step(communicationTime, ctxt.currentStepSize);

                    communicationPoint.setValue(new DoubleExpressionValue(discard.getValue().getExp()));

                    PredicateFmi2Api didDiscard = new PredicateFmi2Api(discard.getKey().getExp()).not();

                    dynamicScope.enterIf(didDiscard);
                    {
                        builder.getLogger()
                                .debug("## FMU: '%s' DISCARDED step at sim-time: %f for step-size: %f and proposed sim-time: %.15f", instance.getName(),
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

                // STEP ALL FMI3
                fmuInstance3ToCommunicationPoint.forEach((instance, communicationPoint) -> {

                    DoubleVariableFmi2Api communicationTime = ctxt.currentCommunicationTime;

//                    Map.Entry<DoubleVariableFmi2Api, Optional<PredicateFmi2Api>> swapStep = ModelSwapBuilder.updateStep(modelSwapContext, env, instance,
//                            communicationTime);

//                    Optional<PredicateFmi2Api> stepPredicate = swapStep.getValue();
//                    communicationTime = swapStep.getKey();

//                    stepPredicate.ifPresent(dynamicScope::enterIf);

                    Map.Entry<FmiBuilder.BoolVariable<PStm>, InstanceVariableFmi3Api.StepResult> discard = instance.step(builder.getDynamicScope(),
                            communicationTime, ctxt.currentStepSize, new ABoolLiteralExp(false));

                    communicationPoint.setValue(new DoubleExpressionValue(discard.getValue().getLastSuccessfulTime().getExp()));

                    PredicateFmi2Api didDiscard = new PredicateFmi2Api(discard.getKey().getExp()).not();

                    dynamicScope.enterIf(didDiscard);
                    {
                        builder.getLogger()
                                .debug("## FMU: '%s' DISCARDED step at sim-time: %f for step-size: %f and proposed sim-time: %.15f", instance.getName(),
                                        communicationTime, ctxt.currentStepSize,
                                        new VariableFmi2Api<>(null, discard.getValue().getLastSuccessfulTime().getType(), dynamicScope, dynamicScope, null,
                                                discard.getValue().getLastSuccessfulTime().getExp()));
                        anyDiscards.setValue(new BooleanVariableFmi2Api(null, null, dynamicScope, null, anyDiscards.toPredicate().or(didDiscard).getExp()));
                        dynamicScope.leave();
                    }

//                    if (stepPredicate.isPresent()) {
//                        dynamicScope.leave();
//                    }
                });

                // GET ALL LINKED OUTPUTS INCLUDING LOGGING OUTPUTS
                for (Map.Entry<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Object>>> entry : componentsToPortsWithValues.entrySet()) {
                    Map<PortFmi2Api, VariableFmi2Api<Object>> portsToValues = entry.getValue();
                    portsToValues = entry.getKey().get(portsToValues.keySet().toArray(PortFmi2Api[]::new));
                }
                for (Map.Entry<InstanceVariableFmi3Api, Map<PortFmi3Api, VariableFmi2Api<Object>>> entry : instancesToPortsWithValues.entrySet()) {
                    Map<PortFmi3Api, VariableFmi2Api<Object>> portsToValues = entry.getValue();

                    InstanceVariableFmi3Api instance = entry.getKey();

                    for (PortFmi3Api p : portsToValues.keySet().toArray(PortFmi3Api[]::new)) {
                        Map<PortFmi3Api, VariableFmi2Api<Object>> val = instance.get(p);
                        instance.share(val);
                    }

//                    portsToValues = entry.getKey().get(portsToValues.keySet().toArray(PortFmi3Api[]::new));
                }

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

                        builder.getLogger().debug("## Discard occurred! FMUs are rolled back and step-size reduced to: %f", ctxt.currentStepSize);

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
                    ctxt.currentStepSize.setValue(ctxt.stepSize);
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



