package org.intocps.maestro.interpreter.values;

import java.util.Map;

public abstract class ExternalModuleValue<T> extends ModuleValue {

    final T module;

    public ExternalModuleValue(Map<String, Value> members, T module) {
        super(members);
        this.module = module;
    }

    public T getModule() {
        return module;
    }
}
