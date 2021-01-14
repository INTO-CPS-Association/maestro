package org.intocps.maestro.framework.fmi2.api.mabl.statements;

import org.intocps.maestro.ast.node.PStm;

public interface AMaBLStatement {
    boolean isMeta();

    PStm getStatement();
}

