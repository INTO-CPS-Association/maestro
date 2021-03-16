package org.intocps.maestro.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.AModuleDeclaration;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.ToParExp;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.RelationVariable;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.*;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IfMaBlScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.values.BooleanExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.DoubleExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;

@SimulationFramework(framework = Framework.FMI2)
public class JacobianStepBuilder implements IMaestroExpansionPlugin {

    final static Logger logger = LoggerFactory.getLogger(JacobianStepBuilder.class);

    final AFunctionDeclaration fun = newAFunctionDeclaration(newAIdentifier("fixedStep"),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("stepSize")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("startTime")),
                    newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("endTime"))), newAVoidType());


    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return Stream.of(fun).collect(Collectors.toSet());
    }


    @Override
    public List<PStm> expand(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config,
            ISimulationEnvironment envIn, IErrorReporter errorReporter) throws ExpandException {

        logger.info("Unfolding with jacobian step: {}", declaredFunction.toString());
        JacobianStepConfig jacobianStepConfig = (JacobianStepConfig) config;

        if (!getDeclaredUnfoldFunctions().contains(declaredFunction)) {
            throw new ExpandException("Unknown function declaration");
        }
        AFunctionDeclaration selectedFun = fun;

        if (formalArguments == null || formalArguments.size() != selectedFun.getFormals().size()) {
            throw new ExpandException("Invalid args");
        }

        if (envIn == null) {
            throw new ExpandException("Simulation environment must not be null");
        }

        Fmi2SimulationEnvironment env = (Fmi2SimulationEnvironment) envIn;

        PExp stepSize = formalArguments.get(1).clone();
        PExp startTime = formalArguments.get(2).clone();
        PExp endTime = formalArguments.get(3).clone();
        if (declaredFunction.equals(fun)) {
            try {
                MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
                // Selected fun now matches funWithBuilder
                MablApiBuilder builder = new MablApiBuilder(settings, true);

                DynamicActiveBuilderScope dynamicScope = builder.getDynamicScope();
                MathBuilderFmi2Api math = builder.getMablToMablAPI().getMathBuilder();
                BooleanBuilderFmi2Api booleanLogic = builder.getMablToMablAPI().getBooleanBuilder();

                // Convert raw MaBL to API
                DoubleVariableFmi2Api externalStepSize = builder.getDoubleVariableFrom(stepSize);
                DoubleVariableFmi2Api stepSizeVar = dynamicScope.store("step_size", 0.0);
                stepSizeVar.setValue(externalStepSize);

                DoubleVariableFmi2Api fixedStepSize = dynamicScope.store("fixed_step_size", 0.0);
                fixedStepSize.setValue(externalStepSize);

                DoubleVariableFmi2Api externalStartTime = new DoubleVariableFmi2Api(null, null, null, null, startTime);
                DoubleVariableFmi2Api currentCommunicationTime = dynamicScope.store("fixed_current_communication_point", 0.0);
                currentCommunicationTime.setValue(externalStartTime);
                DoubleVariableFmi2Api externalEndTime = new DoubleVariableFmi2Api(null, null, null, null, endTime);
                DoubleVariableFmi2Api endTimeVar = dynamicScope.store("fixed_end_time", 0.0);
                endTimeVar.setValue(externalEndTime);

                // Import the external components into Fmi2API
                Map<String, ComponentVariableFmi2Api> fmuInstances =
                        FromMaBLToMaBLAPI.GetComponentVariablesFrom(builder, formalArguments.get(0), env);

                // Create bindings
                FromMaBLToMaBLAPI.CreateBindings(fmuInstances, env);

                // Create the logging
                DataWriter dataWriter = builder.getMablToMablAPI().getDataWriter();
                DataWriter.DataWriterInstance dataWriterInstance = dataWriter.createDataWriterInstance();
                dataWriterInstance.initialize(fmuInstances.values().stream().flatMap(x -> x.getVariablesToLog().stream()).collect(Collectors.toList()));


                // Create the iteration predicate
                PredicateFmi2Api loopPredicate = currentCommunicationTime.toMath().addition(stepSizeVar).lessThan(endTimeVar);


                // Get and share all variables related to outputs or logging.
                fmuInstances.forEach((x, y) -> {
                    List<RelationVariable> variablesToLog = env.getVariablesToLog(x);
                    y.share(y.get(variablesToLog.stream().map(var -> var.scalarVariable.getName()).toArray(String[]::new)));
                });

                // Initialize variable step module
                //                VariableStep variableStep = builder.getMablToMablAPI().getVariableStep();
                //                VariableStep.VariableStepInstance variableStepInstance = variableStep.createVariableStepInstanceInstance();
                //                variableStepInstance.initialize(fmuInstances.values(), new ArrayList<>(), endTimeVar);


                // Map FMUs to step-size array values

                ArrayVariableFmi2Api<Double> stepSizes = dynamicScope.store("stepSizes", new Double[fmuInstances.entrySet().size()]);
                Map<ComponentVariableFmi2Api, VariableFmi2Api<Double>> fmuInstancesToArrayVaraible = new HashMap<>();

                boolean everyFMUSupportsGetState = true;
                int indexer = 0;
                for(Map.Entry<String, ComponentVariableFmi2Api> entry : fmuInstances.entrySet()){
                    everyFMUSupportsGetState = entry.getValue().getModelDescription().getCanGetAndSetFmustate() && everyFMUSupportsGetState;
                    fmuInstancesToArrayVaraible.put(entry.getValue(), stepSizes.items().get(indexer));
                    indexer++;
                }
                BooleanVariableFmi2Api canGetFMUStates = dynamicScope.store("canGetFMUStates", everyFMUSupportsGetState);
                List<Fmi2Builder.StateVariable<PStm>> fmuStates = new ArrayList<>();

                ScopeFmi2Api scopeFmi2Api = dynamicScope.enterWhile(loopPredicate);
                {

                    if(everyFMUSupportsGetState){
                        for (Map.Entry<String, ComponentVariableFmi2Api> entry : fmuInstances.entrySet()) {
                            fmuStates.add(entry.getValue().getState());
                        }
                    }

                    // SET ALL LINKED VARIABLES
                    // This has to be carried out regardless of stabilisation or not.
                    fmuInstances.forEach((x, y) -> y.setLinked());

                    BooleanVariableFmi2Api anyDiscards = dynamicScope.store("anyDiscards", false);

                    // STEP ALL
                    fmuInstances.forEach((x, y) -> {
                                Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>> discard = y.step(currentCommunicationTime,
                                        stepSizeVar);
                    });
                    fmuInstancesToArrayVaraible.forEach((k,v) -> {
                        Map.Entry<Fmi2Builder.BoolVariable<PStm>, Fmi2Builder.DoubleVariable<PStm>> discard = k.step(currentCommunicationTime, stepSizeVar);

                        v.setValue(new DoubleExpressionValue(discard.getValue().getExp()));

                        PredicateFmi2Api didDiscard = new PredicateFmi2Api(discard.getKey().getExp()).not();

                        dynamicScope.enterIf(didDiscard);
                        {
                            anyDiscards.setValue(new BooleanVariableFmi2Api(null, null, dynamicScope, null,
                                    anyDiscards.toPredicate().or(didDiscard).getExp()));
                            dynamicScope.leave();
                        }
                    });

                    // FMU get state not supported
                    dynamicScope.enterIf(canGetFMUStates.toPredicate());
                    {
                        //TODO: What should be done?
                        dynamicScope.leave();
                    }

                    // Discard
                    PredicateFmi2Api anyDiscardsPred = anyDiscards.toPredicate();
                    IfMaBlScope discardScope = dynamicScope.enterIf(anyDiscardsPred);
                    {
                        DoubleVariableFmi2Api decreasedStepTime = dynamicScope.store("decreasedStepTime", 0.0);
                        decreasedStepTime.setValue(builder.getMathBuilder().minRealFromArray(stepSizes));

                        //rollback FMU
                        fmuStates.forEach(Fmi2Builder.StateVariable::set);

                        stepSizeVar.setValue(decreasedStepTime.toMath().subtraction(currentCommunicationTime));

                        dynamicScope.leave();
                    }

                    discardScope.enterElse();
                    {
                        stepSizeVar.setValue(fixedStepSize);

                        // GET ALL LINKED OUTPUTS INCLUDING LOGGING OUTPUTS
                        Map<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Object>>> retrievedValues =
                                fmuInstances.entrySet().stream().collect(Collectors.toMap(entry -> entry.getValue(), entry -> {
                                    List<RelationVariable> variablesToLog = env.getVariablesToLog(entry.getKey());
                                    String[] variablesToGet = variablesToLog.stream().map(var -> var.scalarVariable.getName()).toArray(String[]::new);
                                    return entry.getValue().get(variablesToGet);
                                }));


                        retrievedValues.forEach((k, v) -> k.share(v));

                        // Update currentCommunicationTime
                        currentCommunicationTime.setValue(currentCommunicationTime.toMath().addition(stepSizeVar));

                        // Call log
                        dataWriterInstance.log(currentCommunicationTime);

                        dynamicScope.leave();
                    }
                }

                ABlockStm algorithm = (ABlockStm) builder.buildRaw();

                algorithm.apply(new ToParExp());
                System.out.println(PrettyPrinter.print(algorithm));

                return algorithm.getBody();
            } catch (Exception e) {
                throw new ExpandException("Internal error: ", e);
            }
        } else {
            throw new ExpandException("Unknown function");
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
        unit.setImports(
                Stream.of("FMI2", "TypeConverter", "Math", "Logger", "DataWriter", "ArrayUtil", "VariableStep").map(MableAstFactory::newAIdentifier)
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


}
