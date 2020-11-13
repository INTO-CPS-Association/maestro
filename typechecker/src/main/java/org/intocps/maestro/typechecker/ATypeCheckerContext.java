package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.ast.node.PType;

public abstract class ATypeCheckerContext {

    private final ATypeCheckerContext outerContext;


    public ATypeCheckerContext(ATypeCheckerContext outerContext) {
        this.outerContext = outerContext;
    }

    public PType findModuleType(LexIdentifier module) {
        if (findModuleDeclarations(module) != null) {
            return MableAstFactory.newAModuleType(module);
        } else {
            return null;
        }
    }

    public TypeDefinitionMap findModuleDeclarations(LexIdentifier module) {
        if (outerContext == null) {
            return null;
        }
        return outerContext.findModuleDeclarations(module);
    }

    public PType getType(PDeclaration def) {
        if (outerContext == null) {
            return null;
        }
        return outerContext.getType(def);
    }


    public PType findDefinitionType(LexIdentifier name) {
        if (outerContext == null) {
            return null;
        }
        return outerContext.findDefinitionType(name);
    }

    public PDeclaration findDefinition(LexIdentifier name) {
        if (outerContext == null) {
            return null;
        }
        return outerContext.findDefinition(name);
    }


}