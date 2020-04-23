package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.PDeclaration;

import java.util.List;

public class SpecEnvironment extends BaseEnvironment {
    public SpecEnvironment(Environment outer, List<PDeclaration> definitions) {
        super(outer, definitions);
    }
}
