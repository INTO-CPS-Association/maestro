package org.intocps.maestro.plugin;

import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.RealTime;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.DoubleExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;

import java.util.List;
import java.util.Objects;

public class RealTimeSlowDownBuilder {

    public static class RealTimeSlowDownContext {
        RealTime realTimeModule = null;
        DoubleVariableFmi2Api realStartTime = null;
    }

    public static RealTimeSlowDownContext init(MablApiBuilder builder, List<String> imports) {
        RealTimeSlowDownContext ctsCtxt = new RealTimeSlowDownContext();
        ctsCtxt.realTimeModule = builder.getRealTimeModule();
        imports.add("RealTime");
        return ctsCtxt;
    }

    public static void setStartTime(RealTimeSlowDownContext ctsCtxt, DynamicActiveBuilderScope dynamicScope) {
        ctsCtxt.realStartTime = dynamicScope.store("real_start_time", 0.0);
        ctsCtxt.realStartTime.setValue(Objects.requireNonNull(ctsCtxt.realTimeModule).getRealTime());
    }

    public static void slowDown(RealTimeSlowDownContext ctsCtxt, DynamicActiveBuilderScope dynamicScope, JacobianInternalBuilder.BaseJacobianContext ctxt, MablApiBuilder builder) {
        DoubleVariableFmi2Api realStepTime = dynamicScope.store("real_step_time", 0.0);
        realStepTime.setValue(
                new DoubleExpressionValue(ctsCtxt.realTimeModule.getRealTime().toMath().subtraction(ctsCtxt.realStartTime.toMath()).getExp()));

        dynamicScope.enterIf(realStepTime.toMath().lessThan(ctxt.currentCommunicationTime.toMath().multiply(1000)));
        {
            DoubleVariableFmi2Api sleepTime = dynamicScope.store("sleep_time", 0.0);
            sleepTime.setValue(ctxt.currentCommunicationTime.toMath().multiply(1000).subtraction(realStepTime));
            builder.getLogger().debug("## Simulation is ahead of real time. Sleeping for: %f MS", sleepTime);
            ctsCtxt.realTimeModule.sleep(sleepTime);
            dynamicScope.leave();
        }
    }
}
