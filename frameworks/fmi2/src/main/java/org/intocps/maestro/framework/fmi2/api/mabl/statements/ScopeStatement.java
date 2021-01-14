package org.intocps.maestro.framework.fmi2.api.mabl.statements;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.AMaBLScope;

public class ScopeStatement implements AMaBLStatement {
    public AMaBLScope scope;

    @Override
    public boolean isMeta() {
        return false;
    }

    @Override
    public PStm getStatement() {
        return null;//scope.getStatement();
    }
}
