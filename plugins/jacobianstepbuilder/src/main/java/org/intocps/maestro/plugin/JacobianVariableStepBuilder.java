package org.intocps.maestro.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.StringEscapeUtils;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.PortFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.PredicateFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.VariableStep;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IfMaBlScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.StringVariableFmi2Api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class JacobianVariableStepBuilder {

  public static   class JacobianVariableStepContext{
      VariableStep variableStep;
      VariableStep.VariableStepInstance variableStepInstance = null;
    }

    public static JacobianVariableStepContext init(JacobianInternalBuilder.BaseJacobianContext ctxt, JacobianStepConfig jacobianStepConfig, DynamicActiveBuilderScope dynamicScope, MablApiBuilder builder, Map<StringVariableFmi2Api, ComponentVariableFmi2Api> fmuNamesToFmuInstances) throws JsonProcessingException {
        JacobianVariableStepContext varCtxt = new JacobianVariableStepContext();
        // Initialize variable step module
        List<PortFmi2Api> ports = ctxt.fmuInstances.values().stream().map(ComponentVariableFmi2Api::getPorts).flatMap(Collection::stream)
                .filter(p -> jacobianStepConfig.getVariablesOfInterest().stream()
                        .anyMatch(p1 -> p1.equals(p.getMultiModelScalarVariableName()))).collect(Collectors.toList());

        varCtxt.variableStep = builder.getVariableStep(dynamicScope.store("variable_step_config",
                StringEscapeUtils.escapeJava((new ObjectMapper()).writeValueAsString(jacobianStepConfig.stepAlgorithm))));
        varCtxt.variableStepInstance = varCtxt.variableStep.createVariableStepInstanceInstance();
        varCtxt.variableStepInstance.initialize(fmuNamesToFmuInstances, ports, ctxt.endTime);

        return varCtxt;
    }

    public static void updateCurrentStepTiming(JacobianInternalBuilder.BaseJacobianContext ctxt, JacobianVariableStepContext varStep, DynamicActiveBuilderScope dynamicScope, BooleanVariableFmi2Api anyDiscards){
        // Get variable step
        DoubleVariableFmi2Api variableStepSize = dynamicScope.store("variable_step_size", 0.0);
        dynamicScope.enterIf(anyDiscards.toPredicate().not());
        {
            variableStepSize.setValue(varStep.variableStepInstance.getStepSize(ctxt.currentCommunicationTime));
            ctxt.currentStepSize.setValue(variableStepSize);
            ctxt.stepSize.setValue(variableStepSize);
            dynamicScope.leave();
        }
    }

    public static void step(JacobianInternalBuilder.BaseJacobianContext ctxt, JacobianVariableStepContext varStep, DynamicActiveBuilderScope dynamicScope, MablApiBuilder builder, BooleanVariableFmi2Api allFMUsSupportGetState, List<FmiBuilder.StateVariable<PStm>> fmuStates, BooleanVariableFmi2Api anyDiscards){           // Validate step
        PredicateFmi2Api notValidStepPred = Objects.requireNonNull(varStep.variableStepInstance).validateStepSize(
                        new DoubleVariableFmi2Api(null, null, dynamicScope, null,
                                ctxt.currentCommunicationTime.toMath().addition(ctxt.currentStepSize).getExp()), allFMUsSupportGetState).toPredicate()
                .not();

        BooleanVariableFmi2Api hasReducedStepSize = new BooleanVariableFmi2Api(null, null, dynamicScope, null,
                Objects.requireNonNull(varStep.variableStepInstance).hasReducedStepsize().getReferenceExp());

        dynamicScope.enterIf(notValidStepPred);
        {
            IfMaBlScope reducedStepSizeScope = dynamicScope.enterIf(hasReducedStepSize.toPredicate());
            {
                // Rollback FMUs
                fmuStates.forEach(FmiBuilder.StateVariable::set);

                // Set step-size to suggested size
                ctxt.currentStepSize.setValue(Objects.requireNonNull(varStep.variableStepInstance).getReducedStepSize());

                builder.getLogger()
                        .debug("## Invalid variable step-size! FMUs are rolled back and step-size reduced to: %f", ctxt.currentStepSize);

                anyDiscards.setValue(
                        new BooleanVariableFmi2Api(null, null, dynamicScope, null, anyDiscards.toPredicate().not().getExp()));

                dynamicScope.leave();
            }
            reducedStepSizeScope.enterElse();
            {
                builder.getLogger()
                        .debug("## The step could not be validated by the constraint at time %f. Continue nevertheless " + "with" +
                                " next simulation step!", ctxt.currentCommunicationTime);
                dynamicScope.leave();
            }

            dynamicScope.leave();
        }}
}
