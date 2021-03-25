package org.intocps.maestro.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.AModuleDeclaration;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.ToParExp;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.ABlockStm;
import org.intocps.maestro.ast.node.AImportedModuleCompilationUnit;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.api.StepAlgorithm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.RelationVariable;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.*;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IfMaBlScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.values.DoubleExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;

@SimulationFramework(framework = Framework.FMI2)
public class JacobianStepBuilder implements IMaestroExpansionPlugin {

    final static Logger logger = LoggerFactory.getLogger(JacobianStepBuilder.class);
    final AFunctionDeclaration fixedStepFunc = newAFunctionDeclaration(newAIdentifier("fixedStep"),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("stepSize")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("startTime")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("endTime"))), newAVoidType());
    final AFunctionDeclaration variableStepFunc = newAFunctionDeclaration(newAIdentifier("variableStep"),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("initSize")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("startTime")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("endTime"))), newAVoidType());
    private StepAlgorithm algorithm;

    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return Stream.of(fixedStepFunc, variableStepFunc).collect(Collectors.toSet());
    }

    @Override
    public List<PStm> expand(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config,
            ISimulationEnvironment envIn, IErrorReporter errorReporter) throws ExpandException {

        logger.info("Unfolding with jacobian step: {}", declaredFunction.toString());
        JacobianStepConfig jacobianStepConfig = (JacobianStepConfig) config;

        if (!getDeclaredUnfoldFunctions().contains(declaredFunction)) {
            throw new ExpandException("Unknown function declaration");
        }

        AFunctionDeclaration selectedFun;

        if (declaredFunction.getName().toString().equals("variableStep")) {
            algorithm = StepAlgorithm.VARIABLESTEP;
            selectedFun = variableStepFunc;
        } else {
            algorithm = StepAlgorithm.FIXEDSTEP;
            selectedFun = fixedStepFunc;
        }

        if (formalArguments == null || formalArguments.size() != selectedFun.getFormals().size()) {
            throw new ExpandException("Invalid args");
        }

        if (envIn == null) {
            throw new ExpandException("Simulation environment must not be null");
        }

        Fmi2SimulationEnvironment env = (Fmi2SimulationEnvironment) envIn;
        PExp stepSizeVar = formalArguments.get(1).clone();
        PExp startTime = formalArguments.get(2).clone();
        PExp endTimeVar = formalArguments.get(3).clone();

        try {
            MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
            settings.fmiErrorHandlingEnabled = false;
            // Selected fun now matches funWithBuilder
            MablApiBuilder builder = new MablApiBuilder(settings, true);

            DynamicActiveBuilderScope dynamicScope = builder.getDynamicScope();
            MathBuilderFmi2Api math = builder.getMablToMablAPI().getMathBuilder();
            BooleanBuilderFmi2Api booleanLogic = builder.getMablToMablAPI().getBooleanBuilder();

            // Convert raw MaBL to API
            DoubleVariableFmi2Api externalStepSize = builder.getDoubleVariableFrom(stepSizeVar);
            DoubleVariableFmi2Api currentStepSize = dynamicScope.store("current_step_size", 0.0);
            currentStepSize.setValue(externalStepSize);

            DoubleVariableFmi2Api stepSize = dynamicScope.store("step_size", 0.0);
            stepSize.setValue(externalStepSize);

            DoubleVariableFmi2Api externalStartTime = new DoubleVariableFmi2Api(null, null, null, null, startTime);
            DoubleVariableFmi2Api currentCommunicationTime = dynamicScope.store("current_communication_point", 0.0);
            currentCommunicationTime.setValue(externalStartTime);
            DoubleVariableFmi2Api externalEndTime = new DoubleVariableFmi2Api(null, null, null, null, endTimeVar);
            DoubleVariableFmi2Api endTime = dynamicScope.store("end_time", 0.0);
            endTime.setValue(externalEndTime);

            // Import the external components into Fmi2API
            Map<String, ComponentVariableFmi2Api> fmuInstances = FromMaBLToMaBLAPI.GetComponentVariablesFrom(builder, formalArguments.get(0), env);

            // Create bindings
            FromMaBLToMaBLAPI.CreateBindings(fmuInstances, env);

            // Create the logging
            DataWriter dataWriter = builder.getMablToMablAPI().getDataWriter();
            DataWriter.DataWriterInstance dataWriterInstance = dataWriter.createDataWriterInstance();
            dataWriterInstance.initialize(fmuInstances.values().stream().flatMap(x -> x.getVariablesToLog().stream()).collect(Collectors.toList()));


            // Create the iteration predicate
            PredicateFmi2Api loopPredicate = currentCommunicationTime.toMath().addition(currentStepSize).lessThan(endTime);

            // Get and share all variables related to outputs or logging.
            Map<ComponentVariableFmi2Api, List<String>> portsToGet = new HashMap<>();

            fmuInstances.forEach((x, y) -> {
                List<RelationVariable> variablesToLog = env.getVariablesToLog(x);

                Set<String> variablesToShare = y.getPorts().stream()
                        .filter(p -> jacobianStepConfig.variablesOfInterest.stream().anyMatch(p1 -> p1.equals(p.getLogScalarVariableName())))
                        .map(PortFmi2Api::getName).collect(Collectors.toSet());
                variablesToShare.addAll(variablesToLog.stream().map(var -> var.scalarVariable.getName()).collect(Collectors.toSet()));

                Map<PortFmi2Api, VariableFmi2Api<Object>> portsToShare = y.get(variablesToShare.toArray(String[]::new));

                List<String> portsOfInterest = portsToShare.keySet().stream()
                        .filter(objectVariableFmi2Api -> objectVariableFmi2Api.scalarVariable.causality == ModelDescription.Causality.Output ||
                                objectVariableFmi2Api.scalarVariable.causality == ModelDescription.Causality.Input).map(PortFmi2Api::getName)
                        .collect(Collectors.toList());

                portsToGet.put(y, portsOfInterest);
                y.share(portsToShare);
            });

            // Build static FMU relations and validate if all fmus can get state
            Map<StringVariableFmi2Api, ComponentVariableFmi2Api> fmuNamesToInstances = new HashMap<>();
            Map<ComponentVariableFmi2Api, VariableFmi2Api<Double>> fmuInstancesToVariablesInArray = new HashMap<>();
            ArrayVariableFmi2Api<Double> fmuCommunicationPoints =
                    dynamicScope.store("fmu_communicationpoints", new Double[fmuInstances.entrySet().size()]);
            boolean everyFMUSupportsGetState = true;
            int indexer = 0;
            for (Map.Entry<String, ComponentVariableFmi2Api> entry : fmuInstances.entrySet()) {
                StringVariableFmi2Api fullyQualifiedFMUInstanceName = new StringVariableFmi2Api(null, null, null, null, MableAstFactory
                        .newAStringLiteralExp(
                                env.getInstanceByLexName(entry.getValue().getName()).getFmuIdentifier() + "." + entry.getValue().getName()));
                fmuNamesToInstances.put(fullyQualifiedFMUInstanceName, entry.getValue());

                fmuInstancesToVariablesInArray.put(entry.getValue(), fmuCommunicationPoints.items().get(indexer));

                everyFMUSupportsGetState = entry.getValue().getModelDescription().getCanGetAndSetFmustate() && everyFMUSupportsGetState;
                indexer++;
            }
            BooleanVariableFmi2Api canGetFMUStates = dynamicScope.store("can_get_FMU_states", everyFMUSupportsGetState);

            VariableStep variableStep;
            VariableStep.VariableStepInstance variableStepInstance = null;
            if (algorithm == StepAlgorithm.VARIABLESTEP) {
                // Initialize variable step module
                List<PortFmi2Api> ports = fmuInstances.values().stream().map(ComponentVariableFmi2Api::getPorts).flatMap(Collection::stream)
                        .filter(p -> jacobianStepConfig.variablesOfInterest.stream().anyMatch(p1 -> p1.equals(p.getLogScalarVariableName())))
                        .collect(Collectors.toList());

                variableStep = builder.getMablToMablAPI().getVariableStep();
                variableStepInstance = variableStep.createVariableStepInstanceInstance();
                variableStepInstance.initialize(fmuNamesToInstances, ports, endTime);
            }

            List<Fmi2Builder.StateVariable<PStm>> fmuStates = new ArrayList<>();

            BooleanVariableFmi2Api anyDiscards = dynamicScope.store("any_discards", false);

            // Call log
            dataWriterInstance.log(currentCommunicationTime);

            ScopeFmi2Api stabilisationScope = null;
            IntVariableFmi2Api stabilisation_loop = null;
            BooleanVariableFmi2Api convergenceReached = null;
            DoubleVariableFmi2Api absTol = null;
            DoubleVariableFmi2Api relTol = null;
            IntVariableFmi2Api stabilisation_loop_max_iterations = null;

            if (jacobianStepConfig.stabilisation) {
                absTol = dynamicScope.store("absolute_tolerance", jacobianStepConfig.absoluteTolerance);
                relTol = dynamicScope.store("relative_tolerance", jacobianStepConfig.relativeTolerance);
                stabilisation_loop_max_iterations =
                        dynamicScope.store("stabilisation_loop_max_iterations", jacobianStepConfig.stabilisationLoopMaxIterations);
                stabilisation_loop = dynamicScope.store("stabilisation_loop", stabilisation_loop_max_iterations);
                convergenceReached = dynamicScope.store("has_converged", false);
            }

            ScopeFmi2Api scopeFmi2Api = dynamicScope.enterWhile(loopPredicate);
            {

                // FMU get state not supported
                dynamicScope.enterIf(canGetFMUStates.toPredicate().not());
                {
                    //TODO: What should be done?
                    dynamicScope.leave();
                }


                if (everyFMUSupportsGetState) {
                    fmuStates.clear();
                    for (Map.Entry<String, ComponentVariableFmi2Api> entry : fmuInstances.entrySet()) {
                        fmuStates.add(entry.getValue().getState());
                    }
                }

                if (jacobianStepConfig.stabilisation) {
                    stabilisation_loop.setValue(stabilisation_loop_max_iterations);
                    convergenceReached
                            .setValue(new BooleanVariableFmi2Api(null, null, dynamicScope, null, MableAstFactory.newABoolLiteralExp(false)));
                    stabilisationScope = dynamicScope.enterWhile(
                            convergenceReached.toPredicate().not().and(stabilisation_loop.toMath().greaterThan(IntExpressionValue.of(0))));
                }

                // SET ALL LINKED VARIABLES
                // This has to be carried out regardless of stabilisation or not.
                fmuInstances.forEach((x, y) -> y.setLinked());

                if (algorithm == StepAlgorithm.VARIABLESTEP) {
                    dynamicScope.enterIf(anyDiscards.toPredicate().not());
                    {
                        DoubleVariableFmi2Api variableStepSize = dynamicScope.store("variable_step_size", 0.0);
                        variableStepSize.setValue(variableStepInstance.getStepSize(currentCommunicationTime));
                        currentStepSize.setValue(variableStepSize);
                        stepSize.setValue(variableStepSize);
                        dynamicScope.leave();
                    }
                }

                anyDiscards.setValue(new BooleanVariableFmi2Api(null, null, dynamicScope, null, MableAstFactory.newABoolLiteralExp(false)));

                // STEP ALL
                fmuInstancesToVariablesInArray.forEach((k, v) -> {
                    Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>> discard =
                            k.step(currentCommunicationTime, currentStepSize);

                    v.setValue(new DoubleExpressionValue(discard.getValue().getExp()));

                    PredicateFmi2Api didDiscard = new PredicateFmi2Api(discard.getKey().getExp()).not();

                    dynamicScope.enterIf(didDiscard);
                    {
                        builder.getLogger()
                                .debug("## FMU: '%s' DISCARDED step at sim-time: %f for step-size: %f and proposed sim-time: %.15f", k.getName(),
                                        currentCommunicationTime, currentStepSize,
                                        new VariableFmi2Api<>(null, discard.getValue().getType(), dynamicScope, dynamicScope, null,
                                                discard.getValue().getExp()));
                        anyDiscards.setValue(
                                new BooleanVariableFmi2Api(null, null, dynamicScope, null, anyDiscards.toPredicate().or(didDiscard).getExp()));
                        dynamicScope.leave();
                    }
                });

                // GET ALL LINKED OUTPUTS INCLUDING LOGGING OUTPUTS
                Map<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Object>>> portsToShare = portsToGet.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getKey().get(entry.getValue().toArray(String[]::new))));

                // CONVERGENCE
                if (jacobianStepConfig.stabilisation) {
                    // For each instance ->
                    //      For each retrieved variable
                    //          compare with previous in terms of convergence
                    //  If all converge, set retrieved values and continue
                    //  else reset to previous state, set retrieved values and continue
                    List<BooleanVariableFmi2Api> convergenceVariables = new ArrayList<>();
                    for (Map.Entry<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Object>>> comptoPortAndVariable : portsToShare
                            .entrySet()) {
                        List<BooleanVariableFmi2Api> converged = new ArrayList<>();
                        List<Map.Entry<PortFmi2Api, VariableFmi2Api<Object>>> entries = comptoPortAndVariable.getValue().entrySet().stream()
                                .filter(x -> x.getKey().scalarVariable.type.type == ModelDescription.Types.Real).collect(Collectors.toList());

                        for (Map.Entry<PortFmi2Api, VariableFmi2Api<Object>> entry : entries) {
                            VariableFmi2Api oldVariable = entry.getKey().getSharedAsVariable();
                            VariableFmi2Api<Object> newVariable = entry.getValue();
                            BooleanVariableFmi2Api isClose = dynamicScope.store("isClose", false);
                            isClose.setValue(math.checkConvergence(oldVariable, newVariable, absTol, relTol));
                            dynamicScope.enterIf(isClose.toPredicate().not());
                            {
                                builder.getLogger().debug("Unstable signal %s = %.15E at time: %.15E", entry.getKey().getLogScalarVariableName(),
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

                    dynamicScope.enterIf(convergenceReached.toPredicate().not()).enterThen();
                    {
                        fmuStates.forEach(Fmi2Builder.StateVariable::set);
                        stabilisation_loop.decrement();
                        dynamicScope.leave();
                    }
                    portsToShare.forEach(ComponentVariableFmi2Api::share);
                    stabilisationScope.leave();
                }


                if (!jacobianStepConfig.stabilisation) {
                    portsToShare.forEach(ComponentVariableFmi2Api::share);
                }

                // Discard
                PredicateFmi2Api anyDiscardsPred = anyDiscards.toPredicate();
                IfMaBlScope discardScope = dynamicScope.enterIf(anyDiscardsPred);
                {

                    // rollback FMUs
                    fmuStates.forEach(Fmi2Builder.StateVariable::set);

                    // set step-size to lowest
                    currentStepSize.setValue(math.minRealFromArray(fmuCommunicationPoints).toMath().subtraction(currentCommunicationTime));

                    builder.getLogger().debug("## Discard occurred! step-size reduced to: %f", currentStepSize);

                    dynamicScope.leave();
                }

                discardScope.enterElse();
                {


                    // Update currentCommunicationTime
                    currentCommunicationTime.setValue(currentCommunicationTime.toMath().addition(currentStepSize));
                    currentStepSize.setValue(stepSize);

                    // Call log
                    dataWriterInstance.log(currentCommunicationTime);

                    dynamicScope.leave();
                }

                //TODO: When should validate happen?
                if (algorithm == StepAlgorithm.VARIABLESTEP) {
                    //Validate step
                    PredicateFmi2Api validStepPred = Objects.requireNonNull(variableStepInstance).validateStepSize(
                            new DoubleVariableFmi2Api(null, null, dynamicScope, null,
                                    currentCommunicationTime.toMath().addition(currentStepSize).getExp())).toPredicate().not();
                    dynamicScope.enterIf(validStepPred);
                    {
                        dynamicScope.leave();
                    }
                }

                scopeFmi2Api.activate();
            }

            ABlockStm algorithm = (ABlockStm) builder.buildRaw();

            algorithm.apply(new ToParExp());
            System.out.println(PrettyPrinter.print(algorithm));

            return algorithm.getBody();
        } catch (Exception e) {
            throw new ExpandException("Internal error: ", e);
        }
    }

    @Override
    public boolean requireConfig() {
        return true;
    }

    @Override
    public IPluginConfiguration parseConfig(InputStream is) throws IOException {
        return (new ObjectMapper().readValue(is, JacobianStepConfig.class));
    }

    @Override
    public AImportedModuleCompilationUnit getDeclaredImportUnit() {
        AImportedModuleCompilationUnit unit = new AImportedModuleCompilationUnit();
        unit.setImports(Stream.of("FMI2", "TypeConverter", "Math", "Logger", "DataWriter", "ArrayUtil").map(MableAstFactory::newAIdentifier)
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

    private enum algorithmEnum {
        variableStep,
        fixedStep
    }
}



