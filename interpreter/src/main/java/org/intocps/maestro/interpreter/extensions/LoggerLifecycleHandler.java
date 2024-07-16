package org.intocps.maestro.interpreter.extensions;

import com.spencerwi.either.Either;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.LoggerValue;
import org.intocps.maestro.interpreter.values.Value;

import java.util.List;

@IValueLifecycleHandler.ValueLifecycle(name = "Logger")
public  class LoggerLifecycleHandler extends BaseLifecycleHandler {
    @Override
    public Either<Exception, Value> instantiate(List<Value> args) {
        return Either.right(new LoggerValue());
    }
}