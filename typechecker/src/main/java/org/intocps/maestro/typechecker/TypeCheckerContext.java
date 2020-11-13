package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.ast.node.PType;

public class TypeCheckerContext extends ATypeCheckerContext {
    final TypeDefinitionMap typedefinitions;

    public TypeCheckerContext(TypeDefinitionMap typeDefinitions, ATypeCheckerContext outerContext) {
        super(outerContext);
        this.typedefinitions = typeDefinitions;
    }

    @Override
    public PType findDefinitionType(LexIdentifier name) {
        PType localPType = typedefinitions.getType(name);
        if (localPType == null) {
            return super.findDefinitionType(name);
        } else {
            return localPType;
        }

    }

    @Override
    public PDeclaration findDefinitionDeclaration(LexIdentifier name) {
        PDeclaration declaration = typedefinitions.getDeclaration(name);
        if (declaration == null) {
            return super.findDefinitionDeclaration(name);
        } else {
            return declaration;
        }
    }

}
