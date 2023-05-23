package org.intocps.maestro.interpreter.values;

import org.intocps.maestro.interpreter.InterpreterException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConsolePrinterValue extends ModuleValue {
    public ConsolePrinterValue() {
        super(createMembers());
    }

    private static Object[] getValues(List<Value> values) {
        return values.stream().map(Value::deref).map(v -> {

            if (v instanceof RealValue || v instanceof FloatValue) {
                return ((NumericValue) v).realValue();
            } else if (v instanceof UnsignedIntegerValue) {
                return ((UnsignedIntegerValue) v).getValue();
            } else if (v instanceof BooleanValue) {
                return ((BooleanValue) v).getValue();
            } else if (v instanceof LongValue) {
                return ((LongValue) v).getValue();
            } else if (v instanceof IntegerValue || v instanceof ByteValue || v instanceof ShortValue) {
                return ((NumericValue) v).intValue();
            } else if (v instanceof StringValue) {
                return ((StringValue) v).getValue();
            } else {
                return v.toString();
            }

        }).toArray();
    }

    private static Map<String, Value> createMembers() {
        Map<String, Value> componentMembers = new HashMap<>();

        componentMembers.put("print", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgs(fcargs);
            String formattedMsg = String.format(((StringValue) fcargs.get(0).deref()).getValue(),
                    getValues(fcargs.stream().skip(1).collect(Collectors.toList())));
            System.out.print(formattedMsg);
            return new VoidValue();
        }));

        componentMembers.put("println", new FunctionValue.ExternalFunctionValue(fcargs -> {
            checkArgs(fcargs);
            String formattedMsg = String.format(((StringValue) fcargs.get(0).deref()).getValue(),
                    getValues(fcargs.stream().skip(1).collect(Collectors.toList())));
            System.out.println(formattedMsg);
            return new VoidValue();
        }));
        return componentMembers;
    }

    private static void checkArgs(List<Value> args) {
        if (args == null) {
            throw new InterpreterException("No values passed");
        }

        if (args.stream().anyMatch(Objects::isNull)) {
            throw new InterpreterException("Argument list contains null values");
        }
    }
}
