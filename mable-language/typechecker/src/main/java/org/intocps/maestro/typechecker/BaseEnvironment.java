package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;

import java.util.List;

abstract class BaseEnvironment extends Environment {

    public BaseEnvironment(Environment outer, List<PDeclaration> definitions) {
        super(outer);
        this.definitions = definitions;
    }

    protected final List<PDeclaration> definitions;


    @Override
    protected List<PDeclaration> getDefinitions() {
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
