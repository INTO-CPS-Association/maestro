package org.intocps.maestro.typechecker;

public class TypeCheckInfo {
    public final ATypeCheckerContext typeCheckerContext;

    public TypeCheckInfo(ATypeCheckerContext context) {
        this.typeCheckerContext = context;
    }

    public ATypeCheckerContext getTypeCheckerContext() {
        return typeCheckerContext;
    }
}
