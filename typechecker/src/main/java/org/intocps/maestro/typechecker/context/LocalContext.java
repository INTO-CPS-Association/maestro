package org.intocps.maestro.typechecker.context;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.typechecker.DeclarationList;

import java.util.List;

public class LocalContext extends Context {
    final DeclarationList typedefinitions;

    public LocalContext(DeclarationList typeDefinitions, Context outerContext) {
        super(outerContext);
        this.typedefinitions = typeDefinitions;
    }

    public LocalContext(List<? extends PDeclaration> typeDefinitions, Context outerContext) {
        super(outerContext);
        this.typedefinitions = new DeclarationList(typeDefinitions);
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
