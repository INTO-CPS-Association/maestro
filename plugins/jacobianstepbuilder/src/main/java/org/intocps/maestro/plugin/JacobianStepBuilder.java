package org.intocps.maestro.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.AModuleDeclaration;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.dto.StepAlgorithm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.FrameworkUnitInfo;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.core.RelationVariable;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.plugin.JacobianStepBuilder.ARG_INDEX.*;

@SimulationFramework(framework = Framework.FMI2)
public class JacobianStepBuilder extends BasicMaestroExpansionPlugin {

    protected List<IndexedFunctionDeclarationContainer<ARG_INDEX>> getFunctions() {
        return Arrays.asList(fixedStepFunc, fixedStepTransferFunc, variableStepFunc);
    }

    protected enum ARG_INDEX {
        FMI2_INSTANCES, FMI3_INSTANCES, START_TIME, STEP_SIZE, END_TIME, END_TIME_DEFINED
    }

    protected final static Logger logger = LoggerFactory.getLogger(JacobianStepBuilder.class);

    protected final IndexedFunctionDeclarationContainer<ARG_INDEX> fixedStepFunc = IndexedFunctionDeclarationContainer.newBuilder(
                    "fixedStepSize", ARG_INDEX.class).
            addArg(FMI2_INSTANCES, "component", newAArrayType(newANameType("FMI2Component"))).
            addArg(STEP_SIZE, "stepSize", newARealNumericPrimitiveType()).
            addArg(START_TIME, "startTime", newARealNumericPrimitiveType()).
            addArg(END_TIME, "endTime", newARealNumericPrimitiveType()).
            addArg(END_TIME_DEFINED, "endTimeDefined", newBoleanType()).build();

    protected final IndexedFunctionDeclarationContainer<ARG_INDEX> fixedStepTransferFunc = IndexedFunctionDeclarationContainer.newBuilder(
                    "fixedStepSizeTransfer", ARG_INDEX.class).
            addArg(FMI2_INSTANCES, "component", newAArrayType(newANameType("FMI2Component"))).
            addArg(STEP_SIZE, "stepSize", newARealNumericPrimitiveType()).
            addArg(START_TIME, "startTime", newARealNumericPrimitiveType()).
            addArg(END_TIME, "endTime", newARealNumericPrimitiveType()).
            addArg(END_TIME_DEFINED, "endTimeDefined", newBoleanType()).build();

    protected final IndexedFunctionDeclarationContainer<ARG_INDEX> variableStepFunc = IndexedFunctionDeclarationContainer.newBuilder(
                    "variableStepSize", ARG_INDEX.class).
            addArg(FMI2_INSTANCES, "component", newAArrayType(newANameType("FMI2Component"))).
            addArg(STEP_SIZE, "initSize", newARealNumericPrimitiveType()).
            addArg(START_TIME, "startTime", newARealNumericPrimitiveType()).
            addArg(END_TIME, "endTime", newARealNumericPrimitiveType()).
            addArg(END_TIME_DEFINED, "endTimeDefined", newBoleanType()).build();


    protected final List<String> imports =
            Stream.of("FMI2", "TypeConverter", "Math", "Logger", "DataWriter", "ArrayUtil", "BooleanLogic",
                            "SimulationControl")
                    .collect(Collectors.toList());
    static BiConsumer<PortFmi2Api, PortFmi2Api> relinkPorts = (source, target) -> {
        try {

            switch (source.scalarVariable.causality) {


                case Input:
                    source.breakLink();
                    target.linkTo(source);
                    break;
                case Output:
                    target.breakLink();
                    source.linkTo(target);
                    break;
                default:
                    break;
            }

        } catch (FmiBuilder.Port.PortLinkException e) {
            e.printStackTrace();
        }
    };

    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return getFunctions().stream().map(IndexedFunctionDeclarationContainer::getDecl).collect(Collectors.toSet());
    }

    @Override
    public <R> RuntimeConfigAddition<R> expandWithRuntimeAddition(AFunctionDeclaration declaredFunction,
                                                                  FmiBuilder<PStm, ASimulationSpecificationCompilationUnit, PExp, ?> parentBuilder,
                                                                  List<FmiBuilder.Variable<PStm, ?>> formalArguments,
                                                                  IPluginConfiguration config,
                                                                  ISimulationEnvironment envIn,
                                                                  IErrorReporter errorReporter) throws ExpandException {


        logger.info("Unfolding with jacobian step: {}", declaredFunction.toString());
        JacobianStepConfig jacobianStepConfig = config != null ? (JacobianStepConfig) config : new JacobianStepConfig();

        if (!getDeclaredUnfoldFunctions().contains(declaredFunction)) {
            throw new ExpandException("Unknown function declaration");
        }

        if (envIn == null) {
            throw new ExpandException("Simulation environment must not be null");
        }

        StepAlgorithm algorithm = StepAlgorithm.FIXEDSTEP;
        IndexedFunctionDeclarationContainer<ARG_INDEX> selectedFun = getFunctions().stream()
                .filter(f -> f.getDecl().getName().getText().equals(declaredFunction.getName().getText())).findFirst()
                .orElse(null);

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
                        "Not supporting the given builder type. Expecting " + MablApiBuilder.class.getSimpleName() + " got " +
                                parentBuilder.getClass().getSimpleName());
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
            JacobianInternalBuilder.BaseJacobianContext ctxt = JacobianInternalBuilder.buildBaseCtxt(selectedFun,
                    formalArguments, dynamicScope);

            Map<String, ComponentVariableFmi2Api> fmuInstances = ctxt.fmuInstances;


            // Create the logging
            DataWriter dataWriter = builder.getDataWriter();
            DataWriter.DataWriterInstance dataWriterInstance = dataWriter.createDataWriterInstance();
            dataWriterInstance.initialize(fmuInstances.values().stream().flatMap(x -> x.getVariablesToLog().stream()
                    .map(xsv -> new DataWriter.DataWriterInstance.LogEntry(xsv.getMultiModelScalarVariableName(),
                            () -> xsv.getSharedAsVariable().getReferenceExp().clone()))).collect(Collectors.toList()));

            // Create simulation control to allow for user interactive loop stopping
            SimulationControl simulationControl = builder.getSimulationControl();

            // Create the iteration predicate
            PredicateFmi2Api loopPredicate =
                    ctxt.externalEndTimeDefined.toPredicate().not()
                            .or(ctxt.currentCommunicationTime.toMath().addition(ctxt.currentStepSize)
                                    .lessEqualTo(ctxt.endTime));


            // Get all variables related to outputs or logging.
            Map<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Object>>> componentsToPortsWithValues = new HashMap<>();
            fmuInstances.forEach((identifier, instance) -> {
                Set<String> scalarVariablesToGet = instance.getPorts().stream()
                        .filter(p -> jacobianStepConfig.getVariablesOfInterest().stream()
                                .anyMatch(p1 -> p1.equals(p.getMultiModelScalarVariableName())))
                        .map(PortFmi2Api::getName).collect(Collectors.toSet());
                scalarVariablesToGet.addAll(
                        env.getVariablesToLog(instance.getEnvironmentName()).stream().map(RelationVariable::getName)
                                .collect(Collectors.toSet()));

                componentsToPortsWithValues.put(instance, instance.get(scalarVariablesToGet.toArray(String[]::new)));
            });

            // Share
            componentsToPortsWithValues.forEach(ComponentVariableFmi2Api::share);

            // Build static FMU relations and validate if all fmus can get state
            Map<StringVariableFmi2Api, ComponentVariableFmi2Api> fmuNamesToFmuInstances = new LinkedHashMap<>();
            Map<ComponentVariableFmi2Api, VariableFmi2Api<Double>> fmuInstanceToCommunicationPoint = new LinkedHashMap<>();
            ArrayVariableFmi2Api<Double> fmuCommunicationPoints =
                    dynamicScope.store("fmu_communicationpoints", new Double[fmuInstances.entrySet().size()]);
            boolean everyFMUSupportsGetState = true;
            int indexer = 0;
            for (ComponentVariableFmi2Api instance : fmuInstances.values()) {

                FrameworkUnitInfo v = env.getInstanceByLexName(instance.getEnvironmentName());
                if (v instanceof ComponentInfo) {
                    StringVariableFmi2Api fullyQualifiedFMUInstanceName = new StringVariableFmi2Api(null, null, null,
                            null,
                            MableAstFactory.newAStringLiteralExp(
                                    ((ComponentInfo) v).getFmuIdentifier() + "." + instance.getName()));
                    fmuNamesToFmuInstances.put(fullyQualifiedFMUInstanceName, instance);

                    fmuInstanceToCommunicationPoint.put(instance, fmuCommunicationPoints.items().get(indexer));

                    everyFMUSupportsGetState = instance.getModelDescription()
                            .getCanGetAndSetFmustate() && everyFMUSupportsGetState;

                } else {
                    throw new RuntimeException("instance is not fmi2");
                }


                indexer++;
            }


            if (!everyFMUSupportsGetState && jacobianStepConfig.stabilisation) {
                throw new RuntimeException("Cannot use stabilisation as not every FMU supports rollback");
            }

            BooleanVariableFmi2Api allFMUsSupportGetState = dynamicScope.store("all_fmus_support_get_state",
                    everyFMUSupportsGetState);

            JacobianVariableStepBuilder.JacobianVariableStepContext varStep = null;
            if (algorithm == StepAlgorithm.VARIABLESTEP) {
                varStep = JacobianVariableStepBuilder.init(ctxt, jacobianStepConfig, dynamicScope, builder,
                        fmuNamesToFmuInstances);
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
                ScopeFmi2Api stoppingThenScope = scopeFmi2Api.enterIf(simulationControl.stopRequested().toPredicate())
                        .enterThen();
                stoppingThenScope.add(new AErrorStm(newAStringLiteralExp("Simulation stopped by user")));
                stoppingThenScope.leave();

                //mark a safe point for a transfer to another specification
                dynamicScope.markTransferPoint();


                // Update all swap and step condition variables
                ModelSwapBuilder.updateSwapConditionVariables(modelSwapContext, dynamicScope,
                        componentsToPortsWithValues);

                // Get fmu states
                if (everyFMUSupportsGetState) {
                    for (ComponentVariableFmi2Api instance : fmuInstances.values()) {
                        fmuStates.add(instance.getState());
                    }
                }

                if (jacobianStepConfig.stabilisation) {
                    StabilisationBuilder.step(stabilisationCtxt, dynamicScope);

                }

                // SET ALL LINKED VARIABLES
                // This has to be carried out regardless of stabilisation or not.
                ModelSwapBuilder.setWithModelSwapLinking(fmuInstances, env, dynamicScope, modelSwapContext);

                if (algorithm == StepAlgorithm.VARIABLESTEP) {
                    // Get variable step
                    JacobianVariableStepBuilder.updateCurrentStepTiming(ctxt, varStep, dynamicScope, anyDiscards);

                }

                anyDiscards.setValue(new BooleanVariableFmi2Api(null, null, dynamicScope, null,
                        MableAstFactory.newABoolLiteralExp(false)));

                // STEP ALL
                fmuInstanceToCommunicationPoint.forEach((instance, communicationPoint) -> {

                    DoubleVariableFmi2Api communicationTime = ctxt.currentCommunicationTime;

                    Map.Entry<DoubleVariableFmi2Api, Optional<PredicateFmi2Api>> swapStep = ModelSwapBuilder.updateStep(
                            modelSwapContext, env, instance, communicationTime);
                    Optional<PredicateFmi2Api> stepPredicate = swapStep.getValue();
                    communicationTime = swapStep.getKey();

                    stepPredicate.ifPresent(dynamicScope::enterIf);

                    Map.Entry<FmiBuilder.BoolVariable<PStm>, FmiBuilder.DoubleVariable<PStm>> discard =
                            instance.step(communicationTime, ctxt.currentStepSize);

                    communicationPoint.setValue(new DoubleExpressionValue(discard.getValue().getExp()));

                    PredicateFmi2Api didDiscard = new PredicateFmi2Api(discard.getKey().getExp()).not();

                    dynamicScope.enterIf(didDiscard);
                    {
                        builder.getLogger()
                                .debug("## FMU: '%s' DISCARDED step at sim-time: %f for step-size: %f and proposed sim-time: %.15f",
                                        instance.getName(), communicationTime, ctxt.currentStepSize,
                                        new VariableFmi2Api<>(null, discard.getValue().getType(), dynamicScope,
                                                dynamicScope, null,
                                                discard.getValue().getExp()));
                        anyDiscards.setValue(
                                new BooleanVariableFmi2Api(null, null, dynamicScope, null,
                                        anyDiscards.toPredicate().or(didDiscard).getExp()));
                        dynamicScope.leave();
                    }

                    if (stepPredicate.isPresent()) {
                        dynamicScope.leave();
                    }
                });

                // GET ALL LINKED OUTPUTS INCLUDING LOGGING OUTPUTS
                for (Map.Entry<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Object>>> entry : componentsToPortsWithValues.entrySet()) {
                    Map<PortFmi2Api, VariableFmi2Api<Object>> portsToValues = entry.getValue();
                    portsToValues = entry.getKey().get(portsToValues.keySet().toArray(PortFmi2Api[]::new));
                }

                // CONVERGENCE
                if (jacobianStepConfig.stabilisation) {
                    StabilisationBuilder.convergence(dynamicScope, componentsToPortsWithValues, stabilisationCtxt, ctxt,
                            builder, math, booleanLogic, fmuStates);

                }


                if (!jacobianStepConfig.stabilisation) {
                    componentsToPortsWithValues.forEach(ComponentVariableFmi2Api::share);
                }

                if (everyFMUSupportsGetState) {
                    // Discard
                    IfMaBlScope discardScope = dynamicScope.enterIf(anyDiscards.toPredicate());
                    {
                        // Rollback FMUs
                        fmuStates.forEach(FmiBuilder.StateVariable::set);

                        // Set step-size to lowest
                        ctxt.currentStepSize.setValue(math.minRealFromArray(fmuCommunicationPoints).toMath()
                                .subtraction(ctxt.currentCommunicationTime));

                        builder.getLogger()
                                .debug("## Discard occurred! FMUs are rolled back and step-size reduced to: %f",
                                        ctxt.currentStepSize);

                        dynamicScope.leave();
                    }

                    discardScope.enterElse();
                }
                {
                    if (algorithm == StepAlgorithm.VARIABLESTEP) {
                        // Validate step
                        JacobianVariableStepBuilder.step(ctxt, varStep, dynamicScope, builder, allFMUsSupportGetState,
                                fmuStates, anyDiscards);
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
                    ctxt.currentCommunicationTime.setValue(
                            ctxt.currentCommunicationTime.toMath().addition(ctxt.currentStepSize));

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
            errorReporter.report(0, e.toString(), null);
            throw new ExpandException("Internal error: ", e);
        }

        return new EmptyRuntimeConfig<>();
    }

    @Override
    public ConfigOption getConfigRequirement() {
        return ConfigOption.Optional;
    }

    @Override
    public IPluginConfiguration parseConfig(InputStream is) throws IOException {
        return (new ObjectMapper().readValue(is, JacobianStepConfig.class));
    }

    @Override
    public AImportedModuleCompilationUnit getDeclaredImportUnit() {
        AImportedModuleCompilationUnit unit = new AImportedModuleCompilationUnit();
        unit.setImports(imports.stream().map(MableAstFactory::newAIdentifier).collect(Collectors.toList()));
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
        return "1.1.0";
    }

}



