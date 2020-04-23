package org.intocps.maestro.ast;

public class MableAstFactory {

    public ABooleanPrimitiveType newABooleanPrimitiveType() {
        return new ABooleanPrimitiveType();
    }

    public AFunctionType newAFunctionType() {
        return newAFunctionType();
    }

    public AIntNumericPrimitiveType newAIntPrimitiveType() {
        return new AIntNumericPrimitiveType();
    }

    public ARealNumericPrimitiveType newARealPrimitiveType() {
        return new ARealNumericPrimitiveType();
    }


    public AStringPrimitiveType newAStringPrimitiveType() {
        return new AStringPrimitiveType();
    }

    public AUIntNumericPrimitiveType newAUIntPrimitiveType() {
        return new AUIntNumericPrimitiveType();
    }

    public AUnknownType newAUnknownType() {
        return new AUnknownType();
    }


    public PType newAVoidType() {
        return new AVoidType();
    }

    public PType newAModuleType(LexIdentifier name) {
        return new AModuleType();
    }
}
