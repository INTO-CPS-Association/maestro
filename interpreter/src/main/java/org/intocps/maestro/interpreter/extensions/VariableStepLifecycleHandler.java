package org.intocps.maestro.interpreter.extensions;

import com.spencerwi.either.Either;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.StringValue;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.variablestep.VariableStepValue;

import java.util.List;
import java.util.Objects;

@IValueLifecycleHandler.ValueLifecycle(name = "VariableStep")
public  class VariableStepLifecycleHandler extends BaseLifecycleHandler {
    @Override
    public Either<Exception, Value> instantiate(List<Value> args) {
        if (args == null || args.isEmpty()) {
            return Either.left(new AnalysisException("No values passed"));
        }

        if (args.stream().anyMatch(Objects::isNull)) {
            return Either.left(new AnalysisException("Argument list contains null values"));
        }

        String config = ((StringValue) args.get(0).deref()).getValue();
        return Either.right(new VariableStepValue(config));
    }
}