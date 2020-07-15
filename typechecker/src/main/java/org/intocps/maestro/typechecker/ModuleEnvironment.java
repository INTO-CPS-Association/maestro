package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.PDeclaration;

import java.util.List;

public class ModuleEnvironment extends BaseEnvironment {
    public ModuleEnvironment(Environment outer, List<? extends PDeclaration> definitions) {
        super(outer, definitions);
    }
}
