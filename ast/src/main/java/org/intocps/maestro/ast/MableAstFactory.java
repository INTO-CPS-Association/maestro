package org.intocps.maestro.ast;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class MableAstFactory {



    public static AUIntNumericPrimitiveType newAUIntNumericPrimitiveType() {
        return new AUIntNumericPrimitiveType();
    }


    public static AIdentifierExp newAIdentifierExp(LexIdentifier name) {
        AIdentifierExp identifier = new AIdentifierExp();
        identifier.setName(name);
        return identifier;
    }

    public static AVariableDeclaration newAVariableDeclaration(LexIdentifier name, PType type, PInitializer initializer_) {
        AVariableDeclaration vardecl = new AVariableDeclaration();
        vardecl.setName(name);
        vardecl.setType(type);
        vardecl.setInitializer(initializer_);
        return vardecl;
    }

    public static ANameType newANameType(LexIdentifier name) {
        ANameType nameType = new ANameType();
        nameType.setName(name);
        return nameType;
    }


    public static ALoadExp newALoadExp(URI uri) {
        ALoadExp exp = new ALoadExp();
        List<PExp> args = new ArrayList<>();
        args.add(newAStringLiteralExp(uri.toString()));
        exp.setArgs(args);
        return exp;
    }

    public static ACallExp newACallExp(PExp identifier, List<? extends PExp> args_) {
        ACallExp exp = new ACallExp();
        exp.setRoot(identifier);
        exp.setArgs(args_);
        return exp;
    }

    public static AExpInitializer newAExpInitializer(PExp exp) {
        AExpInitializer expInit = new AExpInitializer();
        expInit.setExp(exp);
        return expInit;
    }

    public static ADotExp newADotExp(PExp root, PExp exp) {
        ADotExp exp_ = new ADotExp();
        exp_.setRoot(root);
        exp_.setExp(exp);
        return exp_;
    }

    public static ALocalVariableStm newALocalVariableStm(AVariableDeclaration aVariableDeclaration) {
        ALocalVariableStm stm = new ALocalVariableStm();
        stm.setDeclaration(aVariableDeclaration);
        return stm;
    }

    public static ABlockStm newABlockStm(List<? extends PStm> statements) {
        ABlockStm stm = new ABlockStm();
        stm.setBody(statements);
        return stm;
    }

    public static AFunctionDeclaration newAFunctionDeclaration(LexIdentifier name,
                                                               List<? extends AFormalParameter> arguments,
                                                               PType returnType) {
        AFunctionDeclaration funcDecl = new AFunctionDeclaration();
        funcDecl.setName(name);
        funcDecl.setFormals(arguments);
        funcDecl.setReturnType(returnType);
        return funcDecl;
    }

    public static AUIntLiteralExp newAUIntLiteralExp(Long value)
    {
        AUIntLiteralExp exp = new AUIntLiteralExp();
        exp.setValue(value);
        return exp;
    }

    public static AArrayInitializer newAArrayInitializer(List<? extends PExp> args){
        AArrayInitializer initializer = new AArrayInitializer();
        initializer.setExp(args);
        return initializer;
    }

    public static ABoolLiteralExp newABoolLiteralExp(Boolean value){
        ABoolLiteralExp exp = new ABoolLiteralExp();
                exp.setValue(value);
        return exp;
    }

    public static ARealLiteralExp newARealLiteralExp(Double value)
    {
        ARealLiteralExp exp = new ARealLiteralExp();
        exp.setValue(value);
        return exp;
    }

    public static AIntLiteralExp newAIntLiteralExp(Integer value)
    {
        AIntLiteralExp exp = new AIntLiteralExp();
        exp.setValue(value);
        return exp;
    }

    public static AStringLiteralExp newAStringLiteralExp(String value)
    {
        AStringLiteralExp exp = new AStringLiteralExp();
        exp.setValue(value);
        return exp;
    }

    // TODO: FIX
    public static AAssigmentStm newAAssignmentStm(PStateDesignator target, PExp exp){
        AAssigmentStm stm = new AAssigmentStm();
        stm.setTarget(target);
        stm.setExp(exp);
        return stm;
    }

    public static ABooleanPrimitiveType newABoleanPrimitiveType(){
        ABooleanPrimitiveType type = new ABooleanPrimitiveType();
        return type;
    }

    public static AVoidType newAVoidType() {
        AVoidType type = new AVoidType();
        return type;

    }

    public static ARealNumericPrimitiveType newARealNumericPrimitiveType() {
        ARealNumericPrimitiveType type = new ARealNumericPrimitiveType();
        return type;
    }

    public static AStringPrimitiveType newAStringPrimitiveType() {
        AStringPrimitiveType type = new AStringPrimitiveType();
        return type;
    }

    public static AIntNumericPrimitiveType newAIntNumericPrimitiveType(){
        AIntNumericPrimitiveType type = new AIntNumericPrimitiveType();
        return type;
    }

    public static AArrayType newAArrayType(PType arrayType, Integer size){
        AArrayType type = new AArrayType();
                type.setType(arrayType);
                type.setSize(size);
                return type;
    }

    public static AArrayStateDesignator newAArayStateDesignator(PStateDesignator target, SLiteralExp exp){
        AArrayStateDesignator arrayStateDesignator = new AArrayStateDesignator();
        arrayStateDesignator.setExp(exp);
        arrayStateDesignator.setTarget(target);
        return arrayStateDesignator;
    }

    public static AIdentifierStateDesignator newAIdentifierStateDesignator(LexIdentifier name){
        AIdentifierStateDesignator identifierStateDesignator = new AIdentifierStateDesignator();
        identifierStateDesignator.setName(name);
        return identifierStateDesignator;
    }

    public static AArrayIndexExp newAArrayIndexExp(PExp array, List<? extends PExp> values){
        AArrayIndexExp exp = new AArrayIndexExp();
        exp.setArray(array);
        exp.setIndices(values);
        return exp;
    }

    public PType newAModuleType(LexIdentifier name) {
        return new AModuleType();
    }

    public AUnknownType newAUnknownType() {
        return new AUnknownType();
    }


}
