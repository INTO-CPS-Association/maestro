package org.intocps.maestro.typechecker;

import org.apache.commons.collections.map.HashedMap;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.analysis.QuestionAnswerAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.InternalException;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.typechecker.context.Context;
import org.intocps.maestro.typechecker.context.GlobalContext;
import org.intocps.maestro.typechecker.context.LocalContext;
import org.intocps.maestro.typechecker.context.ModulesContext;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;

class TypeCheckVisitor extends QuestionAnswerAdaptor<Context, PType> {
    private final IErrorReporter errorReporter;
    private final Map<INode, PType> resolvedTypes = new HashMap<>();
    TypeComparator typeComparator;
    MableAstFactory astFactory;
    Map<INode, PType> checkedTypes = new HashedMap();

    public TypeCheckVisitor(IErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
        this.typeComparator = new TypeComparator();
        astFactory = new MableAstFactory();
    }

    @Override
    public PType createNewReturnValue(INode node, Context info) throws AnalysisException {
        return null;
    }

    @Override
    public PType createNewReturnValue(Object node, Context info) throws AnalysisException {
        return null;
    }

    @Override
    public PType defaultPInitializer(PInitializer node, Context ctxt) throws AnalysisException {
        return super.defaultPInitializer(node, ctxt);
    }

    @Override
    public PType defaultPType(PType node, Context ctxt) throws AnalysisException {
        return store(node, node.clone());
    }

    @Override
    public PType caseAArrayType(AArrayType node, Context ctxt) throws AnalysisException {
        //FIXME should not need clone
        return store(node, MableAstFactory.newAArrayType(node.getType().apply(this, ctxt).clone()));
    }

    @Override
    public PType caseAExpInitializer(AExpInitializer node, Context ctxt) throws AnalysisException {
        return store(node, node.getExp().apply(this, ctxt));
    }

    @Override
    public PType caseAArrayIndexExp(AArrayIndexExp node, Context ctxt) throws AnalysisException {

        for (PExp index : node.getIndices()) {
            PType type = index.apply(this, ctxt);
            if (!typeComparator.compatible(AIntNumericPrimitiveType.class, type)) {
                errorReporter.report(-5, "Array index must be an integer actual: " + type, null);
            }
        }
        PType type = node.getArray().apply(this, ctxt);

        if (!(type instanceof AArrayType)) {
            errorReporter.report(0, "Canont index none array expression", null);
            return store(node, MableAstFactory.newAUnknownType());
        } else {
            return store(node, ((AArrayType) type).getType());
        }
    }

    @Override
    public PType caseALoadExp(ALoadExp node, Context ctxt) throws AnalysisException {
        //TODO: Needs work in terms of load type.
        // See https://github.com/INTO-CPS-Association/maestro/issues/66
        // Return whatever type, such that variable declaration decides.
        return store(node, newAUnknownType());
    }

    @Override
    public PType caseAMinusUnaryExp(AMinusUnaryExp node, Context ctxt) throws AnalysisException {

        return store(node, checkUnaryNumeric(node, ctxt));
    }

    private PType checkUnaryNumeric(SUnaryExp node, Context ctxt) throws AnalysisException {
        PType type = node.getExp().apply(this, ctxt);

        if (!typeComparator.compatible(SNumericPrimitiveType.class, type)) {
            errorReporter.report(-5, "Expected a numeric expression: " + type, null);
        }
        return store(node, type);
    }

    @Override
    public PType caseAPlusUnaryExp(APlusUnaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkUnaryNumeric(node, ctxt));
    }

    @Override
    public PType caseAArrayInitializer(AArrayInitializer node, Context ctxt) throws AnalysisException {

        PType type = newAUnknownType();

        if (node.getExp().size() > 0) {

            type = node.getExp().get(0).apply(this, ctxt);
            for (int i = 1; i < node.getExp().size(); i++) {
                PType elementType = node.getExp().get(i).apply(this, ctxt);
                if (!typeComparator.compatible(type, elementType)) {
                    errorReporter.report(0, "Array initializer types mixed. Expected: " + type + " but found: " + elementType, null);
                }
            }
        } else {
            errorReporter.report(0, "Array initializer must not be empty", null);
        }
        return store(node, newAArrayType(type));
    }

    @Override
    public PType caseACallExp(ACallExp node, Context ctxt) throws AnalysisException {

        PDeclaration def = null;


        if (node.getObject() != null) {
            // This is a module
            // Ensure that the object is of module type
            PType objectType = node.getObject().apply(this, ctxt);

            if (objectType instanceof AModuleType) {
                def = ctxt.findDeclaration(((AModuleType) objectType).getName(), node.getMethodName());
            } else {
                errorReporter.report(0, "Unknown object type", node.getMethodName().getSymbol());
            }
        } else {
            def = ctxt.findDeclaration(node.getMethodName());
        }

        if (def == null) {
            errorReporter.report(0, "Call decleration not found: " + node.getMethodName(), node.getMethodName().getSymbol());
        } else {
            PType type = checkedTypes.get(def);


            if (type instanceof AFunctionType) {
                AFunctionType targetType = (AFunctionType) type;


                List<PType> callArgTypes = new Vector<>();

                //                if (targetType.getParameters().size() != node.getArgs().size()) {
                //                    errorReporter.report(0, "Wrong number of arguments in call '" + node.getMethodName().getText() + "'. Expected: " +
                //                            targetType.getParameters().size() + " " + "found: " + node.getArgs().size(), node.getMethodName().getSymbol());
                //                } else {
                for (int i = 0; i < node.getArgs().size(); i++) {
                    PExp arg = node.getArgs().get(i);
                    PType argType = arg.apply(this, ctxt);
                    callArgTypes.add(argType);
                    //                        PType argTargetType = targetType.getParameters().get(i);
                    //                        if (!typeComparator.compatible(argTargetType, argType)) {
                    //                            errorReporter.report(-5, "Parameter type: " + argType + " not matching expected type: " + argTargetType, null);
                    //
                    //                        }
                }
                //                }
                //                if (targetType.getParameters().size() != node.getArgs().size()) {
                //                    System.out.println();
                //                }

                AFunctionType callType = new AFunctionType();
                callType.setResult(targetType.getResult().clone());
                callType.setParameters(callArgTypes);

                if (!typeComparator.compatible(targetType, callType)) {
                    errorReporter.report(0, "Function applied with wrong argument types. Expected: " + targetType + " Actual: " + callType,
                            node.getMethodName().getSymbol());
                }


                return store(node, callType);
            } else {
                errorReporter.report(0, "Expected a function decleration: " + def.getName().getText(), def.getName().getSymbol());
            }
        }
        return store(node, newAUnknownType());

    }

    @Override
    public PType caseANameType(ANameType node, Context ctxt) throws AnalysisException {

        PDeclaration def = ctxt.findDeclaration(node.getName());

        if (!(def instanceof AModuleDeclaration)) {
            if (def == null) {
                errorReporter.report(0, "Unresolved name: " + node.getName().getText() + ". Did you forgot to include " + "a module?",
                        node.getName().getSymbol());
            } else {
                errorReporter.report(0, "Type: " + node.getName().getText() + " Is not a module.", node.getName().getSymbol());
            }
            return store(node, newAUnknownType());
        }


        PType type = checkedTypes.get(def);
        if (type == null) {
            errorReporter.report(-5, "No type found for decleration " + node.getName(), null);
        } else {
            return store(node, type);
        }
        return store(node, newAUnknownType());
    }

    @Override
    public PType defaultSNumericPrimitiveType(SNumericPrimitiveType node, Context ctxt) throws AnalysisException {
        return store(node, node.clone());
    }

    @Override
    public PType caseABooleanPrimitiveType(ABooleanPrimitiveType node, Context ctxt) throws AnalysisException {
        return store(node, node.clone());
    }

    @Override
    public PType caseAVariableDeclaration(AVariableDeclaration node, Context ctxt) throws AnalysisException {
        PType type = node.getType().apply(this, ctxt);

        if (!node.getSize().isEmpty()) {
            //only one dimensional arrays for now
            //type = MableAstFactory.newAArrayType(type);

            for (PExp sizeExp : node.getSize()) {
                PType sizeType = sizeExp.apply(this, ctxt);
                if (!typeComparator.compatible(MableAstFactory.newIntType(), sizeType)) {
                    errorReporter.report(0, "Array size must be int. Actual: " + sizeType, null);
                }
            }
        }

        if (node.getInitializer() != null) {
            PType initType = node.getInitializer().apply(this, ctxt);
            if (!typeComparator.compatible(type, initType)) {
                errorReporter.report(0, type + " array cannot be initialized with type: " + initType, node.getName().getSymbol());
            }

            //additional check for array initializer, not complete but should do most simple cases
            if (node.getInitializer() instanceof AArrayInitializer) {
                AArrayInitializer initializer = (AArrayInitializer) node.getInitializer();

                Integer declaredSize = null;
                if (!node.getSize().isEmpty() && node.getSize().get(0) instanceof AIntLiteralExp) {
                    declaredSize = ((AIntLiteralExp) node.getSize().get(0)).getValue();
                }

                if (!node.getSize().isEmpty() || !initializer.getExp().isEmpty()) {
                    if (declaredSize == null || declaredSize != initializer.getExp().size()) {
                        if (declaredSize == null) {
                            errorReporter.report(0,
                                    "Array declared without a size. Please fix by declaring with size: '" + +initializer.getExp().size() + "'", null);
                        } else {
                            errorReporter.report(0, "Array declared with different size than initializer. Declared size: '" + declaredSize +
                                    "'. Size of initializer: " + initializer.getExp().size(), null);
                        }

                    }

                }
            }
        }
        return store(node, type);
    }

    private <T extends PType> T store(INode node, T type) {
        checkedTypes.put(node, type);
        return type;
    }

    @Override
    public PType defaultPExp(PExp node, Context ctxt) throws AnalysisException {
        throw new InternalException(-5, "Node unknown to typechecker: " + node + " type: " + node.getClass().getSimpleName());
    }

    @Override
    public PType defaultPStm(PStm node, Context ctxt) throws AnalysisException {
        throw new InternalException(-5, "Node unknown to typechecker: " + node + " type: " + node.getClass().getSimpleName());
    }

    @Override
    public PType caseAConfigStm(AConfigStm node, Context question) throws AnalysisException {
        return MableAstFactory.newAVoidType();
    }

    @Override
    public PType caseAConfigFramework(AConfigFramework node, Context question) throws AnalysisException {
        return MableAstFactory.newAUnknownType();
    }

    @Override
    public PType caseAPlusBinaryExp(APlusBinaryExp node, Context ctxt) throws AnalysisException {

        return store(node, checkNumeric(node, ctxt));
    }

    @Override
    public PType caseAGreaterBinaryExp(AGreaterBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkNumeric(node, ctxt));
    }

    @Override
    public PType caseAGreaterEqualBinaryExp(AGreaterEqualBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkNumeric(node, ctxt));
    }

    @Override
    public PType caseAMultiplyBinaryExp(AMultiplyBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkNumeric(node, ctxt));
    }

    @Override
    public PType caseADivideBinaryExp(ADivideBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkNumeric(node, ctxt));
    }

    public PType checkNumeric(SBinaryExp node, Context ctxt) throws AnalysisException {

        PType left = node.getLeft().apply(this, ctxt);

        if (!typeComparator.compatible(SNumericPrimitiveType.class, left)) {
            errorReporter.report(2, "Type is not numeric: " + node.getLeft() + " - type: " + left, null);
        }

        PType right = node.getRight().apply(this, ctxt);

        if (!typeComparator.compatible(SNumericPrimitiveType.class, right)) {
            errorReporter.report(2, "Type is not numeric: " + node + " - type: " + right, null);
        }

        if (left instanceof ARealNumericPrimitiveType) {
            return MableAstFactory.newARealNumericPrimitiveType();
        } else if (right instanceof ARealNumericPrimitiveType) {
            return MableAstFactory.newARealNumericPrimitiveType();
        } else if (left instanceof AUIntNumericPrimitiveType) {
            return MableAstFactory.newAUIntNumericPrimitiveType();
        } else if (right instanceof AUIntNumericPrimitiveType) {
            return MableAstFactory.newAUIntNumericPrimitiveType();
        } else if (left instanceof AIntNumericPrimitiveType) {
            return MableAstFactory.newAIntNumericPrimitiveType();
        } else if (right instanceof AIntNumericPrimitiveType) {
            return MableAstFactory.newAIntNumericPrimitiveType();
        }
        return null;
    }

    @Override
    public PType caseAMinusBinaryExp(AMinusBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkNumeric(node, ctxt));
    }

    @Override
    public PType caseABoolLiteralExp(ABoolLiteralExp node, Context ctxt) throws AnalysisException {
        return store(node, MableAstFactory.newABoleanPrimitiveType());
    }

    @Override
    public PType caseAStringLiteralExp(AStringLiteralExp node, Context ctxt) throws AnalysisException {
        return store(node, MableAstFactory.newAStringPrimitiveType());
    }

    @Override
    public PType caseARealLiteralExp(ARealLiteralExp node, Context ctxt) throws AnalysisException {

        double value = node.getValue();
        if (Math.round(value) == value) {
            if (value < 0) {
                return store(node, MableAstFactory.newIntType());
            } else if (value == 0) {

                //nat
                return store(node, MableAstFactory.newIntType());
            } else {
                //natone
                return store(node, MableAstFactory.newIntType());
            }
        } else {
            return store(node, MableAstFactory.newRealType());  // Note, "1.234" is really "1234/1000" (a rat)
        }

    }

    @Override
    public PType caseAUIntLiteralExp(AUIntLiteralExp node, Context ctxt) throws AnalysisException {
        long value = node.getValue();

        if (value < 0 || value < Integer.MAX_VALUE) {
            return store(node, MableAstFactory.newIntType());
        }
        return store(node, MableAstFactory.newUIntType());

    }

    @Override
    public PType caseAIntLiteralExp(AIntLiteralExp node, Context ctxt) throws AnalysisException {
        int value = node.getValue();
        if (value < 0) {
            return store(node, MableAstFactory.newIntType());
        } else if (value == 0) {
            //nat
            return store(node, MableAstFactory.newIntType());
        } else {
            //natone
            return store(node, MableAstFactory.newIntType());
        }
    }

    @Override
    public PType caseAIdentifierExp(AIdentifierExp node, Context ctxt) throws AnalysisException {
        PDeclaration def = ctxt.findDeclaration(node.getName());

        if (def == null) {
            errorReporter.report(0, "Use of undeclared identifier", node.getName().getSymbol());
            return store(node, newAUnknownType());
        }

        return store(node, checkedTypes.get(def));
    }

    @Override
    public PType caseAFunctionDeclaration(AFunctionDeclaration node, Context info) throws AnalysisException {
        // TODO: Check that function does not already exist
        AFunctionType type = new AFunctionType();
        PType resultType = node.getReturnType().apply(this, info);
        type.setResult(resultType);
        if (node.getFormals() != null && node.getFormals().size() > 0) {
            List<PType> functionParameters = new ArrayList<>();
            for (AFormalParameter formalParameter : node.getFormals()) {
                PType parameterType = formalParameter.getType().apply(this, info);
                functionParameters.add(parameterType);
            }
            type.setParameters(functionParameters);
        }

        return store(node, type);
    }

    @Override
    public PType caseARootDocument(ARootDocument node, Context ctxt) throws AnalysisException {
        for (INode node_ : node.getContent()) {
            node_.apply(this, ctxt);
        }

        return store(node, newAVoidType());
    }

    @Override
    public PType caseASimulationSpecificationCompilationUnit(ASimulationSpecificationCompilationUnit node, Context ctxt) throws AnalysisException {

        ModulesContext visibleModulesContext = new ModulesContext(getAccessibleModulesContext(node.getImports(), ctxt), ctxt);
        node.getBody().apply(this, visibleModulesContext);
        return store(node, newAVoidType());
    }

    @Override
    public PType caseAImportedModuleCompilationUnit(AImportedModuleCompilationUnit node, Context ctxt) throws AnalysisException {
        ModulesContext visibleModulesContext = new ModulesContext(getAccessibleModulesContext(node.getImports(), ctxt), ctxt);
        node.getModule().apply(this, visibleModulesContext);
        return store(node, newAVoidType());
    }

    @Override
    public PType caseAModuleDeclaration(AModuleDeclaration node, Context ctxt) throws AnalysisException {

        PDeclaration decl = ctxt.findDeclaration(node.getName());
        if (decl != null) {
            errorReporter.report(0, "Module dublicates: " + node.getName().getText() + " dublicates: " + decl.getName().getText(),
                    node.getName().getSymbol());
        }

        for (AFunctionDeclaration funDecl : node.getFunctions()) {
            funDecl.apply(this, ctxt);
        }
        return store(node, newAModuleType((LexIdentifier) node.getName().clone()));
    }

    private List<AModuleDeclaration> getAccessibleModulesContext(List<? extends LexIdentifier> imports, Context ctxt) {
        List<AModuleDeclaration> importedModules = new Vector<>();
        for (LexIdentifier importName : imports) {
            PDeclaration decl = ctxt.findGlobalDeclaration(importName);
            if (decl == null) {
                errorReporter.report(0, "Imported decleration: '" + importName.getText() + "' not in scope", importName.getSymbol());
            } else {
                if (decl instanceof AModuleDeclaration) {
                    importedModules.add((AModuleDeclaration) decl);

                    AImportedModuleCompilationUnit importedUnit = decl.getAncestor(AImportedModuleCompilationUnit.class);
                    if (importedUnit != null) {
                        //recursive add imports
                        importedModules.addAll(getAccessibleModulesContext(importedUnit.getImports(), ctxt));
                    } else {
                        errorReporter.report(0, "Module is not in a import unit", decl.getName().getSymbol());
                    }
                } else {
                    errorReporter.report(0, "Imported module is not a module", importName.getSymbol());
                }
            }
        }

        return importedModules;
    }

    @Override
    public PType caseABlockStm(ABlockStm node, Context ctxt) throws AnalysisException {

        DeclarationList tdm = new DeclarationList();
        Context localCtxt = new LocalContext(tdm, ctxt);
        for (INode bodyNode : node.getBody()) {
            if (bodyNode instanceof ALocalVariableStm) {
                ALocalVariableStm stm = (ALocalVariableStm) bodyNode;
                if (stm.getDeclaration() != null) {
                    //type is added in the global cache during var decl check
                    /*PType type =*/
                    stm.getDeclaration().apply(this, localCtxt);
                    tdm.add(stm.getDeclaration());
                }
            } else {
                bodyNode.apply(this, localCtxt);
            }

        }

        return store(node, MableAstFactory.newAVoidType());
    }

    @Override
    public PType caseALocalVariableStm(ALocalVariableStm node, Context ctxt) throws AnalysisException {
        PType type = node.getDeclaration().apply(this, ctxt);
        return store(node, MableAstFactory.newAVoidType());
    }

    @Override
    public PType caseAParExp(AParExp node, Context ctxt) throws AnalysisException {
        PType expType = node.getExp().apply(this, ctxt);

        return store(node, expType);
    }

    @Override
    public PType caseAWhileStm(AWhileStm node, Context ctxt) throws AnalysisException {
        PType testType = node.getTest().apply(this, ctxt);
        if (!(testType instanceof ABooleanPrimitiveType)) {
            errorReporter.report(-5, "While condition is not of type bool: " + node, null);
        }
        node.getBody().apply(this, ctxt);

        return store(node, MableAstFactory.newAVoidType());
    }

    @Override
    public PType caseAAssigmentStm(AAssigmentStm node, Context ctxt) throws AnalysisException {
        PType expType = node.getExp().apply(this, ctxt);
        PType type = node.getTarget().apply(this, ctxt);
        if (!typeComparator.compatible(type, expType)) {
            errorReporter.report(-5, "Invalid assignment to: " + node.getTarget() + " from:" + node.getExp(), null);

        }
        return store(node, MableAstFactory.newAVoidType());
    }


    @Override
    public PType caseAIdentifierStateDesignator(AIdentifierStateDesignator node, Context ctxt) throws AnalysisException {
        PDeclaration def = ctxt.findDeclaration(node.getName());

        if (def == null) {
            errorReporter.report(0, "Use of undeclared variable", node.getName().getSymbol());
        }
        return store(node, checkedTypes.get(def));
    }

    @Override
    public PType caseAIfStm(AIfStm node, Context ctxt) throws AnalysisException {
        if (node.getTest() != null) {
            PType testType = node.getTest().apply(this, ctxt);
            if (!(typeComparator.compatible(newBoleanType(), testType))) {
                errorReporter.report(-5, "If condition is not of type bool: " + node, null);
            }
        }
        if (node.getThen() != null) {
            node.getThen().apply(this, ctxt);
        }
        if (node.getElse() != null) {
            node.getElse().apply(this, ctxt);
        }
        return store(node, MableAstFactory.newAVoidType());
    }

    @Override
    public PType caseAEqualBinaryExp(AEqualBinaryExp node, Context ctxt) throws AnalysisException {
        PType left = node.getLeft().apply(this, ctxt);
        PType right = node.getRight().apply(this, ctxt);
        if (!typeComparator.compatible(left, right)) {
            errorReporter.report(-5, "Left and right part of == expression are not compatible: " + node, null);

        }
        return store(node, MableAstFactory.newABoleanPrimitiveType());
    }

    @Override
    public PType caseAExpressionStm(AExpressionStm node, Context ctxt) throws AnalysisException {
        node.getExp().apply(this, ctxt);
        return store(node, MableAstFactory.newAVoidType());
    }

    @Override
    public PType caseABreakStm(ABreakStm node, Context ctxt) throws AnalysisException {

        if (node.getAncestor(AWhileStm.class) == null) {
            errorReporter.report(0, "Break statement only allowed inside a while", node.getToken());
        }

        return store(node, MableAstFactory.newAVoidType());
    }

    @Override
    public PType caseAArrayStateDesignator(AArrayStateDesignator node, Context ctxt) throws AnalysisException {
        PType indexType = node.getExp().apply(this, ctxt);
        if (!(indexType instanceof AIntNumericPrimitiveType)) {
            errorReporter.report(-5, "Index has to be of int type." + node, null);
        }
        // Peel of the array type
        PType targetType = node.getTarget().apply(this, ctxt);
        if (targetType instanceof AArrayType) {
            return store(node, ((AArrayType) targetType).getType());
        } else {
            errorReporter.report(-5, "Attempt to index into a variable of non-array type:" + node, null);
            return store(node, targetType);
        }
    }

    public PType checkLogicialBinary(SBinaryExp node, Context ctxt) throws AnalysisException {
        PType left = node.getLeft().apply(this, ctxt);
        if (!(left instanceof ABooleanPrimitiveType)) {
            errorReporter.report(-5, "Expected lvalue to be bool actual:" + left, null);
        }
        PType right = node.getLeft().apply(this, ctxt);
        if (!(right instanceof ABooleanPrimitiveType)) {
            errorReporter.report(-5, "Expected rvalue to be bool actual:" + right, null);
        }
        return store(node, MableAstFactory.newABoleanPrimitiveType());
    }

    @Override
    public PType caseAAndBinaryExp(AAndBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkLogicialBinary(node, ctxt));
    }

    @Override
    public PType caseAOrBinaryExp(AOrBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkLogicialBinary(node, ctxt));
    }

    @Override
    public PType caseALessEqualBinaryExp(ALessEqualBinaryExp node, Context ctxt) throws AnalysisException {
        PType left = node.getLeft().apply(this, ctxt);
        if (!(left instanceof SNumericPrimitiveType)) {
            errorReporter.report(-5, "Left part of Less Equal expression is not of numeric type:" + node.getLeft(), null);
        }
        PType right = node.getRight().apply(this, ctxt);
        if (!(right instanceof SNumericPrimitiveType)) {
            errorReporter.report(-5, "Right part of Less Equal expression is not of numeric type:" + node.getRight(), null);
        }
        return store(node, MableAstFactory.newABoleanPrimitiveType());
    }

    @Override
    public PType caseALessBinaryExp(ALessBinaryExp node, Context ctxt) throws AnalysisException {
        PType left = node.getLeft().apply(this, ctxt);
        if (!(left instanceof SNumericPrimitiveType)) {
            errorReporter.report(-5, "Left part of Less expression is not of numeric type:" + node, null);
        }
        PType right = node.getRight().apply(this, ctxt);
        if (!(right instanceof SNumericPrimitiveType)) {
            errorReporter.report(-5, "Right part of Less expression is not of numeric type:" + node, null);
        }
        return store(node, MableAstFactory.newABoleanPrimitiveType());
    }

    @Override
    public PType caseANotEqualBinaryExp(ANotEqualBinaryExp node, Context ctxt) throws AnalysisException {
        PType left = node.getLeft().apply(this, ctxt);
        PType right = node.getRight().apply(this, ctxt);
        if (!typeComparator.compatible(left, right)) {
            errorReporter.report(-5, "Left and right part of != expression are not compatible: " + node, null);

        }
        return store(node, MableAstFactory.newABoleanPrimitiveType());
    }

    @Override
    public PType caseANotUnaryExp(ANotUnaryExp node, Context ctxt) throws AnalysisException {
        PType expType = node.getExp().apply(this, ctxt);
        if (!(expType instanceof ABooleanPrimitiveType)) {
            errorReporter.report(-5, "Expression used with ! has to be of type bool: " + node, null);
        }
        return store(node, MableAstFactory.newABoleanPrimitiveType());
    }

    @Override
    public PType caseAUnloadExp(AUnloadExp node, Context ctxt) throws AnalysisException {
        if (node.getArgs() == null || node.getArgs().size() != 1) {
            errorReporter.report(-5, "Wrong number of arguments to Unload. Unload accepts 1 argument: " + node, null);
        } else {
            PType argType = node.getArgs().get(0).apply(this, ctxt);
            if (!(argType instanceof AModuleType)) {
                errorReporter.report(-5, "Argument to unload must be a moduletype.: " + node, null);

            }
        }
        //TODO void?
        return store(node, newAVoidType());
    }

    public void typecheck(List<ARootDocument> rootDocuments, List<? extends PDeclaration> globalFunctions) throws AnalysisException {

        if (globalFunctions == null) {
            globalFunctions = new Vector<>();
        }

        // Find all importedModuleCompilationUnits and typecheck these twice.
        List<AImportedModuleCompilationUnit> importedModuleUnits = rootDocuments.stream()
                .map(x -> x.getContent().stream().filter(AImportedModuleCompilationUnit.class::isInstance)
                        .map(AImportedModuleCompilationUnit.class::cast)).flatMap(Function.identity()).collect(Collectors.toList());
        //        final List<AImportedModuleCompilationUnit> modules = new Vector<>();
        //
        //
        //        for (ARootDocument module : allModules) {
        //            for (PCompilationUnit singleModule : module.getContent()) {
        //                AImportedModuleCompilationUnit importedModule = (AImportedModuleCompilationUnit) singleModule;
        //                modules.add(importedModule);
        //            }
        //        }


        List<AModuleDeclaration> importedModules =
                importedModuleUnits.stream().map(AImportedModuleCompilationUnit::getModule).collect(Collectors.toList());
        final GlobalContext ctx = new GlobalContext(importedModules, null);

        final QuestionAnswerAdaptor<Context, PType> typeCheckVisitor = this;

        //check all modules imported imported
        //TODO why are we adding this type here?
        for (ARootDocument doc : rootDocuments) {
            doc.apply(new DepthFirstAnalysisAdaptor() {
                @Override
                public void caseAModuleDeclaration(AModuleDeclaration node) throws AnalysisException {
                    checkedTypes.put(node, newAModuleType(node.getName()));
                }
            });
        }

        //check all imported modules
        for (ARootDocument doc : rootDocuments) {
            for (PCompilationUnit unit : doc.getContent()) {
                if (unit instanceof AImportedModuleCompilationUnit) {
                    unit.apply(typeCheckVisitor, ctx);
                }
            }
            //            for(int )
            //            doc.apply(new DepthFirstAnalysisAdaptor() {
            //                @Override
            //                public void caseAFunctionDeclaration(AFunctionDeclaration node) throws AnalysisException {
            //                    checkedTypes.put(node, node.apply(typeCheckVisitor, allModulesCtxt));
            //                }
            //            });
        }

        ModulesContext allModulesCtxt = new ModulesContext(importedModules, ctx);
        for (PDeclaration func : globalFunctions) {
            func.apply(typeCheckVisitor, allModulesCtxt);
        }

        //add all global functions as local functions
        LocalContext globalContext = new LocalContext(globalFunctions, ctx);

        List<ARootDocument> allSimulationSpecifications =
                rootDocuments.stream().filter(x -> x.getContent().stream().anyMatch(ASimulationSpecificationCompilationUnit.class::isInstance))
                        .collect(Collectors.toList());

        if (allSimulationSpecifications.size() != 1) {
            errorReporter.report(-5, "1 simulation specification is allowed. Found: " + allSimulationSpecifications.size(), null);
        } else {
            allSimulationSpecifications.get(0).apply(this, globalContext);
        }
    }

}
