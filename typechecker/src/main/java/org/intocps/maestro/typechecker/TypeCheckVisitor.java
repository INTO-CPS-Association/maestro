package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.QuestionAnswerAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.messages.IErrorReporter;

import java.util.LinkedList;
import java.util.List;
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
        return node;
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
            return ((AArrayType) type).getType();
        } else {
            errorReporter.report(-5, "Failed to get inner type of Array at node: " + node, null);
        }
        return node.getType();
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
                PType expType;
                if (exp.getType() != null) {
                    expType = exp.getType().apply(this, question);
                } else {
                    expType = exp.apply(this, question);
                }
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
                PDeclaration moduleFunction = question.findModuleFunction((AModuleType) objectType, node.getMethodName());
                PType moduleFunctionType = moduleFunction.apply(this, question);
                if (moduleFunctionType instanceof AFunctionType) {
                    PType moduleFunctionReturnType = (((AFunctionType) moduleFunctionType).getResult()).apply(this, question);

                    List<PType> callArgs = new LinkedList<>();
                    for (PExp arg : node.getArgs()) {
                        callArgs.add(arg.apply(this, question));
                    }
                    if (!typeComparator.compatible(((AFunctionType) moduleFunctionType).getParameters(), callArgs)) {
                        errorReporter.report(-5, "Function call type does not match with application for node" + node, null);
                    }
                    return moduleFunctionReturnType;

                }
            }
        }
        return null;
    }


    @Override
    public PType caseANameType(ANameType node, TypeCheckInfo question) throws AnalysisException {
        PType type = question.findModule(node.getName());
        if (type == null) {
            errorReporter.report(-5, "Use of undeclared identifier: " + node.getName() + ". Did you forgot to include a module?", null);
        } else {
            return type;
        }
        return astFactory.newAUnknownType();
    }

    @Override
    public PType defaultSNumericPrimitiveType(SNumericPrimitiveType node, TypeCheckInfo question) throws AnalysisException {
        return node;
    }

    @Override
    public PType caseABooleanPrimitiveType(ABooleanPrimitiveType node, TypeCheckInfo question) throws AnalysisException {
        return node;
    }


    @Override
    public PType caseAVariableDeclaration(AVariableDeclaration node, TypeCheckInfo question) throws AnalysisException {
        PType variableType = node.getType().apply(this, question);
        // TODO: Fix after https://github.com/INTO-CPS-Association/maestro/issues/157
        if (node.getIsArray() && !(variableType instanceof AArrayType)) {
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
        return question.findName(node.getName());
    }


    @Override
    public PType caseAFunctionDeclaration(AFunctionDeclaration node, TypeCheckInfo info) throws AnalysisException {
        AFunctionType type = new AFunctionType();
        type.setResult(node.getReturnType());
        type.setParameters(node.getFormals().stream().map(x -> x.getType()).collect(Collectors.toList()));
        return type;
    }
}
