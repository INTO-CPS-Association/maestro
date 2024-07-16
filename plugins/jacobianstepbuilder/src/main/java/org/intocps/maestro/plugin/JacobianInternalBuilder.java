package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.intocps.maestro.plugin.JacobianStepBuilder.ARG_INDEX.*;

class JacobianInternalBuilder {

    public static class BaseJacobianContext {
        DoubleVariableFmi2Api externalStepSize;
        DoubleVariableFmi2Api currentStepSize;
        DoubleVariableFmi2Api stepSize;
        DoubleVariableFmi2Api externalStartTime;
        DoubleVariableFmi2Api currentCommunicationTime;
        DoubleVariableFmi2Api externalEndTime;
        BooleanVariableFmi2Api externalEndTimeDefined;
        DoubleVariableFmi2Api endTime;
        Map<String, ComponentVariableFmi2Api> fmuInstances;
    }
    static BaseJacobianContext buildBaseCtxt(IndexedFunctionDeclarationContainer<JacobianStepBuilder.ARG_INDEX> selectedFun, List<FmiBuilder.Variable<PStm, ?>> formalArguments, DynamicActiveBuilderScope dynamicScope) {
        BaseJacobianContext ctxt = new BaseJacobianContext();
        // Convert raw MaBL to API
        ctxt.externalStepSize = (DoubleVariableFmi2Api) selectedFun.getArgumentValue(formalArguments, STEP_SIZE);
        ctxt.currentStepSize = dynamicScope.store("jac_current_step_size", 0.0);
        ctxt.stepSize = dynamicScope.store("jac_step_size", 0.0);
        ctxt.externalStartTime = (DoubleVariableFmi2Api) selectedFun.getArgumentValue(formalArguments, START_TIME);
        dynamicScope.addTransferAs(ctxt.externalStartTime.getName());
        ctxt.currentCommunicationTime = dynamicScope.store("jac_current_communication_point", 0.0);
        ctxt.externalEndTime = (DoubleVariableFmi2Api) selectedFun.getArgumentValue(formalArguments, END_TIME);
        ctxt.externalEndTimeDefined = (BooleanVariableFmi2Api) selectedFun.getArgumentValue(formalArguments, END_TIME_DEFINED);
        ctxt.endTime = dynamicScope.store("jac_end_time", 0.0);

        ctxt.currentStepSize.setValue(ctxt.externalStepSize);
        ctxt.stepSize.setValue(ctxt.externalStepSize);
        ctxt.currentCommunicationTime.setValue(ctxt.externalStartTime);
        ctxt.endTime.setValue(ctxt.externalEndTime);

        // Get FMU instances - use LinkedHashMap to preserve added order
        ctxt.fmuInstances =
                ((List<ComponentVariableFmi2Api>) ((FmiBuilder.ArrayVariable) selectedFun.getArgumentValue(formalArguments, FMI2_INSTANCES)).items()).stream()
                        .collect(Collectors.toMap(ComponentVariableFmi2Api::getName, Function.identity(), (u, v) -> u, LinkedHashMap::new));

        return ctxt;

    }
}
