package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;

import java.util.Collection;
import java.util.Vector;

public class DeclarationList extends Vector<PDeclaration> {
    public DeclarationList(Collection<? extends PDeclaration> c) {
        super(c);
    }

    public DeclarationList() {
    }

    public PDeclaration findDeclaration(LexIdentifier name) {
        return this.stream().filter(d -> d.getName().equals(name)).findFirst().orElse(null);
    }
}
