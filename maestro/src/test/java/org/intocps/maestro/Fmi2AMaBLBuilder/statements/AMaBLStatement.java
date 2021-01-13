package org.intocps.maestro.Fmi2AMaBLBuilder.statements;

import org.intocps.maestro.ast.node.PStm;

public interface AMaBLStatement {
    boolean isMeta();

    PStm getStatement();
}

