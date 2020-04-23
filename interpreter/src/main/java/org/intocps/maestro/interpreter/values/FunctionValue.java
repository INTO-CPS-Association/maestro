package org.intocps.maestro.interpreter.values;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public abstract class FunctionValue extends Value {


    public Value evaluate(Value... args) {
        return this.evaluate(Arrays.asList(args));
    }

    public abstract Value evaluate(List<Value> args);

    public static class ExternalFunctionValue extends FunctionValue {
        final Function<List<Value>, Value> function;

        public ExternalFunctionValue(Function<List<Value>, Value> function) {
            this.function = function;
        }

        @Override
        public Value evaluate(List<Value> args) {
            return function.apply(args);
        }
    }
}
