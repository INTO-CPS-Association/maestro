package org.intocps.maestro.interpreter.extensions;

import com.spencerwi.either.Either;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.BooleanLogicValue;
import org.intocps.maestro.interpreter.values.Value;

import java.util.List;

@IValueLifecycleHandler.ValueLifecycle(name = "BooleanLogic")
public  class BooleanLogicLifecycleHandler extends BaseLifecycleHandler {
    @Override
    public Either<Exception, Value> instantiate(List<Value> args) {
        return Either.right(new BooleanLogicValue());
    }
}
