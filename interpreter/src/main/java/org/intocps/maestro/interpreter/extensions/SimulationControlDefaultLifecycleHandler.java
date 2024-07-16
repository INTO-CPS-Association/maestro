package org.intocps.maestro.interpreter.extensions;

import com.spencerwi.either.Either;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.simulationcontrol.SimulationControlValue;

import java.util.List;

@IValueLifecycleHandler.ValueLifecycle(name = "SimulationControl")
public  class SimulationControlDefaultLifecycleHandler extends BaseLifecycleHandler {
    @Override
    public Either<Exception, Value> instantiate(List<Value> args) {
        return Either.right(new SimulationControlValue());
    }
}