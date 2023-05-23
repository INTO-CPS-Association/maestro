package org.intocps.maestro.interpreter.extensions;

import com.spencerwi.either.Either;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.RealTimeValue;
import org.intocps.maestro.interpreter.values.Value;

import java.util.List;

@IValueLifecycleHandler.ValueLifecycle(name = "RealTime")
public  class RealTimeLifecycleHandler extends BaseLifecycleHandler {
    @Override
    public Either<Exception, Value> instantiate(List<Value> args) {
        return Either.right(new RealTimeValue());
    }
}
