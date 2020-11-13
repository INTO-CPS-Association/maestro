package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.QuestionAnswerAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.InternalException;
import org.intocps.maestro.core.messages.IErrorReporter;

import java.util.*;
import java.util.stream.Collectors;

public class TypeCheckVisitor extends QuestionAnswerAdaptor<TypeCheckInfo, PType> {
    private final IErrorReporter errorReporter;
    TypeComparator typeComparator;
    MableAstFactory astFactory;

    public TypeCheckVisitor(IErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
        this.typeComparator = new TypeComparator();
        astFactory = new MableAstFactory();
    }

    @Override
    public PType createNewReturnValue(INode node, TypeCheckInfo info) throws AnalysisException {
        return null;
    }

    @Override
    public PType createNewReturnValue(Object node, TypeCheckInfo info) throws AnalysisException {
        return null;
    }

    @Override
    public PType defaultPInitializer(PInitializer node, TypeCheckInfo question) throws AnalysisException {
        return super.defaultPInitializer(node, question);
    }

    @Override
    public PType defaultPType(PType node, TypeCheckInfo question) throws AnalysisException {
        return node.clone();
    }

    @Override
    public PType caseAArrayType(AArrayType node, TypeCheckInfo question) throws AnalysisException {
        return MableAstFactory.newAArrayType(node.getType().apply(this, question));
    }

    @Override
    public PType caseAExpInitializer(AExpInitializer node, TypeCheckInfo question) throws AnalysisException {
        return node.getExp().apply(this, question);
    }


    @Override
    public PType caseAArrayIndexExp(AArrayIndexExp node, TypeCheckInfo question) throws AnalysisException {
        PType type = node.getArray().apply(this, question);
        // Since we a indexeing, we need to step out a level from AArrayType
        if (type instanceof AArrayType) {
            return ((AArrayType) type).getType().clone();
        } else {
            errorReporter.report(-5, "Failed to get inner type of Array at node: " + node, null);
        }
        return MableAstFactory.newAUnknownType();
    }

    @Override
    public PType caseALoadExp(ALoadExp node, TypeCheckInfo question) throws AnalysisException {
        //TODO: Needs work in terms of load type.
        // See https://github.com/INTO-CPS-Association/maestro/issues/66
        // Return whatever type, such that variable declaration decides.
        return astFactory.newAUnknownType();
    }

    @Override
    public PType caseAArrayInitializer(AArrayInitializer node, TypeCheckInfo question) throws AnalysisException {
        PType arrayType = astFactory.newAUnknownType();
        // If the type of the elements are not the same, then return unknown type.
        if (node.getExp().size() > 0) {
            PType type = null;
            for (PExp exp : node.getExp()) {
                PType expType = exp.apply(this, question);
                if (type == null) {
                    type = expType;
                } else {
                    if (!(type instanceof AUnknownType) && !typeComparator.compatible(type, expType)) {
                        type = astFactory.newAUnknownType();
                    }
                }
            }
            arrayType = MableAstFactory.newAArrayType(type);
        }
        return arrayType;
    }

    @Override
    public PType caseACallExp(ACallExp node, TypeCheckInfo question) throws AnalysisException {
        if (node.getObject() != null) {
            // This is a module
            // Ensure that the object is of module type
            PType objectType = node.getObject().apply(this, question);
            if (objectType instanceof AModuleType) {
                LexIdentifier moduleName = ((AModuleType) objectType).getName().getName();
                TypeDefinitionMap moduleDeclarations = question.getTypeCheckerContext().findModuleDeclarations(moduleName);
                //                PDeclaration moduleFunction = question.findModuleFunction(((AModuleType) objectType).getName().getName(), node.getMethodName());

                //                PType moduleFunctionType = moduleFunction.apply(this, question);
                PType moduleFunctionType = moduleDeclarations.getType(node.getMethodName());
                if (moduleFunctionType instanceof AFunctionType) {
                    PType moduleFunctionReturnType = (((AFunctionType) moduleFunctionType).getResult()).apply(this, question);

                    List<PType> callArgs = new LinkedList<>();
                    for (PExp arg : node.getArgs()) {
                        callArgs.add(arg.apply(this, question));
                    }
                    if (!typeComparator.compatible(((AFunctionType) moduleFunctionType).getParameters(), callArgs)) {
                        errorReporter.report(-5, "Function call type does not match with application for node: " + node, null);
                    }
                    return moduleFunctionReturnType;

                }
            }
        }
        return null;
    }

    @Override
    public PType caseANameType(ANameType node, TypeCheckInfo question) throws AnalysisException {
        PType type = question.getTypeCheckerContext().findModuleType(node.getName());
        if (type == null) {
            errorReporter.report(-5, "Use of undeclared identifier: " + node.getName() + ". Did you forgot to include a module?", null);
        } else {
            return type;
        }
        return astFactory.newAUnknownType();
    }

    @Override
    public PType defaultSNumericPrimitiveType(SNumericPrimitiveType node, TypeCheckInfo question) throws AnalysisException {
        return node.clone();
    }

    @Override
    public PType caseABooleanPrimitiveType(ABooleanPrimitiveType node, TypeCheckInfo question) throws AnalysisException {
        return node.clone();
    }


    @Override
    public PType caseAVariableDeclaration(AVariableDeclaration node, TypeCheckInfo question) throws AnalysisException {
        PType variableType = node.getType().apply(this, question);
        // TODO: Fix after https://github.com/INTO-CPS-Association/maestro/issues/157
        if ((node.getIsArray() != null && node.getIsArray()) && !(variableType instanceof AArrayType)) {
            AArrayType type = MableAstFactory.newAArrayType(variableType.clone());
            if (node.getSize() != null) {
                type.setSize(node.getSize().size());
            }
            variableType = type;
        }
        if (variableType == null) {
            errorReporter.report(-5, "Failed to retrieve the type for node: " + node, null);
        } else {
            if (node.getInitializer() != null) {
                PType initializerType = node.getInitializer().apply(this, question);
                if (initializerType != null) {
                    if (!typeComparator.compatible(variableType, initializerType)) {
                        errorReporter.report(-5, "Node type and initializer type does not match for node: " + node, null);
                    }
                } else {
                    errorReporter.report(-5, "Initializer type could not be retrieved for node: " + node, null);
                }
            }
        }
        return variableType;
    }

    @Override
    public PType defaultPExp(PExp node, TypeCheckInfo question) throws AnalysisException {
        throw new InternalException(-5, "Node unknown to typechecker: " + node);
    }

    @Override
    public PType defaultPStm(PStm node, TypeCheckInfo question) throws AnalysisException {
        throw new InternalException(-5, "Node unknown to typechecker: " + node);
    }

    @Override
    public PType caseAPlusBinaryExp(APlusBinaryExp node, TypeCheckInfo question) throws AnalysisException {

        //fixme: need to call type narrow, left + right

        return node.getLeft().apply(this, question);
    }

    @Override
    public PType caseAMinusBinaryExp(AMinusBinaryExp node, TypeCheckInfo question) throws AnalysisException {
        //fixme: need to call type narrow, left + right

        PType leftType = node.getLeft().apply(this, question);
        PType rightType = node.getRight().apply(this, question);
        if (!typeComparator.compatible(leftType, rightType)) {
            errorReporter.report(-5, "Left type: " + node.getLeft() + " - does not align with right type: " + node.getRight(), null);
        }
        // Todo: return "higher" type, i.e. real - int is real. int - real is real.

        return node.getLeft().apply(this, question);
    }

    @Override
    public PType caseABoolLiteralExp(ABoolLiteralExp node, TypeCheckInfo question) throws AnalysisException {
        return astFactory.newABoleanPrimitiveType();
    }

    @Override
    public PType caseAStringLiteralExp(AStringLiteralExp node, TypeCheckInfo question) throws AnalysisException {
        return astFactory.newAStringPrimitiveType();
    }

    @Override
    public PType caseARealLiteralExp(ARealLiteralExp node, TypeCheckInfo question) throws AnalysisException {
        return astFactory.newARealNumericPrimitiveType();
    }

    @Override
    public PType caseAUIntLiteralExp(AUIntLiteralExp node, TypeCheckInfo question) throws AnalysisException {
        return astFactory.newAUIntNumericPrimitiveType();
    }

    @Override
    public PType caseAIntLiteralExp(AIntLiteralExp node, TypeCheckInfo question) throws AnalysisException {
        return astFactory.newAIntNumericPrimitiveType();
    }

    @Override
    public PType caseAIdentifierExp(AIdentifierExp node, TypeCheckInfo question) throws AnalysisException {
        return question.getTypeCheckerContext().findDefinitionType(node.getName());
    }

    @Override
    public PType caseAFunctionDeclaration(AFunctionDeclaration node, TypeCheckInfo info) throws AnalysisException {
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
        return type;
    }

    @Override
    public PType caseARootDocument(ARootDocument node, TypeCheckInfo question) throws AnalysisException {
        for (INode node_ : node.getContent()) {
            node_.apply(this, question);
        }

        return null;
    }

    @Override
    public PType caseASimulationSpecificationCompilationUnit(ASimulationSpecificationCompilationUnit node,
            TypeCheckInfo question) throws AnalysisException {
        node.getBody().apply(this, question);
        return null;
    }

    private void updateModuleInterDependencies(Map<LexIdentifier, ModuleEnvironment> modules, TypeCheckInfo question) throws AnalysisException {
        for (Map.Entry<LexIdentifier, ModuleEnvironment> module : modules.entrySet()) {
            for (PDeclaration decl : module.getValue().definitions) {
                PType type = decl.apply(this, question);
                if (decl instanceof AFunctionDeclaration && type instanceof AFunctionType) {
                    AFunctionDeclaration functionDecl = ((AFunctionDeclaration) decl);
                    AFunctionType functionType = (AFunctionType) type;
                    functionDecl.setReturnType(functionType.getResult());
                    if (functionType.getParameters() != null) {
                        for (int i = 0; i < functionType.getParameters().size(); i++) {
                            PType parameterType = functionType.getParameters().get(i);
                            functionDecl.getFormals().get(i).setType(parameterType);
                        }
                    }
                }
            }
        }
    }

    @Override
    public PType caseABlockStm(ABlockStm node, TypeCheckInfo question) throws AnalysisException {

        TypeDefinitionMap tdm = new TypeDefinitionMap();
        TypeCheckInfo info = new TypeCheckInfo(new TypeCheckerContext(tdm, question.getTypeCheckerContext()));
        for (INode bodyNode : node.getBody()) {
            if (bodyNode instanceof ALocalVariableStm) {
                ALocalVariableStm stm = (ALocalVariableStm) bodyNode;
                if (stm.getDeclaration() != null) {
                    PType type = stm.getDeclaration().apply(this, info);
                    tdm.add(stm.getDeclaration(), type);
                }
            } else {
                bodyNode.apply(this, info);
            }

        }

        return MableAstFactory.newAVoidType();
    }

    @Override
    public PType caseALocalVariableStm(ALocalVariableStm node, TypeCheckInfo question) throws AnalysisException {
        PType type = node.getDeclaration().apply(this, question);
        return MableAstFactory.newAVoidType();
    }

    @Override
    public PType caseAParExp(AParExp node, TypeCheckInfo question) throws AnalysisException {
        PType expType = node.getExp().apply(this, question);

        return expType;
    }

    @Override
    public PType caseAWhileStm(AWhileStm node, TypeCheckInfo question) throws AnalysisException {
        PType testType = node.getTest().apply(this, question);
        if (!(testType instanceof ABooleanPrimitiveType)) {
            errorReporter.report(-5, "While condition is not of type bool: " + node, null);
        }
        node.getBody().apply(this, question);

        return MableAstFactory.newAVoidType();
    }

    @Override
    public PType caseAAssigmentStm(AAssigmentStm node, TypeCheckInfo question) throws AnalysisException {
        PType expType = node.getExp().apply(this, question);
        PType type = node.getTarget().apply(this, question);
        if (!typeComparator.compatible(type, expType)) {
            errorReporter.report(-5, "Invalid assignment to: " + node.getTarget() + " from:" + node.getExp(), null);

        }
        return MableAstFactory.newAVoidType();
    }

    @Override
    public PType caseAIdentifierStateDesignator(AIdentifierStateDesignator node, TypeCheckInfo question) throws AnalysisException {
        return question.getTypeCheckerContext().findDefinitionType(node.getName());
    }

    @Override
    public PType caseAIfStm(AIfStm node, TypeCheckInfo question) throws AnalysisException {
        if (node.getTest() != null) {
            PType testType = node.getTest().apply(this, question);
            if (!(testType instanceof ABooleanPrimitiveType)) {
                errorReporter.report(-5, "If condition is not of type bool: " + node, null);
            }
        }
        if (node.getThen() != null) {
            node.getThen().apply(this, question);
        }
        if (node.getElse() != null) {
            node.getElse().apply(this, question);
        }
        return MableAstFactory.newAVoidType();
    }

    @Override
    public PType caseAOrBinaryExp(AOrBinaryExp node, TypeCheckInfo question) throws AnalysisException {
        PType left = node.getLeft().apply(this, question);
        if (!(left instanceof ABooleanPrimitiveType)) {
            errorReporter.report(-5, "Left part of or expression is not of type bool:" + node.getLeft(), null);
        }
        PType right = node.getLeft().apply(this, question);
        if (!(right instanceof ABooleanPrimitiveType)) {
            errorReporter.report(-5, "Right part of or expression is not of type bool:" + node.getRight(), null);
        }
        return MableAstFactory.newABoleanPrimitiveType();
    }

    @Override
    public PType caseAEqualBinaryExp(AEqualBinaryExp node, TypeCheckInfo question) throws AnalysisException {
        PType left = node.getLeft().apply(this, question);
        PType right = node.getRight().apply(this, question);
        if (!typeComparator.compatible(left, right)) {
            errorReporter.report(-5, "Left and right part of == expression are not compatible: " + node, null);

        }
        return MableAstFactory.newABoleanPrimitiveType();
    }

    @Override
    public PType caseAExpressionStm(AExpressionStm node, TypeCheckInfo question) throws AnalysisException {
        node.getExp().apply(this, question);
        return MableAstFactory.newAVoidType();
    }

    @Override
    public PType caseABreakStm(ABreakStm node, TypeCheckInfo question) throws AnalysisException {
        return MableAstFactory.newAVoidType();
    }

    @Override
    public PType caseAArrayStateDesignator(AArrayStateDesignator node, TypeCheckInfo question) throws AnalysisException {
        PType indexType = node.getExp().apply(this, question);
        if (!(indexType instanceof AIntNumericPrimitiveType)) {
            errorReporter.report(-5, "Index has to be of int type." + node, null);
        }
        // Peel of the array type
        PType targetType = node.getTarget().apply(this, question);
        if (targetType instanceof AArrayType) {
            return ((AArrayType) targetType).getType();
        } else {
            errorReporter.report(-5, "Attempt to index into a variable of non-array type:" + node, null);
            return targetType;
        }
    }

    @Override
    public PType caseAAndBinaryExp(AAndBinaryExp node, TypeCheckInfo question) throws AnalysisException {
        PType left = node.getLeft().apply(this, question);
        if (!(left instanceof ABooleanPrimitiveType)) {
            errorReporter.report(-5, "Left part of And expression is not of type bool:" + node.getLeft(), null);
        }
        PType right = node.getLeft().apply(this, question);
        if (!(right instanceof ABooleanPrimitiveType)) {
            errorReporter.report(-5, "Right part of And expression is not of type bool:" + node.getRight(), null);
        }
        return MableAstFactory.newABoleanPrimitiveType();
    }

    @Override
    public PType caseALessEqualBinaryExp(ALessEqualBinaryExp node, TypeCheckInfo question) throws AnalysisException {
        PType left = node.getLeft().apply(this, question);
        if (!(left instanceof SNumericPrimitiveType)) {
            errorReporter.report(-5, "Left part of Less Equal expression is not of numeric type:" + node.getLeft(), null);
        }
        PType right = node.getRight().apply(this, question);
        if (!(right instanceof SNumericPrimitiveType)) {
            errorReporter.report(-5, "Right part of Less Equal expression is not of numeric type:" + node.getRight(), null);
        }
        return MableAstFactory.newABoleanPrimitiveType();
    }

    @Override
    public PType caseALessBinaryExp(ALessBinaryExp node, TypeCheckInfo question) throws AnalysisException {
        PType left = node.getLeft().apply(this, question);
        if (!(left instanceof SNumericPrimitiveType)) {
            errorReporter.report(-5, "Left part of Less expression is not of numeric type:" + node, null);
        }
        PType right = node.getRight().apply(this, question);
        if (!(right instanceof SNumericPrimitiveType)) {
            errorReporter.report(-5, "Right part of Less expression is not of numeric type:" + node, null);
        }
        return MableAstFactory.newABoleanPrimitiveType();
    }

    @Override
    public PType caseANotEqualBinaryExp(ANotEqualBinaryExp node, TypeCheckInfo question) throws AnalysisException {
        PType left = node.getLeft().apply(this, question);
        PType right = node.getRight().apply(this, question);
        if (!typeComparator.compatible(left, right)) {
            errorReporter.report(-5, "Left and right part of != expression are not compatible: " + node, null);

        }
        return MableAstFactory.newABoleanPrimitiveType();
    }

    @Override
    public PType caseANotUnaryExp(ANotUnaryExp node, TypeCheckInfo question) throws AnalysisException {
        PType expType = node.getExp().apply(this, question);
        if (!(expType instanceof ABooleanPrimitiveType)) {
            errorReporter.report(-5, "Expression used with ! has to be of type bool: " + node, null);
        }
        return MableAstFactory.newABoleanPrimitiveType();
    }

    @Override
    public PType caseAUnloadExp(AUnloadExp node, TypeCheckInfo question) throws AnalysisException {
        if (node.getArgs() == null || node.getArgs().size() != 1) {
            errorReporter.report(-5, "Wrong number of arguments to Unload. Unload accepts 1 argument: " + node, null);
        } else {
            PType argType = node.getArgs().get(0).apply(this, question);
            if (!(argType instanceof AModuleType)) {
                errorReporter.report(-5, "Argument to unload must be a moduletype.: " + node, null);

            }
        }

        return null;
    }

    public void typecheck(List<ARootDocument> rootDocuments) throws AnalysisException {

        // Find all importedModuleCompilationUnits and typecheck these twice.
        List<ARootDocument> allModules =
                rootDocuments.stream().filter(x -> x.getContent().stream().anyMatch(y -> y instanceof AImportedModuleCompilationUnit))
                        .collect(Collectors.toList());
        final Map<AImportedModuleCompilationUnit, TypeDefinitionMap> modules = new HashMap<>();

        for (ARootDocument module : allModules) {
            for (PCompilationUnit singleModule : module.getContent()) {
                AImportedModuleCompilationUnit singleModule_ = (AImportedModuleCompilationUnit) singleModule;
                modules.put(singleModule_, new TypeDefinitionMap(new ArrayList<>(), new HashMap<>()));
            }
        }
        ModulesContext ctx =
                new ModulesContext(modules.entrySet().stream().collect(Collectors.toMap(m -> m.getKey().getName(), v -> v.getValue())), null);
        TypeCheckInfo info = new TypeCheckInfo(ctx);
        for (Map.Entry<AImportedModuleCompilationUnit, TypeDefinitionMap> moduleEntry : modules.entrySet()) {
            Map<AFunctionDeclaration, PType> functionDeclarationToType = new HashMap<>();
            for (AFunctionDeclaration functionDeclaration : moduleEntry.getKey().getFunctions()) {
                PType type = functionDeclaration.apply(this, info);
                functionDeclarationToType.put(functionDeclaration, type);
            }
            functionDeclarationToType.forEach((def, type) -> {
                moduleEntry.getValue().add(def, type);
            });
        }


        List<ARootDocument> allSimulationSpecifications =
                rootDocuments.stream().filter(x -> x.getContent().stream().anyMatch(y -> y instanceof ASimulationSpecificationCompilationUnit))
                        .collect(Collectors.toList());

        if (allSimulationSpecifications.size() != 1) {
            errorReporter.report(-5, "1 simulation specification is allowed. Found: " + allSimulationSpecifications.size(), null);
        } else {
            allSimulationSpecifications.get(0).apply(this, info);
        }
    }
}
