package org.intocps.maestro.Fmi2AMaBLBuilder.statements;

import org.intocps.maestro.ast.node.PStm;

public class LabelStatement implements AMaBLStatement {
    public String labelName;

    @Override
    public boolean isMeta() {
        return true;
    }

    @Override
    public PStm getStatement() {
        return null;
    }
}
