package org.intocps.maestro.ast;

import org.intocps.maestro.ast.node.*;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class MableAstFactory {
    public static AUIntNumericPrimitiveType newAUIntNumericPrimitiveType() {
        return new AUIntNumericPrimitiveType();
    }

    public static ARefExp newARefExp(PExp expression) {
        ARefExp exp = new ARefExp();
        exp.setExp(expression);
        return exp;
    }

    public static ARefExp newARefExp(LexIdentifier name) {
        ARefExp exp = new ARefExp();
        exp.setExp(MableAstFactory.newAIdentifierExp(name));
        return exp;
    }

    public static AModuleType newAModuleType(LexIdentifier name) {
        AModuleType type = new AModuleType();
        type.setName(name);
        return type;
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

        if (type instanceof AArrayType) {
            throw new IllegalArgumentException("array declerations must use overload with size");
        }

        AVariableDeclaration vardecl = new AVariableDeclaration();

        vardecl.setName(name);
        vardecl.setType(type);
        vardecl.setInitializer(initializer_);
        return vardecl;
    }

    public static AVariableDeclaration newAVariableDeclaration(LexIdentifier name, PType type, int size, PInitializer initializer_) {
        return newAVariableDeclaration(name, type, MableAstFactory.newAIntLiteralExp(size), initializer_);
    }

    public static AVariableDeclaration newAVariableDeclarationMultiDimensionalArray(LexIdentifier name, PType type, List<Integer> shape) {
        AVariableDeclaration variableDeclaration = new AVariableDeclaration();
        variableDeclaration.setName(name);
        variableDeclaration.setType(type);
        List<AIntLiteralExp> size_ = shape.stream().map(MableAstFactory::newAIntLiteralExp).collect(Collectors.toList());
        variableDeclaration.setSize(size_);

        return variableDeclaration;
    }

    public static AVariableDeclaration newAVariableDeclaration(LexIdentifier name, PType type, PExp size, PInitializer initializer_) {
        if (type instanceof AArrayType) {
            type = ((AArrayType) type).getType();
        }

        AVariableDeclaration vardecl = new AVariableDeclaration();

        vardecl.setName(name);
        vardecl.setType(type);
        vardecl.setInitializer(initializer_);
        vardecl.setSize(new ArrayList<>(Collections.singletonList(size)));
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

    public static ATryStm newTry(ABasicBlockStm bodyStm, ABasicBlockStm finallyStm) {
        var stm = new ATryStm();
        stm.setBody(bodyStm);
        stm.setFinally(finallyStm);
        return stm;
    }

    public static LexIdentifier newLexIdentifier(String identifier) {
        LexIdentifier lexIdentifier = new LexIdentifier(identifier, null);
        return lexIdentifier;
    }

    public static ALessBinaryExp newALessBinaryExp(PExp left, PExp right) {
        ALessBinaryExp exp = new ALessBinaryExp();
        exp.setLeft(left);
        exp.setRight(right);
        return exp;
    }

    public static AGreaterBinaryExp newAGreaterBinaryExp(PExp left, PExp right) {
        AGreaterBinaryExp exp = new AGreaterBinaryExp();
        exp.setLeft(left);
        exp.setRight(right);
        return exp;
    }

    public static AGreaterEqualBinaryExp newAGreaterEqualBinaryExp(PExp left, PExp right) {
        AGreaterEqualBinaryExp exp = new AGreaterEqualBinaryExp();
        exp.setLeft(left);
        exp.setRight(right);
        return exp;
    }

    public static AEqualBinaryExp newAEqualBinaryExp(PExp left, PExp right) {
        AEqualBinaryExp exp = new AEqualBinaryExp();
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

    public static AVariableDeclaration newAVariableDeclaration(LexIdentifier name, PType type, PInitializer initializer_, List<PExp> size) {
        AVariableDeclaration vardecl = new AVariableDeclaration();

        vardecl.setName(name);
        vardecl.setType(type);
        vardecl.setInitializer(initializer_);
        vardecl.setSize(size);
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

    public static AUnloadExp newUnloadExp(PExp... args) {
        AUnloadExp exp = new AUnloadExp();
        if (args != null && args.length > 0) {
            exp.setArgs(Arrays.asList(args));
        }
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

    public static ACallExp newACallExp(LexToken expand, PExp object, LexIdentifier identifier, List<? extends PExp> args_) {
        ACallExp exp = new ACallExp();
        exp.setObject(object);
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

    public static ABasicBlockStm newABlockStm(List<? extends PStm> statements) {
        ABasicBlockStm stm = new ABasicBlockStm();
        stm.setBody(statements.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        return stm;
    }

    public static ABasicBlockStm newABlockStm(PStm... statements) {
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

    public static PExp newAParExp(PExp exp) {
        AParExp parExp = new AParExp();
        parExp.setExp(exp);
        return parExp;
    }

    public static ABooleanPrimitiveType newABoleanPrimitiveType() {
        ABooleanPrimitiveType type = new ABooleanPrimitiveType();
        return type;
    }

    public static ABooleanPrimitiveType newBoleanType() {
        return newABoleanPrimitiveType();
    }

    public static AVoidType newAVoidType() {
        AVoidType type = new AVoidType();
        return type;

    }

    public static ARealNumericPrimitiveType newARealNumericPrimitiveType() {
        ARealNumericPrimitiveType type = new ARealNumericPrimitiveType();
        return type;
    }

    public static ARealNumericPrimitiveType newRealType() {

        return newARealNumericPrimitiveType();
    }

    public static AStringPrimitiveType newAStringPrimitiveType() {
        AStringPrimitiveType type = new AStringPrimitiveType();
        return type;
    }

    public static AStringPrimitiveType newStringType() {
        return newAStringPrimitiveType();
    }

    public static AIntNumericPrimitiveType newAIntNumericPrimitiveType() {
        AIntNumericPrimitiveType type = new AIntNumericPrimitiveType();
        return type;
    }

    public static AIntNumericPrimitiveType newIntType() {
        return newAIntNumericPrimitiveType();
    }


    public static AUIntNumericPrimitiveType newUIntType() {
        return newAUIntNumericPrimitiveType();
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
        return new AArrayType(arrayType);
    }

    public static AArrayStateDesignator newAArayStateDesignator(PStateDesignator target, PExp exp) {
        AArrayStateDesignator arrayStateDesignator = new AArrayStateDesignator();
        arrayStateDesignator.setExp(exp);
        arrayStateDesignator.setTarget(target);
        return arrayStateDesignator;
    }

    public static AIdentifierStateDesignator newAIdentifierStateDesignator(String name) {
        return newAIdentifierStateDesignator(newAIdentifier(name));
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

    public static ADivideBinaryExp newDivideExp(PExp left, PExp right) {
        ADivideBinaryExp exp = new ADivideBinaryExp();
        exp.setLeft(left);
        exp.setRight(right);
        return exp;
    }

    public static AMultiplyBinaryExp newMultiplyExp(PExp left, PExp right) {
        AMultiplyBinaryExp exp = new AMultiplyBinaryExp();
        exp.setLeft(left);
        exp.setRight(right);
        return exp;
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

    public static AErrorStm newError(PExp exp) {
        AErrorStm errorStm = new AErrorStm();
        errorStm.setExp(exp);
        return errorStm;
    }

    public static ABreakStm newBreak() {
        return new ABreakStm();
    }

    public static AUnknownType newAUnknownType() {
        return new AUnknownType();
    }

    public static ANullType newNullType() {
        return new ANullType();
    }
}
