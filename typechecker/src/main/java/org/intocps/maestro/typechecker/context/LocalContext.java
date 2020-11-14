package org.intocps.maestro.typechecker.context;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.typechecker.DeclarationList;

public class LocalContext extends Context {
    final DeclarationList typedefinitions;

    public LocalContext(DeclarationList typeDefinitions, Context outerContext) {
        super(outerContext);
        this.typedefinitions = typeDefinitions;
    }


    @Override
    public PDeclaration findDeclaration(LexIdentifier name) {
        PDeclaration declaration = typedefinitions.findDeclaration(name);
        if (declaration == null) {
            return super.findDeclaration(name);
        } else {
            return declaration;
        }
    }

}
