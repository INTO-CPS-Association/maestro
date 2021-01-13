package org.intocps.maestro.Fmi2AMaBLBuilder.statements;

import org.intocps.maestro.Fmi2AMaBLBuilder.AMaBLScope;
import org.intocps.maestro.ast.node.PStm;

public class ScopeStatement implements AMaBLStatement {
    public AMaBLScope scope;

    @Override
    public boolean isMeta() {
        return false;
    }

    @Override
    public PStm getStatement() {
        return scope.getStatement();
    }
}
