package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.BooleanBuilderFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.MathBuilderFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.PortFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StabilisationBuilder {

    public static void step(StabilisationContext stabilisationCtxt, DynamicActiveBuilderScope dynamicScope) {
        stabilisationCtxt.stabilisation_loop.setValue(stabilisationCtxt.stabilisation_loop_max_iterations);
        stabilisationCtxt.convergenceReached.setValue(
                new BooleanVariableFmi2Api(null, null, dynamicScope, null, MableAstFactory.newABoolLiteralExp(false)));
        stabilisationCtxt.stabilisationScope = dynamicScope.enterWhile(
                stabilisationCtxt.     convergenceReached.toPredicate().not().and(stabilisationCtxt.stabilisation_loop.toMath().greaterThan(IntExpressionValue.of(0))));
    }

    public static void convergence(DynamicActiveBuilderScope dynamicScope, Map<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Object>>> componentsToPortsWithValues, StabilisationContext stabilisationCtxt, JacobianInternalBuilder.BaseJacobianContext ctxt, MablApiBuilder builder, MathBuilderFmi2Api math, BooleanBuilderFmi2Api booleanLogic, List<FmiBuilder.StateVariable<PStm>> fmuStates) {
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
                isClose.setValue(math.checkConvergence(oldVariable, newVariable, stabilisationCtxt.absTol,stabilisationCtxt. relTol));
                dynamicScope.enterIf(isClose.toPredicate().not());
                {
                    builder.getLogger()
                            .debug("Unstable signal %s = %.15E at time: %.15E", entry.getKey().getMultiModelScalarVariableName(),
                                    entry.getValue(), ctxt.currentCommunicationTime);
                    dynamicScope.leave();
                }
                converged.add(isClose);
            }
            convergenceVariables.addAll(converged);
        }

        if (stabilisationCtxt.convergenceReached != null) {
            stabilisationCtxt.convergenceReached.setValue(booleanLogic.allTrue("convergence", convergenceVariables));
        } else {
            throw new RuntimeException("NO STABILISATION LOOP FOUND");
        }
        // Rollback
        dynamicScope.enterIf(stabilisationCtxt.convergenceReached.toPredicate().not()).enterThen();
        {
            fmuStates.forEach(FmiBuilder.StateVariable::set);
            stabilisationCtxt.stabilisation_loop.decrement();
            dynamicScope.leave();
        }
        componentsToPortsWithValues.forEach(ComponentVariableFmi2Api::share);
        stabilisationCtxt.stabilisationScope.leave();
    }

    public static class StabilisationContext {
        ScopeFmi2Api stabilisationScope = null;
        IntVariableFmi2Api stabilisation_loop = null;
        BooleanVariableFmi2Api convergenceReached = null;
        DoubleVariableFmi2Api absTol = null, relTol = null;
        IntVariableFmi2Api stabilisation_loop_max_iterations = null;
    }

    public static StabilisationContext init(DynamicActiveBuilderScope dynamicScope, JacobianStepConfig jacobianStepConfig) {
        StabilisationContext stabilisationCtxt = new StabilisationContext();
        stabilisationCtxt.absTol = dynamicScope.store("absolute_tolerance", jacobianStepConfig.absoluteTolerance);
        stabilisationCtxt.relTol = dynamicScope.store("relative_tolerance", jacobianStepConfig.relativeTolerance);
        stabilisationCtxt.stabilisation_loop_max_iterations =
                dynamicScope.store("stabilisation_loop_max_iterations", jacobianStepConfig.stabilisationLoopMaxIterations);
        stabilisationCtxt.stabilisation_loop = dynamicScope.store("stabilisation_loop", stabilisationCtxt.stabilisation_loop_max_iterations);
        stabilisationCtxt.convergenceReached = dynamicScope.store("has_converged", false);
        return stabilisationCtxt;
    }
}
