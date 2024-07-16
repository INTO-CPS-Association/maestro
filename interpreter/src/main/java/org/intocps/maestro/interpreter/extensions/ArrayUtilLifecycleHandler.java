package org.intocps.maestro.interpreter.extensions;

import com.spencerwi.either.Either;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.utilities.ArrayUtilValue;

import java.util.List;

@IValueLifecycleHandler.ValueLifecycle(name = "ArrayUtil")
public  class ArrayUtilLifecycleHandler extends BaseLifecycleHandler {
    @Override
    public Either<Exception, Value> instantiate(List<Value> args) {
        return Either.right(new ArrayUtilValue());
    }
}