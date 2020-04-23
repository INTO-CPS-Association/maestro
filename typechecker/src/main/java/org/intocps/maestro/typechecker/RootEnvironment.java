package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;

import java.util.List;

public class RootEnvironment extends Environment {
    public RootEnvironment() {
        super(null);
    }

    @Override
    protected List<PDeclaration> getDefinitions() {
        return null;
    }

    @Override
    public PDeclaration findName(LexIdentifier name) {
        return null;
    }

    @Override
    public PDeclaration findType(LexIdentifier name) {
        return null;
    }
}
