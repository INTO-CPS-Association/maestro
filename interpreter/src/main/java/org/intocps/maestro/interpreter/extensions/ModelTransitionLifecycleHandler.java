package org.intocps.maestro.interpreter.extensions;

import com.spencerwi.either.Either;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.StringValue;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.modeltransition.ModelTransitionValue;

import java.util.List;

@IValueLifecycleHandler.ValueLifecycle(name = "ModelTransition")
public  class ModelTransitionLifecycleHandler extends BaseLifecycleHandler {

    @Override
    public Either<Exception, Value> instantiate(List<Value> args) {
        String transitionPath = ((StringValue) args.get(0)).getValue();
        return Either.right(new ModelTransitionValue(transitionPath));
    }
}
