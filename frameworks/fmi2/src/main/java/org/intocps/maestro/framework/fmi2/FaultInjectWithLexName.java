package org.intocps.maestro.framework.fmi2;

public class FaultInjectWithLexName extends FaultInject {
    public String lexName;

    public FaultInjectWithLexName(String constraintId, String lexName) {
        super(constraintId);
        this.lexName = lexName;
    }
}
