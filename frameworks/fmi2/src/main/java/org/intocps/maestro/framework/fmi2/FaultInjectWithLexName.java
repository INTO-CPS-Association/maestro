package org.intocps.maestro.framework.fmi2;

import org.intocps.maestro.framework.core.FaultInject;

public class FaultInjectWithLexName extends FaultInject {
    public String lexName;

    public FaultInjectWithLexName(String constraintId, String lexName) {
        super(constraintId);
        this.lexName = lexName;
    }
}
