package org.intocps.maestro.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.AModuleDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.dto.StepAlgorithm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.ModelSwapInfo;
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

    final AFunctionDeclaration fixedStepTransferFunc = newAFunctionDeclaration(newAIdentifier("fixedStepSizeTransfer"),
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
            Stream.of("FMI2", "TypeConverter", "Math", "Logger", "DataWriter", "ArrayUtil", "BooleanLogic").collect(Collectors.toList());

    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return Stream.of(fixedStepFunc, variableStepFunc, fixedStepTransferFunc).collect(Collectors.toSet());
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
        } else if (declaredFunction.getName().toString().equals("fixedStepSizeTransfer")) {
            algorithm = StepAlgorithm.FIXEDSTEP;
            selectedFun = fixedStepFunc;
            imports.add("ModelTransition");
            logger.debug("Activated model transfer");
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

            // Create the iteration predicate
            PredicateFmi2Api loopPredicate = currentCommunicationTime.toMath().addition(currentStepSize).lessThan(endTime);

            // Get all variables related to outputs or logging.
            Map<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Object>>> componentsToPortsWithValues = new HashMap<>();
            fmuInstances.forEach((identifier, instance) -> {
                Set<String> scalarVariablesToGet = instance.getPorts().stream().filter(p -> jacobianStepConfig.getVariablesOfInterest().stream()
                        .anyMatch(p1 -> p1.equals(p.getMultiModelScalarVariableName()))).map(PortFmi2Api::getName).collect(Collectors.toSet());
                scalarVariablesToGet.addAll(env.getVariablesToLog(instance.getEnvironmentName()).stream().map(var -> var.scalarVariable.getName())
                        .collect(Collectors.toSet()));

//                componentsToPortsWithValues.put(instance, instance.get(scalarVariablesToGet.toArray(String[]::new)));
                // fixme: only add swap instances
                componentsToPortsWithValues.put(instance, instance.get());
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

            // Initialise swap and step condition variables
            Map<ModelSwapInfo, Pair<BooleanVariableFmi2Api, BooleanVariableFmi2Api>> modelSwapConditions = new HashMap<>();
            List<ModelSwapInfo> swapInfoList = env.getModelSwaps().stream().map(Map.Entry::getValue).collect(Collectors.toList());
            int index = 0;
            for (ModelSwapInfo info : swapInfoList) {
                BooleanVariableFmi2Api swapVar = dynamicScope.store("swapCondition" + index, false);
                BooleanVariableFmi2Api stepVar = dynamicScope.store("stepCondition" + index, false);
                modelSwapConditions.put(info, Pair.of(swapVar, stepVar));
                index++;
            }

            ScopeFmi2Api scopeFmi2Api = dynamicScope.enterWhile(loopPredicate);
            {
                //mark a safe point for a transfer to another specification
                dynamicScope.markTransferPoint();

                Map<String, PExp> replaceRule = componentsToPortsWithValues.entrySet().stream().flatMap(
                                c -> c.getValue().entrySet().stream().map(p -> Map.entry(p.getKey().getMultiModelScalarVariableNameWithoutFmu(),
                                        p.getKey().getSharedAsVariable().getReferenceExp())))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                // Update all swap and step condition variables
                modelSwapConditions.forEach((info, value) -> {
                    BooleanVariableFmi2Api swapVar = value.getKey();
                    BooleanVariableFmi2Api stepVar = value.getValue();
                    swapVar.setValue(
                            new BooleanVariableFmi2Api(null, null, dynamicScope, null,
                                    MableAstFactory.newOr(swapVar.getExp(), IdentifierReplacer.replaceFields(info.swapCondition, replaceRule))));
                    stepVar.setValue(
                            new BooleanVariableFmi2Api(null, null, dynamicScope, null,
                                    MableAstFactory.newOr(stepVar.getExp(), IdentifierReplacer.replaceFields(info.stepCondition, replaceRule))));
                });

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
                    //  HE: fixme, remove this comment block - only for devel
                    //  if instance is source in swap and has existing source ports
                    //      enterif(swapCond.not)
                    //      inst.setLinked() // all ports
                    //  else if instance is target in swap and has swap source ports
                    //      create links from swap source ports to ports
                    //      enterif(swapCond)
                    //      inst.setLinked() // all ports
                    //  else if instance is connected in swap relation
                    //      for each port of instance
                    //              if port is target in swap relation
                    //                  if port has existing source port
                    //                      enterIf(swapCond.not)
                    //                      instance.setLinked(port)
                    //                      p.breakLink()
                    //                  create link from swap source port to port
                    //                  enterIf(swapCond)
                    //                  instance.setLinked(p)
                    //              else
                    //                  instance.setLinked(p)
                    //  else if instance has source ports
                    //      inst.setLinked()

                    Set<Fmi2SimulationEnvironment.Relation> swapRelations = env.getModelSwapRelations();
                    Optional<Map.Entry<String, ModelSwapInfo>> swapInfoSource =
                            env.getModelSwaps().stream().filter(e -> e.getKey().equals(instance.getName())).findFirst();
                    Optional<Map.Entry<String, ModelSwapInfo>> swapInfoTarget =
                            env.getModelSwaps().stream().filter(e -> e.getValue().swapInstance.equals(instance.getName())).findFirst();

                    if (swapInfoSource.isPresent()) {
                        if (instance.getPorts().stream().anyMatch(port -> port.getSourcePort() != null)) {
                            PredicateFmi2Api swapPredicate =
                                    modelSwapConditions.get(swapInfoSource.get().getValue()).getKey().toPredicate();
                            dynamicScope.enterIf(swapPredicate.not());
                            instance.setLinked();
                            dynamicScope.leave();
                        }
                    } else if (swapInfoTarget.isPresent()) {
                        instance.getPorts().forEach(port -> {
                            PortFmi2Api sourcePort = getSwapSourcePort(port, swapRelations, fmuInstances);
                            if (sourcePort != null) {
                                try {
                                    sourcePort.linkTo(port);
                                } catch (Fmi2Builder.Port.PortLinkException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        if (instance.getPorts().stream().anyMatch(port -> port.getSourcePort() != null)) {
                            PredicateFmi2Api swapPredicate =
                                    modelSwapConditions.get(swapInfoTarget.get().getValue()).getKey().toPredicate();
                            dynamicScope.enterIf(swapPredicate);
                            instance.setLinked();
                            dynamicScope.leave();
                        }
                    } else if (instance.getPorts().stream().anyMatch(port -> getSwapSourcePort(port, swapRelations, fmuInstances) != null)) {
                        instance.getPorts().forEach(port -> {
                            PortFmi2Api swapSourcePort = getSwapSourcePort(port, swapRelations, fmuInstances);

                            if (swapSourcePort != null) {
                                ModelSwapInfo swapInfo = getSwapSourceInfo(port, swapRelations, fmuInstances, env);
                                PredicateFmi2Api swapPredicate =
                                        modelSwapConditions.get(swapInfo).getKey().toPredicate();

                                if (port.getSourcePort() != null) {
                                    dynamicScope.enterIf(swapPredicate.not());
                                    instance.setLinked(port);
                                    dynamicScope.leave();
                                    port.breakLink();
                                }

                                try {
                                    swapSourcePort.linkTo(port);
                                } catch (Fmi2Builder.Port.PortLinkException e) {
                                    e.printStackTrace();
                                }

                                dynamicScope.enterIf(swapPredicate);
                                instance.setLinked(port);
                                dynamicScope.leave();

                            } else if (port.getSourcePort() != null) {
                                instance.setLinked(port);
                            }
                        });
                    }
                    else if (instance.getPorts().stream().anyMatch(p -> p.getSourcePort() != null)) {
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
                    PredicateFmi2Api stepPredicate = null;
                    for (Map.Entry<String, ModelSwapInfo> modelSwapInfoEntry : env.getModelSwaps()) {
                        if (instance.getName().equals(modelSwapInfoEntry.getKey()) ||
                                instance.getName().equals(modelSwapInfoEntry.getValue().swapInstance)) {

                            //fixme handle cases where we cannot replace all field expressions
                            stepPredicate =
                                    modelSwapConditions.get(modelSwapInfoEntry.getValue()).getValue().toPredicate();

                            if (instance.getName().equals(modelSwapInfoEntry.getKey())) {
                                stepPredicate = stepPredicate.not();
                            }
                            break;
                        }
                    }

                    if (stepPredicate != null) {
                        dynamicScope.enterIf(stepPredicate);
                    }

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

                    if (stepPredicate != null) {
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

    private PortFmi2Api getSwapSourcePort(PortFmi2Api port, Set<Fmi2SimulationEnvironment.Relation> swapRelations,
            Map<String, ComponentVariableFmi2Api> fmuInstances) {
        PortFmi2Api sourcePort = null;
        Optional<Fmi2SimulationEnvironment.Relation> relation =
                swapRelations.stream().filter(r -> r.getTargets().values().stream()
                        .anyMatch(v -> v.toString().equals(port.getMultiModelScalarVariableNameWithoutFmu()))).findFirst();
        if (relation.isPresent()) {
            String source = relation.get().getSource().scalarVariable.instance.getText();
            sourcePort = fmuInstances.get(source).getPort(relation.get().getSource().scalarVariable.scalarVariable.getName());
        }
        return sourcePort;
    }

    private ModelSwapInfo getSwapSourceInfo(PortFmi2Api port, Set<Fmi2SimulationEnvironment.Relation> swapRelations,
            Map<String, ComponentVariableFmi2Api> fmuInstances, Fmi2SimulationEnvironment env) {
        ModelSwapInfo swapInfo = null;
        Optional<Fmi2SimulationEnvironment.Relation> relation =
                swapRelations.stream().filter(r -> r.getTargets().values().stream()
                        .anyMatch(v -> v.toString().equals(port.getMultiModelScalarVariableNameWithoutFmu()))).findFirst();
        if (relation.isPresent()) {
            String source = relation.get().getSource().scalarVariable.instance.getText();
            Optional<Map.Entry<String, ModelSwapInfo>> infoEntry =
                    env.getModelSwaps().stream().filter(e -> e.getValue().swapInstance.equals(source)).findFirst();
            if (infoEntry.isPresent()) {
                swapInfo = infoEntry.get().getValue();
            }
        }

        return swapInfo;
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

    static class IdentifierReplacer {
        public static PExp replaceIdentifier(PExp tree, String oldIdentifier, String newIdentifier) {
            return replaceIdentifier(tree, new HashMap<>() {
                {
                    put(oldIdentifier, newIdentifier);
                }
            });
        }

        static public PExp replaceIdentifier(PExp tree, Map<String, String> old2New) {
            try {
                tree.apply(new IdentifierReplace(old2New));
            } catch (AnalysisException e) {
                throw new RuntimeException(e);
            }
            return tree;
        }

        static public PExp replaceFields(PExp tree, Map<String, PExp> old2New) {
            try {
                tree.apply(new FieldExpReplace(old2New));
            } catch (AnalysisException e) {
                throw new RuntimeException(e);
            }
            return tree;
        }

        static class IdentifierReplace extends DepthFirstAnalysisAdaptor {
            final Map<String, String> old2New;

            public IdentifierReplace(Map<String, String> old2New) {
                this.old2New = old2New;
            }

            @Override
            public void caseAIdentifierExp(AIdentifierExp node) throws AnalysisException {
                for (Map.Entry<String, String> replacing : old2New.entrySet()) {
                    if (node.getName().getText().equals(replacing.getKey())) {
                        node.parent().replaceChild(node, new AIdentifierExp(new LexIdentifier(replacing.getValue(), null)));
                    }
                }
            }
        }

        static class FieldExpReplace extends DepthFirstAnalysisAdaptor {
            final Map<String, PExp> old2New;

            public FieldExpReplace(Map<String, PExp> old2New) {
                this.old2New = old2New;
            }

            @Override
            public void caseAFieldExp(AFieldExp node) throws AnalysisException {
                for (Map.Entry<String, PExp> replacing : old2New.entrySet()) {
                    String[] parts = replacing.getKey().split("\\.");
                    if (node.getRoot() instanceof AIdentifierExp && ((AIdentifierExp) node.getRoot()).getName().getText().equals(parts[0]) &&
                            node.getField().getText().equals(parts[1])) {
                        node.parent().replaceChild(node, replacing.getValue().clone());
                    }
                }
            }


        }

    }
}



