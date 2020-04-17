package org.intocps.maestro.interpreter.values;

import java.util.Map;

public class ModuleValue extends Value {

    final Map<String, Value> members;

    public ModuleValue(Map<String, Value> members) {
        this.members = members;
    }

    public Value lookup(String name) {
        return this.members.get(name);
    }


}
