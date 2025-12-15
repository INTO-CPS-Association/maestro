package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.framework.fmi2.api.mabl.PortFmi3Api.PortFilters.isClockTimeBased;
import static org.intocps.maestro.plugin.JacobianStepBuilder.ARG_INDEX.*;

class JacobianInternalBuilder {

    public static class BaseJacobianContext {
        DoubleVariableFmi2Api externalStepSize;
        DoubleVariableFmi2Api currentStepSize;
        DoubleVariableFmi2Api stepSize;
        ArrayVariableFmi2Api<DoubleVariableFmi2Api> stepSizes;
        DoubleVariableFmi2Api externalStartTime;
        DoubleVariableFmi2Api currentCommunicationTime;
        DoubleVariableFmi2Api externalEndTime;
        BooleanVariableFmi2Api externalEndTimeDefined;
        DoubleVariableFmi2Api endTime;
        Map<String, ComponentVariableFmi2Api> fmuInstances;
    }

    public static class Jacobian3Context extends BaseJacobianContext {
        Map<String, InstanceVariableFmi3Api> fmu3Instances;
        BooleanVariableFmi2Api terminateSimulation;
    }

    static <T extends BaseJacobianContext> T buildBaseCtxt(T ctxt, IndexedFunctionDeclarationContainer<JacobianStepBuilder.ARG_INDEX> selectedFun,
                                                           List<FmiBuilder.Variable<PStm, ?>> formalArguments, DynamicActiveBuilderScope dynamicScope) {
        // Convert raw MaBL to API
        ctxt.externalStepSize = (DoubleVariableFmi2Api) selectedFun.getArgumentValue(formalArguments, STEP_SIZE);

        ctxt.currentStepSize = dynamicScope.store("jac_current_step_size", ctxt.externalStepSize);
        ctxt.stepSize = dynamicScope.store("jac_step_size", ctxt.externalStepSize);

        ctxt.externalStartTime = (DoubleVariableFmi2Api) selectedFun.getArgumentValue(formalArguments, START_TIME);
        dynamicScope.addTransferAs(ctxt.externalStartTime.getName());
        ctxt.currentCommunicationTime = dynamicScope.store("jac_current_communication_point", ctxt.externalStartTime);
        ctxt.externalEndTime = (DoubleVariableFmi2Api) selectedFun.getArgumentValue(formalArguments, END_TIME);
        ctxt.externalEndTimeDefined = (BooleanVariableFmi2Api) selectedFun.getArgumentValue(formalArguments, END_TIME_DEFINED);
        ctxt.endTime = dynamicScope.store("jac_end_time", ctxt.externalEndTime);

//        ctxt.currentStepSize.setValue(ctxt.externalStepSize);
//        ctxt.stepSize.setValue(ctxt.externalStepSize);
//        ctxt.currentCommunicationTime.setValue(ctxt.externalStartTime);
//        ctxt.endTime.setValue(ctxt.externalEndTime);

        // Get FMU instances - use LinkedHashMap to preserve added order
        ctxt.fmuInstances =
                ((List<ComponentVariableFmi2Api>) ((FmiBuilder.ArrayVariable) selectedFun.getArgumentValue(formalArguments, FMI2_INSTANCES)).items()).stream()
                        .collect(Collectors.toMap(ComponentVariableFmi2Api::getName, Function.identity(), (u, v) -> u, LinkedHashMap::new));

        return ctxt;

    }

    static BaseJacobianContext buildBaseCtxt(IndexedFunctionDeclarationContainer<JacobianStepBuilder.ARG_INDEX> selectedFun,
                                             List<FmiBuilder.Variable<PStm, ?>> formalArguments, DynamicActiveBuilderScope dynamicScope) {
        return buildBaseCtxt(new BaseJacobianContext(), selectedFun, formalArguments, dynamicScope);
    }

    static Jacobian3Context buildBaseFmi3Ctxt(MablApiBuilder builder, IndexedFunctionDeclarationContainer<JacobianStepBuilder.ARG_INDEX> selectedFun,
                                              List<FmiBuilder.Variable<PStm, ?>> formalArguments, DynamicActiveBuilderScope dynamicScope
    ) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        var ctxt = buildBaseCtxt(new Jacobian3Context(), selectedFun, formalArguments, dynamicScope);
        // Get FMU instances - use LinkedHashMap to preserve added order
        ctxt.fmu3Instances =
                ((List<InstanceVariableFmi3Api>) ((FmiBuilder.ArrayVariable) selectedFun.getArgumentValue(formalArguments, FMI3_INSTANCES)).items()).stream()
                        .collect(Collectors.toMap(InstanceVariableFmi3Api::getName, Function.identity(), (u, v) -> u, LinkedHashMap::new));

//        ctxt.fmu3Instances.entrySet().stream().map(inst->inst.getValue().)
        var totalTimeBasedClocks = Float.valueOf(
                ctxt.fmu3Instances.values().stream().flatMap(inst -> inst.getPorts().stream().filter(isClockTimeBased)).count()).intValue()+1/*we store the fixed step at 0*/;
        var totalTimeBasedClocksVar = dynamicScope.store("totalClockSizes", totalTimeBasedClocks);

       var stepSizesTmp = dynamicScope.createArray("stepSizes", DoubleVariableFmi2Api.class, totalTimeBasedClocksVar);

       var items = IntStream.range(0, totalTimeBasedClocks)
                .mapToObj(i -> new VariableFmi2Api<DoubleVariableFmi2Api>(stepSizesTmp.getDeclaringStm(),stepSizesTmp.getType(), stepSizesTmp.getDeclaredScope(), dynamicScope,
                        newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier("stepSizes")), newAIntLiteralExp(i)),
                        newAArrayIndexExp(newAIdentifierExp("stepSizes"), Collections.singletonList(newAIntLiteralExp(i))))).collect(Collectors.toList());

        ctxt.stepSizes = new ArrayVariableFmi2Api<>(stepSizesTmp.getDeclaringStm(), stepSizesTmp.getType(), stepSizesTmp.getDeclaredScope(), dynamicScope,
                stepSizesTmp.getDesignator(), stepSizesTmp.getReferenceExp(), items);
        ctxt.terminateSimulation = dynamicScope.store("terminateSimulation", false);

//        if(totalTimeBasedClocks>1) {
//            // preconfigure the step size to the shift only if we have clocks
//            int clockIndex = 1;
//            ctxt.stepSizes.items().getFirst().setValue(ctxt.stepSize);
//            for (var instance : ctxt.fmu3Instances.entrySet()) {
//                var clockTimePorts = instance.getValue().getPorts().stream().filter(isClockTimeBased).toList();
//                for (var clockPort : clockTimePorts) {
//                    ctxt.stepSizes.items().get(clockIndex++).setValue(instance.getValue().getClocksUtil().getShift(clockPort));
//                }
//            }
//
//            ctxt.currentStepSize.setValue(builder.getMathBuilder().minRealFromArray(ctxt.stepSizes));
//        }
        return ctxt;

    }
}
