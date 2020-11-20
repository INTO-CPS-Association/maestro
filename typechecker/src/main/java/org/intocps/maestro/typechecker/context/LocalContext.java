package org.intocps.maestro.typechecker.context;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.typechecker.DeclarationList;

import java.util.List;

public class LocalContext extends Context {
    final DeclarationList decls;

    public LocalContext(DeclarationList decls, Context outerContext) {
        super(outerContext);
        this.decls = decls;
    }

    public LocalContext(List<? extends PDeclaration> decls, Context outerContext) {
        super(outerContext);
        this.decls = new DeclarationList(decls);
    }


    @Override
    public PDeclaration findDeclaration(LexIdentifier name) {
        PDeclaration declaration = decls.findDeclaration(name);
        if (declaration == null) {
            return super.findDeclaration(name);
        } else {
            return declaration;
        }
    }

}
