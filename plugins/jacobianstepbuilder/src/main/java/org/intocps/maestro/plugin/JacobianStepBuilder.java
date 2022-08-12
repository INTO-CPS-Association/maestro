package org.intocps.maestro.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.StringEscapeUtils;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.AModuleDeclaration;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.dto.StepAlgorithm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.*;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IfMaBlScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.values.DoubleExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;

@SimulationFramework(framework = Framework.FMI2)
public class JacobianStepBuilder extends BasicMaestroExpansionPlugin {

    final static Logger logger = LoggerFactory.getLogger(JacobianStepBuilder.class);
    final AFunctionDeclaration fixedStepFunc = newAFunctionDeclaration(newAIdentifier("fixedStepSize"),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("stepSize")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("startTime")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("endTime"))), newAVoidType());
    final AFunctionDeclaration variableStepFunc = newAFunctionDeclaration(newAIdentifier("variableStepSize"),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("initSize")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("startTime")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("endTime"))), newAVoidType());
    private final List<String> imports =
            Stream.of("FMI2", "TypeConverter", "Math", "Logger", "DataWriter", "ArrayUtil", "BooleanLogic", "SimulationControl")
                    .collect(Collectors.toList());

    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return Stream.of(fixedStepFunc, variableStepFunc).collect(Collectors.toSet());
    }


    @Override
    public <R> RuntimeConfigAddition<R> expandWithRuntimeAddition(AFunctionDeclaration declaredFunction,
            Fmi2Builder<PStm, ASimulationSpecificationCompilationUnit, PExp, ?> parentBuilder, List<Fmi2Builder.Variable<PStm, ?>> formalArguments,
            IPluginConfiguration config, ISimulationEnvironment envIn, IErrorReporter errorReporter) throws ExpandException {


        logger.info("Unfolding with jacobian step: {}", declaredFunction.toString());
        JacobianStepConfig jacobianStepConfig = config != null ? (JacobianStepConfig) config : new JacobianStepConfig();

        if (!getDeclaredUnfoldFunctions().contains(declaredFunction)) {
            throw new ExpandException("Unknown function declaration");
        }

        if (envIn == null) {
            throw new ExpandException("Simulation environment must not be null");
        }

        AFunctionDeclaration selectedFun;

        StepAlgorithm algorithm;
        if (declaredFunction.getName().toString().equals("variableStepSize")) {
            algorithm = StepAlgorithm.VARIABLESTEP;
            selectedFun = variableStepFunc;
            imports.add("VariableStep");
        } else {
            algorithm = StepAlgorithm.FIXEDSTEP;
            selectedFun = fixedStepFunc;
        }

        if (formalArguments == null || formalArguments.size() != selectedFun.getFormals().size()) {
            throw new ExpandException("Invalid args");
        }

        if (jacobianStepConfig.simulationProgramDelay) {
            imports.add("RealTime");
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
                throw new ExpandException("Not supporting the given builder type. Expecting " + MablApiBuilder.class.getSimpleName() + " got " +
                        parentBuilder.getClass().getSimpleName());
            }

            MablApiBuilder builder = (MablApiBuilder) parentBuilder;

            DynamicActiveBuilderScope dynamicScope = builder.getDynamicScope();
            MathBuilderFmi2Api math = builder.getMablToMablAPI().getMathBuilder();
            BooleanBuilderFmi2Api booleanLogic = builder.getBooleanBuilder();
            RealTime realTimeModule = null;
            if (jacobianStepConfig.simulationProgramDelay) {
                realTimeModule = builder.getRealTimeModule();
            }

            // Convert raw MaBL to API
            DoubleVariableFmi2Api externalStepSize = (DoubleVariableFmi2Api) formalArguments.get(1), currentStepSize =
                    dynamicScope.store("jac_current_step_size", 0.0), stepSize = dynamicScope.store("jac_step_size", 0.0), externalStartTime =
                    (DoubleVariableFmi2Api) formalArguments.get(2), currentCommunicationTime =
                    dynamicScope.store("jac_current_communication_point", 0.0), externalEndTime = (DoubleVariableFmi2Api) formalArguments.get(3),
                    endTime = dynamicScope.store("jac_end_time", 0.0);

            currentStepSize.setValue(externalStepSize);
            stepSize.setValue(externalStepSize);
            currentCommunicationTime.setValue(externalStartTime);
            endTime.setValue(externalEndTime);


            // Get FMU instances - use LinkedHashMap to preserve added order
            Map<String, ComponentVariableFmi2Api> fmuInstances =
                    ((List<ComponentVariableFmi2Api>) ((Fmi2Builder.ArrayVariable) formalArguments.get(0)).items()).stream()
                            .collect(Collectors.toMap(ComponentVariableFmi2Api::getName, Function.identity(), (u, v) -> u, LinkedHashMap::new));

            // Create the logging
            DataWriter dataWriter = builder.getDataWriter();
            DataWriter.DataWriterInstance dataWriterInstance = dataWriter.createDataWriterInstance();
            dataWriterInstance.initialize(fmuInstances.values().stream().flatMap(x -> x.getVariablesToLog().stream()).collect(Collectors.toList()));

            // Create simulation control to allow for user interactive loop stopping
            SimulationControl simulationControl = builder.getSimulationControl();

            // Create the iteration predicate
            PredicateFmi2Api loopPredicate = currentCommunicationTime.toMath().addition(currentStepSize).lessThan(endTime);

            // Get all variables related to outputs or logging.
            Map<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Object>>> componentsToPortsWithValues = new HashMap<>();
            fmuInstances.forEach((identifier, instance) -> {
                Set<String> scalarVariablesToGet = instance.getPorts().stream().filter(p -> jacobianStepConfig.getVariablesOfInterest().stream()
                        .anyMatch(p1 -> p1.equals(p.getMultiModelScalarVariableName()))).map(PortFmi2Api::getName).collect(Collectors.toSet());
                scalarVariablesToGet.addAll(env.getVariablesToLog(instance.getEnvironmentName()).stream().map(var -> var.scalarVariable.getName())
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
                StringVariableFmi2Api fullyQualifiedFMUInstanceName = new StringVariableFmi2Api(null, null, null, null,
                        MableAstFactory.newAStringLiteralExp(
                                env.getInstanceByLexName(instance.getEnvironmentName()).getFmuIdentifier() + "." + instance.getName()));
                fmuNamesToFmuInstances.put(fullyQualifiedFMUInstanceName, instance);

                fmuInstanceToCommunicationPoint.put(instance, fmuCommunicationPoints.items().get(indexer));

                everyFMUSupportsGetState = instance.getModelDescription().getCanGetAndSetFmustate() && everyFMUSupportsGetState;

                indexer++;
            }


            if (!everyFMUSupportsGetState && jacobianStepConfig.stabilisation) {
                throw new RuntimeException("Cannot use stabilisation as not every FMU supports rollback");
            }

            BooleanVariableFmi2Api allFMUsSupportGetState = dynamicScope.store("all_fmus_support_get_state", everyFMUSupportsGetState);

            VariableStep variableStep;
            VariableStep.VariableStepInstance variableStepInstance = null;
            if (algorithm == StepAlgorithm.VARIABLESTEP) {
                // Initialize variable step module
                List<PortFmi2Api> ports = fmuInstances.values().stream().map(ComponentVariableFmi2Api::getPorts).flatMap(Collection::stream)
                        .filter(p -> jacobianStepConfig.getVariablesOfInterest().stream()
                                .anyMatch(p1 -> p1.equals(p.getMultiModelScalarVariableName()))).collect(Collectors.toList());

                variableStep = builder.getVariableStep(dynamicScope.store("variable_step_config",
                        StringEscapeUtils.escapeJava((new ObjectMapper()).writeValueAsString(jacobianStepConfig.stepAlgorithm))));
                variableStepInstance = variableStep.createVariableStepInstanceInstance();
                variableStepInstance.initialize(fmuNamesToFmuInstances, ports, endTime);
            }

            // Log values at t = start time
            dataWriterInstance.log(currentCommunicationTime);

            ScopeFmi2Api stabilisationScope = null;
            IntVariableFmi2Api stabilisation_loop = null;
            BooleanVariableFmi2Api convergenceReached = null;
            DoubleVariableFmi2Api absTol = null, relTol = null;
            IntVariableFmi2Api stabilisation_loop_max_iterations = null;
            if (jacobianStepConfig.stabilisation) {
                absTol = dynamicScope.store("absolute_tolerance", jacobianStepConfig.absoluteTolerance);
                relTol = dynamicScope.store("relative_tolerance", jacobianStepConfig.relativeTolerance);
                stabilisation_loop_max_iterations =
                        dynamicScope.store("stabilisation_loop_max_iterations", jacobianStepConfig.stabilisationLoopMaxIterations);
                stabilisation_loop = dynamicScope.store("stabilisation_loop", stabilisation_loop_max_iterations);
                convergenceReached = dynamicScope.store("has_converged", false);
            }

            DoubleVariableFmi2Api realStartTime = null;
            if (jacobianStepConfig.simulationProgramDelay) {
                realStartTime = dynamicScope.store("real_start_time", 0.0);
                realStartTime.setValue(Objects.requireNonNull(realTimeModule).getRealTime());
            }

            List<Fmi2Builder.StateVariable<PStm>> fmuStates = new ArrayList<>();
            BooleanVariableFmi2Api anyDiscards = dynamicScope.store("any_discards", false);
            ScopeFmi2Api scopeFmi2Api = dynamicScope.enterWhile(loopPredicate);
            {
                ScopeFmi2Api stoppingThenScope = scopeFmi2Api.enterIf(simulationControl.stopRequested().toPredicate()).enterThen();
                stoppingThenScope.add(new AErrorStm(newAStringLiteralExp("Simulation stopped by user")));
                stoppingThenScope.leave();
                // Get fmu states
                if (everyFMUSupportsGetState) {
                    for (ComponentVariableFmi2Api instance : fmuInstances.values()) {
                        fmuStates.add(instance.getState());
                    }
                }

                if (jacobianStepConfig.stabilisation) {
                    stabilisation_loop.setValue(stabilisation_loop_max_iterations);
                    convergenceReached.setValue(
                            new BooleanVariableFmi2Api(null, null, dynamicScope, null, MableAstFactory.newABoolLiteralExp(false)));
                    stabilisationScope = dynamicScope.enterWhile(
                            convergenceReached.toPredicate().not().and(stabilisation_loop.toMath().greaterThan(IntExpressionValue.of(0))));
                }

                // SET ALL LINKED VARIABLES
                // This has to be carried out regardless of stabilisation or not.
                fmuInstances.values().forEach(instance -> {
                    if (instance.getPorts().stream().anyMatch(p -> p.getSourcePort() != null)) {
                        instance.setLinked();
                    }
                });

                if (algorithm == StepAlgorithm.VARIABLESTEP) {
                    // Get variable step
                    DoubleVariableFmi2Api variableStepSize = dynamicScope.store("variable_step_size", 0.0);
                    dynamicScope.enterIf(anyDiscards.toPredicate().not());
                    {
                        variableStepSize.setValue(variableStepInstance.getStepSize(currentCommunicationTime));
                        currentStepSize.setValue(variableStepSize);
                        stepSize.setValue(variableStepSize);
                        dynamicScope.leave();
                    }
                }

                anyDiscards.setValue(new BooleanVariableFmi2Api(null, null, dynamicScope, null, MableAstFactory.newABoolLiteralExp(false)));

                // STEP ALL
                fmuInstanceToCommunicationPoint.forEach((instance, communicationPoint) -> {
                    Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>> discard =
                            instance.step(currentCommunicationTime, currentStepSize);

                    communicationPoint.setValue(new DoubleExpressionValue(discard.getValue().getExp()));

                    PredicateFmi2Api didDiscard = new PredicateFmi2Api(discard.getKey().getExp()).not();

                    dynamicScope.enterIf(didDiscard);
                    {
                        builder.getLogger().debug("## FMU: '%s' DISCARDED step at sim-time: %f for step-size: %f and proposed sim-time: %.15f",
                                instance.getName(), currentCommunicationTime, currentStepSize,
                                new VariableFmi2Api<>(null, discard.getValue().getType(), dynamicScope, dynamicScope, null,
                                        discard.getValue().getExp()));
                        anyDiscards.setValue(
                                new BooleanVariableFmi2Api(null, null, dynamicScope, null, anyDiscards.toPredicate().or(didDiscard).getExp()));
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
                    // For each instance ->
                    //      For each retrieved variable
                    //          compare with previous in terms of convergence
                    //  If all converge, set retrieved values and continue
                    //  else reset to previous state, set retrieved values and continue
                    List<BooleanVariableFmi2Api> convergenceVariables = new ArrayList<>();
                    for (Map<PortFmi2Api, VariableFmi2Api<Object>> portsToValues : componentsToPortsWithValues.values()) {
                        List<BooleanVariableFmi2Api> converged = new ArrayList<>();
                        Map<PortFmi2Api, VariableFmi2Api<Object>> portsToValuesOfInterest = portsToValues.entrySet().stream()
                                .filter(ptv -> ptv.getKey().scalarVariable.type.type == Fmi2ModelDescription.Types.Real &&
                                        (ptv.getKey().scalarVariable.causality == Fmi2ModelDescription.Causality.Output ||
                                                ptv.getKey().scalarVariable.causality == Fmi2ModelDescription.Causality.Input))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        for (Map.Entry<PortFmi2Api, VariableFmi2Api<Object>> entry : portsToValuesOfInterest.entrySet()) {
                            VariableFmi2Api oldVariable = entry.getKey().getSharedAsVariable();
                            VariableFmi2Api<Object> newVariable = entry.getValue();
                            BooleanVariableFmi2Api isClose = dynamicScope.store("isClose", false);
                            isClose.setValue(math.checkConvergence(oldVariable, newVariable, absTol, relTol));
                            dynamicScope.enterIf(isClose.toPredicate().not());
                            {
                                builder.getLogger()
                                        .debug("Unstable signal %s = %.15E at time: %.15E", entry.getKey().getMultiModelScalarVariableName(),
                                                entry.getValue(), currentCommunicationTime);
                                dynamicScope.leave();
                            }
                            converged.add(isClose);
                        }
                        convergenceVariables.addAll(converged);
                    }

                    if (convergenceReached != null) {
                        convergenceReached.setValue(booleanLogic.allTrue("convergence", convergenceVariables));
                    } else {
                        throw new RuntimeException("NO STABILISATION LOOP FOUND");
                    }
                    // Rollback
                    dynamicScope.enterIf(convergenceReached.toPredicate().not()).enterThen();
                    {
                        fmuStates.forEach(Fmi2Builder.StateVariable::set);
                        stabilisation_loop.decrement();
                        dynamicScope.leave();
                    }
                    componentsToPortsWithValues.forEach(ComponentVariableFmi2Api::share);
                    stabilisationScope.leave();
                }


                if (!jacobianStepConfig.stabilisation) {
                    componentsToPortsWithValues.forEach(ComponentVariableFmi2Api::share);
                }

                if (everyFMUSupportsGetState) {
                    // Discard
                    IfMaBlScope discardScope = dynamicScope.enterIf(anyDiscards.toPredicate());
                    {
                        // Rollback FMUs
                        fmuStates.forEach(Fmi2Builder.StateVariable::set);

                        // Set step-size to lowest
                        currentStepSize.setValue(math.minRealFromArray(fmuCommunicationPoints).toMath().subtraction(currentCommunicationTime));

                        builder.getLogger().debug("## Discard occurred! FMUs are rolled back and step-size reduced to: %f", currentStepSize);

                        dynamicScope.leave();
                    }

                    discardScope.enterElse();
                }
                {
                    if (algorithm == StepAlgorithm.VARIABLESTEP) {
                        // Validate step
                        PredicateFmi2Api notValidStepPred = Objects.requireNonNull(variableStepInstance).validateStepSize(
                                        new DoubleVariableFmi2Api(null, null, dynamicScope, null,
                                                currentCommunicationTime.toMath().addition(currentStepSize).getExp()), allFMUsSupportGetState).toPredicate()
                                .not();

                        BooleanVariableFmi2Api hasReducedStepSize = new BooleanVariableFmi2Api(null, null, dynamicScope, null,
                                Objects.requireNonNull(variableStepInstance).hasReducedStepsize().getReferenceExp());

                        dynamicScope.enterIf(notValidStepPred);
                        {
                            IfMaBlScope reducedStepSizeScope = dynamicScope.enterIf(hasReducedStepSize.toPredicate());
                            {
                                // Rollback FMUs
                                fmuStates.forEach(Fmi2Builder.StateVariable::set);

                                // Set step-size to suggested size
                                currentStepSize.setValue(Objects.requireNonNull(variableStepInstance).getReducedStepSize());

                                builder.getLogger()
                                        .debug("## Invalid variable step-size! FMUs are rolled back and step-size reduced to: %f", currentStepSize);

                                anyDiscards.setValue(
                                        new BooleanVariableFmi2Api(null, null, dynamicScope, null, anyDiscards.toPredicate().not().getExp()));

                                dynamicScope.leave();
                            }
                            reducedStepSizeScope.enterElse();
                            {
                                builder.getLogger()
                                        .debug("## The step could not be validated by the constraint at time %f. Continue nevertheless " + "with" +
                                                " next simulation step!", currentCommunicationTime);
                                dynamicScope.leave();
                            }

                            dynamicScope.leave();
                        }
                    }

                    // Slow-down to real-time
                    if (jacobianStepConfig.simulationProgramDelay) {
                        DoubleVariableFmi2Api realStepTime = dynamicScope.store("real_step_time", 0.0);
                        realStepTime.setValue(
                                new DoubleExpressionValue(realTimeModule.getRealTime().toMath().subtraction(realStartTime.toMath()).getExp()));

                        dynamicScope.enterIf(realStepTime.toMath().lessThan(currentCommunicationTime.toMath().multiply(1000)));
                        {
                            DoubleVariableFmi2Api sleepTime = dynamicScope.store("sleep_time", 0.0);
                            sleepTime.setValue(currentCommunicationTime.toMath().multiply(1000).subtraction(realStepTime));
                            builder.getLogger().debug("## Simulation is ahead of real time. Sleeping for: %f MS", sleepTime);
                            realTimeModule.sleep(sleepTime);
                            dynamicScope.leave();
                        }
                    }

                    if (everyFMUSupportsGetState) {
                        dynamicScope.leave();
                    }
                }

                dynamicScope.enterIf(anyDiscards.toPredicate().not());
                {
                    // Update currentCommunicationTime
                    currentCommunicationTime.setValue(currentCommunicationTime.toMath().addition(currentStepSize));

                    // Log values at current communication point
                    dataWriterInstance.log(currentCommunicationTime);
                    currentStepSize.setValue(stepSize);
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
        return "0.1.1";
    }
}



