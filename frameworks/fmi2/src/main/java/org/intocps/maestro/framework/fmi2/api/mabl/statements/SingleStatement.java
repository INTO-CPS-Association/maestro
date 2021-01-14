package org.intocps.maestro.framework.fmi2.api.mabl.statements;

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
