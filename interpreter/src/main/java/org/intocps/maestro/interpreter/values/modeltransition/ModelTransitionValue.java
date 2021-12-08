package org.intocps.maestro.interpreter.values.modeltransition;

import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.InterpreterTransitionException;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.fmi.FmuComponentValue;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModelTransitionValue extends ModuleValue {

    private static Map<String, Value> values = null;

    public ModelTransitionValue() {
        super(createMembers());
        if (values == null) {
            values = new HashMap<>();
        }
    }

    private static Map<String, Value> createMembers() {
        Map<String, Value> members = new HashMap<>();

        members.put("setFMU", new FunctionValue.ExternalFunctionValue(fcargs -> {
            List<Value> args = fcargs.stream().map(Value::deref).collect(Collectors.toList());
            checkArgLength(args, 2);

            Value idVal = args.get(0);
            Value fmuVal = args.get(1);

            String id = ((StringValue) idVal).getValue();
            FmuValue fmu = (FmuValue) fmuVal;

            values.put(id, fmuVal);
            return new VoidValue();
        }));

        members.put("getFMU", new FunctionValue.ExternalFunctionValue(fcargs -> {
            List<Value> args = fcargs.stream().map(Value::deref).collect(Collectors.toList());
            checkArgLength(args, 1);

            Value idVal = args.get(0);
            String id = ((StringValue) idVal).getValue();

            return values.get(id);
        }));

        members.put("setFMUInstance", new FunctionValue.ExternalFunctionValue(fcargs -> {
            List<Value> args = fcargs.stream().map(Value::deref).collect(Collectors.toList());
            checkArgLength(args, 2);

            Value idVal = args.get(0);
            Value fmuInstVal = args.get(1);

            String id = ((StringValue) idVal).getValue();
            FmuComponentValue fmuInst = (FmuComponentValue) fmuInstVal;

            values.put(id, fmuInst);
            return new VoidValue();
        }));

        members.put("getFMUInstance", new FunctionValue.ExternalFunctionValue(fcargs -> {
            List<Value> args = fcargs.stream().map(Value::deref).collect(Collectors.toList());
            checkArgLength(args, 1);

            Value idVal = args.get(0);
            String id = ((StringValue) idVal).getValue();

            return values.get(id);
        }));

        members.put("transition", new FunctionValue.ExternalFunctionValue(fcargs -> {
            throw new InterpreterTransitionException("mt2.mabl");
        }));
        return members;
    }
}
