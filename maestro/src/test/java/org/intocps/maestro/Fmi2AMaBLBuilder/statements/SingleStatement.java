package org.intocps.maestro.Fmi2AMaBLBuilder.statements;

import org.intocps.maestro.ast.node.PStm;

public class SingleStatement implements AMaBLStatement {

    public final PStm stm;

    public SingleStatement(PStm stm) {
        this.stm = stm;
    }

    @Override
    public boolean isMeta() {
        return false;
    }

    @Override
    public PStm getStatement() {
        return stm;
    }
}
