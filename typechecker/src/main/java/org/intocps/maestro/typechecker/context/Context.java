package org.intocps.maestro.typechecker.context;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;

public abstract class Context {

    private final Context outerContext;


    public Context(Context outerContext) {
        this.outerContext = outerContext;
    }

    public PDeclaration findDeclaration(LexIdentifier module, LexIdentifier name) {
        if (outerContext == null) {
            return null;
        }
        return outerContext.findDeclaration(module, name);
    }

    public PDeclaration findDeclaration(LexIdentifier name) {
        if (outerContext == null) {
            return null;
        }
        return outerContext.findDeclaration(name);
    }


}