package org.intocps.maestro.typechecker.context;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.typechecker.DeclarationList;

import java.util.List;

public class GlobalContext extends Context {
    final DeclarationList decls;

    public GlobalContext(DeclarationList decls, Context outerContext) {
        super(outerContext);
        this.decls = decls;
    }

    public GlobalContext(List<? extends PDeclaration> decls, Context outerContext) {
        super(outerContext);
        this.decls = new DeclarationList(decls);
    }

    @Override
    public PDeclaration findGlobalDeclaration(LexIdentifier name) {

        PDeclaration declaration = decls.findDeclaration(name);
        if (declaration == null) {
            return super.findGlobalDeclaration(name);
        } else {
            return declaration;
        }
    }
}
