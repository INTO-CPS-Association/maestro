package org.intocps.maestro.interpreter.values.modeltransition;

import org.intocps.maestro.interpreter.values.*;

import java.util.HashMap;
import java.util.Map;

public class ModelTransitionValue extends ModuleValue {

    public ModelTransitionValue() { super(createMembers()); }

    private static Map<String, Value> createMembers() {
        Map<String, Value> members = new HashMap<>();

        members.put("transition", new FunctionValue.ExternalFunctionValue(fcargs -> {
            return new VoidValue();
        }));
        return members;
    }
}
