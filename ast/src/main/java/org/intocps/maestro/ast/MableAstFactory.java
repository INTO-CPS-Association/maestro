package org.intocps.maestro.ast;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MableAstFactory {
    public static AUIntNumericPrimitiveType newAUIntNumericPrimitiveType() {
        return new AUIntNumericPrimitiveType();
    }


    public static AIdentifierExp newAIdentifierExp(LexIdentifier name) {
        AIdentifierExp identifier = new AIdentifierExp();
        identifier.setName(name);
        return identifier;
    }

    public static AInstanceMappingStm newAInstanceMappingStm(LexIdentifier mablName, String envName) {
        AInstanceMappingStm ims = new AInstanceMappingStm();
        ims.setIdentifier(mablName);
        ims.setName(envName);
        return ims;
    }

    public static AIdentifierExp newAIdentifierExp(String name) {
        return newAIdentifierExp(newAIdentifier(name));
    }

    public static AVariableDeclaration newAVariableDeclaration(LexIdentifier name, PType type) {
        return newAVariableDeclaration(name, type, null);
    }

    public static AVariableDeclaration newAVariableDeclaration(LexIdentifier name, PType type, PInitializer initializer_) {
        AVariableDeclaration vardecl = new AVariableDeclaration();

        vardecl.setName(name);
        vardecl.setType(type);
        vardecl.setInitializer(initializer_);
        if (type instanceof AArrayType) {
            vardecl.setIsArray(true);
            AArrayType type_ = (AArrayType) type;
            vardecl.setSize(new ArrayList<PExp>(Arrays.asList(MableAstFactory.newAIntLiteralExp(type_.getSize()))));
        } else {
            vardecl.setIsArray(false);
        }
        return vardecl;
    }

    public static ANullExp newNullExp() {
        return new ANullExp();
    }

    public static ASimulationSpecificationCompilationUnit newASimulationSpecificationCompilationUnit(List<? extends LexIdentifier> imports,
            PStm body) {
        ASimulationSpecificationCompilationUnit stm = new ASimulationSpecificationCompilationUnit();
        stm.setImports(imports);
        stm.setBody(body);
        return stm;
    }

    public static AWhileStm newWhile(PExp test, PStm body) {
        AWhileStm stm = new AWhileStm();
        stm.setTest(test);
        stm.setBody(body);
        return stm;
    }

    public static AIfStm newIf(PExp test, PStm thenStm, PStm elseStm) {
        AIfStm stm = new AIfStm();
        stm.setTest(test);
        stm.setThen(thenStm);
        stm.setElse(elseStm);
        return stm;
    }

    public static ALessBinaryExp newALessBinaryExp(PExp left, PExp right) {
        ALessBinaryExp exp = new ALessBinaryExp();
        exp.setLeft(left);
        exp.setRight(right);
        return exp;
    }

    public static ALessEqualBinaryExp newALessEqualBinaryExp(PExp left, PExp right) {
        ALessEqualBinaryExp exp = new ALessEqualBinaryExp();
        exp.setLeft(left);
        exp.setRight(right);
        return exp;
    }

    public static ANotUnaryExp newNot(PExp exp) {
        ANotUnaryExp n = new ANotUnaryExp();
        n.setExp(exp);
        return n;
    }

    public static AEqualBinaryExp newEqual(PExp left, PExp right) {
        AEqualBinaryExp exp = new AEqualBinaryExp();
        exp.setLeft(left);
        exp.setRight(right);
        return exp;
    }

    public static ANotEqualBinaryExp newNotEqual(PExp left, PExp right) {
        ANotEqualBinaryExp exp = new ANotEqualBinaryExp();
        exp.setLeft(left);
        exp.setRight(right);
        return exp;
    }

    public static AVariableDeclaration newAVariableDeclaration(LexIdentifier name, PType type, PInitializer initializer_, Boolean isArray,
            List<PExp> size) {
        AVariableDeclaration vardecl = new AVariableDeclaration();

        vardecl.setName(name);
        vardecl.setType(type);
        vardecl.setInitializer(initializer_);
        vardecl.setIsArray(isArray);
        if (isArray) {
            vardecl.setSize(size);
        }
        return vardecl;
    }

    public static ANameType newANameType(LexIdentifier name) {
        ANameType nameType = new ANameType();
        nameType.setName(name);
        return nameType;
    }

    public static ANameType newANameType(String name) {
        return newANameType(newAIdentifier(name));
    }


    public static ALoadExp newALoadExp(URI uri) {
        ALoadExp exp = new ALoadExp();
        List<PExp> args = new ArrayList<>();
        args.add(newAStringLiteralExp(uri.toString()));
        exp.setArgs(args);
        return exp;
    }

    public static AUnloadExp newUnloadExp(List<? extends PExp> args) {
        AUnloadExp exp = new AUnloadExp();
        exp.setArgs(args);
        return exp;
    }


    public static AExpressionStm newExpressionStm(PExp exp) {
        AExpressionStm stm = new AExpressionStm();
        stm.setExp(exp);
        return stm;
    }

    public static ALoadExp newALoadExp(List<? extends PExp> args) {
        ALoadExp exp = new ALoadExp();
        exp.setArgs(args);
        return exp;
    }

    public static ACallExp newACallExp(LexIdentifier identifier, List<? extends PExp> args_) {
        ACallExp exp = new ACallExp();
        exp.setMethodName(identifier);
        exp.setArgs(args_);
        return exp;
    }

    public static LexToken newExpandToken() {
        return new LexToken("expand", 0, 0);
    }

    public static ACallExp newACallExp(LexToken expand, LexIdentifier identifier, List<? extends PExp> args_) {
        ACallExp exp = new ACallExp();
        exp.setExpand(expand);
        exp.setMethodName(identifier);
        exp.setArgs(args_);
        return exp;
    }

    public static ACallExp newACallExp(PExp object, LexIdentifier identifier, List<? extends PExp> args_) {
        ACallExp exp = new ACallExp();
        exp.setObject(object);
        exp.setMethodName(identifier);
        exp.setArgs(args_);
        return exp;
    }

    public static AExpInitializer newAExpInitializer(PExp exp) {
        AExpInitializer expInit = new AExpInitializer();
        expInit.setExp(exp);
        return expInit;
    }

    public static AFieldExp newAFieldExp(PExp root, LexIdentifier field) {
        AFieldExp exp_ = new AFieldExp();
        exp_.setRoot(root);
        exp_.setField(field);
        return exp_;
    }

    public static ALocalVariableStm newALocalVariableStm(AVariableDeclaration aVariableDeclaration) {
        ALocalVariableStm stm = new ALocalVariableStm();
        stm.setDeclaration(aVariableDeclaration);
        return stm;
    }

    public static ABlockStm newABlockStm(List<? extends PStm> statements) {
        ABlockStm stm = new ABlockStm();
        stm.setBody(statements.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        return stm;
    }

    public static ABlockStm newABlockStm(PStm... statements) {
        return newABlockStm(Arrays.asList(statements));
    }

    public static AFunctionDeclaration newAFunctionDeclaration(LexIdentifier name, List<? extends AFormalParameter> arguments, PType returnType) {
        AFunctionDeclaration funcDecl = new AFunctionDeclaration();
        funcDecl.setName(name);
        funcDecl.setFormals(arguments);
        funcDecl.setReturnType(returnType);
        return funcDecl;
    }

    public static AFormalParameter newAFormalParameter(PType type, LexIdentifier name) {
        AFormalParameter formal = new AFormalParameter();

        formal.setType(type);
        formal.setName(name);

        return formal;
    }

    public static AUIntLiteralExp newAUIntLiteralExp(Long value) {
        AUIntLiteralExp exp = new AUIntLiteralExp();
        exp.setValue(value);
        return exp;
    }

    public static AArrayInitializer newAArrayInitializer(List<? extends PExp> args) {
        AArrayInitializer initializer = new AArrayInitializer();
        initializer.setExp(args);
        return initializer;
    }

    public static ABoolLiteralExp newABoolLiteralExp(Boolean value) {
        ABoolLiteralExp exp = new ABoolLiteralExp();
        exp.setValue(value);
        return exp;
    }

    public static ARealLiteralExp newARealLiteralExp(Double value) {
        ARealLiteralExp exp = new ARealLiteralExp();
        exp.setValue(value);
        return exp;
    }

    public static AIntLiteralExp newAIntLiteralExp(Integer value) {
        AIntLiteralExp exp = new AIntLiteralExp();
        exp.setValue(value);
        return exp;
    }

    public static AStringLiteralExp newAStringLiteralExp(String value) {
        AStringLiteralExp exp = new AStringLiteralExp();
        exp.setValue(value);
        return exp;
    }

    // TODO: FIX
    public static AAssigmentStm newAAssignmentStm(PStateDesignator target, PExp exp) {
        AAssigmentStm stm = new AAssigmentStm();
        stm.setTarget(target);
        stm.setExp(exp);
        return stm;
    }


    public static ABooleanPrimitiveType newABoleanPrimitiveType() {
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

    public static AIntNumericPrimitiveType newAIntNumericPrimitiveType() {
        AIntNumericPrimitiveType type = new AIntNumericPrimitiveType();
        return type;
    }

    public static AArrayType newAArrayType(PType arrayType, Integer size) {
        AArrayType type = new AArrayType();
        type.setType(arrayType);
        type.setSize(size);
        return type;
    }

    public static AParExp newPar(PExp exp) {
        AParExp par = new AParExp();
        par.setExp(exp);
        return par;
    }

    public static AOrBinaryExp newOr(PExp left, PExp right) {
        AOrBinaryExp exp = new AOrBinaryExp();
        exp.setLeft(left);
        exp.setRight(right);
        return exp;
    }

    public static AAndBinaryExp newAnd(PExp left, PExp right) {
        AAndBinaryExp exp = new AAndBinaryExp();
        exp.setLeft(left);
        exp.setRight(right);
        return exp;
    }

    public static AArrayType newAArrayType(PType arrayType) {
        return newAArrayType(arrayType, null);
    }

    public static AArrayStateDesignator newAArayStateDesignator(PStateDesignator target, PExp exp) {
        AArrayStateDesignator arrayStateDesignator = new AArrayStateDesignator();
        arrayStateDesignator.setExp(exp);
        arrayStateDesignator.setTarget(target);
        return arrayStateDesignator;
    }

    public static AIdentifierStateDesignator newAIdentifierStateDesignator(LexIdentifier name) {
        AIdentifierStateDesignator identifierStateDesignator = new AIdentifierStateDesignator();
        identifierStateDesignator.setName(name);
        return identifierStateDesignator;
    }

    public static AArrayIndexExp newAArrayIndexExp(PExp array, List<? extends PExp> values) {
        AArrayIndexExp exp = new AArrayIndexExp();
        exp.setArray(array);
        exp.setIndices(values);
        return exp;
    }

    public static LexIdentifier newAIdentifier(String identifier) {
        return new LexIdentifier(identifier, null);
    }

    public static AMinusBinaryExp newMinusExp(PExp left, PExp right) {
        AMinusBinaryExp exp = new AMinusBinaryExp();
        exp.setLeft(left);
        exp.setRight(right);
        return exp;
    }

    public static APlusBinaryExp newPlusExp(PExp left, PExp right) {
        APlusBinaryExp exp = new APlusBinaryExp();
        exp.setLeft(left);
        exp.setRight(right);
        return exp;
    }

    public static ABreakStm newBreak() {
        return new ABreakStm();
    }

    public PType newAModuleType(LexIdentifier name) {
        return new AModuleType();
    }

    public AUnknownType newAUnknownType() {
        return new AUnknownType();
    }
}
