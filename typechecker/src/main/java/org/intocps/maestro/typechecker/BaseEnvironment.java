package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;

import java.util.List;

abstract class BaseEnvironment extends Environment {

    protected final List<? extends PDeclaration> definitions;

    public BaseEnvironment(Environment outer, List<? extends PDeclaration> definitions) {
        super(outer);
        this.definitions = definitions;
    }

    @Override
    protected List<? extends PDeclaration> getDefinitions() {
        return definitions;
    }

    @Override
    public PDeclaration findName(LexIdentifier name) {

        if (definitions != null) {
            for (PDeclaration def : definitions) {
                if (def.getName().getText().equals(name.getText())) {
                    return def;
                }
            }
        }
        if (outer != null) {
            return outer.findName(name);
        }
        return null;
    }

    @Override
    public PDeclaration findType(LexIdentifier name) {
        return null;
    }
}
