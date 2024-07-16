package org.intocps.maestro.interpreter.extensions;

import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.Value;

import java.io.InputStream;

public abstract  class BaseLifecycleHandler implements IValueLifecycleHandler {
    @Override
    public void destroy(Value value) {

    }

    @Override
    public InputStream getMablModule() {
        return null;
    }
}