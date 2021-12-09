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

/**
 *  Singleton store that keeps map of transition names and values
 */
class ModelTransitionStore {
    private static ModelTransitionStore instance = null;
    private Map<String, Value> values = null;

    private ModelTransitionStore() {
        values = new HashMap<>();
    }

    public static ModelTransitionStore getInstance() {
        if (instance == null) {
            instance = new ModelTransitionStore();
        }
        return instance;
    }

    public void put(String id, Value val) {
       values.put(id, val);
    }

    public Value get(String id) {
        return values.get(id);
    }
}

public class ModelTransitionValue extends ModuleValue {

    public ModelTransitionValue() {super(createMembers(ModelTransitionStore.getInstance()));
    }

    private static Map<String, Value> createMembers(ModelTransitionStore instance) {
        Map<String, Value> members = new HashMap<>();

        members.put("setFMU", new FunctionValue.ExternalFunctionValue(fcargs -> {
            List<Value> args = fcargs.stream().map(Value::deref).collect(Collectors.toList());
            checkArgLength(args, 2);

            Value idVal = args.get(0);
            Value fmuVal = args.get(1);

            String id = ((StringValue) idVal).getValue();
            FmuValue fmu = (FmuValue) fmuVal;

            instance.put(id, fmuVal);
            return new VoidValue();
        }));

        members.put("getFMU", new FunctionValue.ExternalFunctionValue(fcargs -> {
            List<Value> args = fcargs.stream().map(Value::deref).collect(Collectors.toList());
            checkArgLength(args, 1);

            Value idVal = args.get(0);
            String id = ((StringValue) idVal).getValue();

            return instance.get(id);
        }));

        members.put("setFMUInstance", new FunctionValue.ExternalFunctionValue(fcargs -> {
            List<Value> args = fcargs.stream().map(Value::deref).collect(Collectors.toList());
            checkArgLength(args, 2);

            Value idVal = args.get(0);
            Value fmuInstVal = args.get(1);

            String id = ((StringValue) idVal).getValue();
            FmuComponentValue fmuInst = (FmuComponentValue) fmuInstVal;

            instance.put(id, fmuInst);
            return new VoidValue();
        }));

        members.put("getFMUInstance", new FunctionValue.ExternalFunctionValue(fcargs -> {
            List<Value> args = fcargs.stream().map(Value::deref).collect(Collectors.toList());
            checkArgLength(args, 1);

            Value idVal = args.get(0);
            String id = ((StringValue) idVal).getValue();

            return instance.get(id);
        }));

        members.put("transition", new FunctionValue.ExternalFunctionValue(fcargs -> {
            throw new InterpreterTransitionException("mt2.mabl");
        }));
        return members;
    }
}
