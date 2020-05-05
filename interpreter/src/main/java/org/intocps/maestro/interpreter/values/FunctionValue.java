package org.intocps.maestro.interpreter.values;

import org.intocps.maestro.interpreter.InterpreterException;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public abstract class FunctionValue extends Value {


    public Value evaluate(Value... args) throws InterpreterException {
        return this.evaluate(Arrays.asList(args));
    }

    public abstract Value evaluate(List<Value> args) throws InterpreterException;

    public static class ExternalFunctionValue extends FunctionValue {
        final Function<List<Value>, Value> function;

        public ExternalFunctionValue(Function<List<Value>, Value> function) throws InterpreterException {
            this.function = function;
        }

        @Override
        public Value evaluate(List<Value> args) throws InterpreterException {
            return function.apply(args);
        }
    }
}
