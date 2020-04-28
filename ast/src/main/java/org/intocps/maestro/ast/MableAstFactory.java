package org.intocps.maestro.ast;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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

    public static AIdentifierExp newAIdentifierExp(LexIdentifier name) {
        AIdentifierExp identifier = new AIdentifierExp();
        identifier.setName(name);
        return identifier;
    }

    public static AVariableDeclaration newAVariableDeclaration(LexIdentifier name, PType type, PInitializer initializer_){
        AVariableDeclaration vardecl = new AVariableDeclaration();
        vardecl.setName(name);
        vardecl.setType(type);
        vardecl.setInitializer(initializer_);
        return vardecl;
    }

    public static ANameType newANameType(LexIdentifier name){
        ANameType nameType = new ANameType();
        nameType.setName(name);
        return nameType;
    }

    public static AStringLiteralExp newAStringLiteralExp(String s)
    {
        AStringLiteralExp exp = new AStringLiteralExp();
        exp.setValue(s);
        return exp;
    }

    public static ALoadExp newALoadExp(URI uri){
        ALoadExp exp = new ALoadExp();
        List<PExp> args = new ArrayList<PExp>();
        args.add(newAStringLiteralExp(uri.toString()));
        exp.setArgs(args);
        return exp;
    }

    public static ACallExp newACallExp(PExp identifier, List<? extends PExp> args_)
    {
        ACallExp exp = new ACallExp();
        exp.setIdentifier(identifier);
        exp.setArgs(args_);
        return exp;
    }

    public static AExpInitializer newAExpInitializer(PExp exp)
    {
        AExpInitializer expInit = new AExpInitializer();
        expInit.setExp(exp);
        return expInit;
    }

    public static ADotExp newADotExp(PExp root, PExp exp)
    {
        ADotExp exp_ = new ADotExp();
        exp_.setRoot(root);
        exp_.setExp(exp);
        return exp_;
    }
    public static ALocalVariableStm newALocalVariableStm(AVariableDeclaration aVariableDeclaration){
        ALocalVariableStm stm = new ALocalVariableStm();
        stm.setDeclaration(aVariableDeclaration);
        return stm;
    }

    public static ABlockStm newABlockStm(List<? extends PStm> statements )
    {
        ABlockStm stm = new ABlockStm();
        stm.setBody(statements);
        return stm;
    }

    public static AFunctionDeclaration newAFunctionDeclaration(LexIdentifier name)
    {
        AFunctionDeclaration funcDecl = new AFunctionDeclaration();
        funcDecl.setName(name);
        return funcDecl;
    }
}
