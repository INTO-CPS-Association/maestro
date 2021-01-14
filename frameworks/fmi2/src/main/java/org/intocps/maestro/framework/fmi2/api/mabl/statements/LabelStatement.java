package org.intocps.maestro.framework.fmi2.api.mabl.statements;

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
