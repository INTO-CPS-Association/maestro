package org.intocps.maestro;

import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.QuestionAnswerAdaptor;

public class TypecheckerTest extends QuestionAnswerAdaptor<TypeCheckInfo, PType> {
    @Override
    public PType createNewReturnValue(INode node, TypeCheckInfo question) throws AnalysisException {
        return null;
    }

    @Override
    public PType createNewReturnValue(Object node, TypeCheckInfo question) throws AnalysisException {
        return null;
    }


    @Override
    public PType caseAStringLiteralExp(AStringLiteralExp node, TypeCheckInfo question) throws AnalysisException {
        PType type = MableAstFactory.newAStringPrimitiveType();
        node.setType(type);
        return type;
    }

    @Override
    public PType caseABoolLiteralExp(ABoolLiteralExp node, TypeCheckInfo question) throws AnalysisException {
        PType type = MableAstFactory.newABoleanPrimitiveType();
        node.setType(type);
        return type;
    }

    @Override
    public PType caseAIntLiteralExp(AIntLiteralExp node, TypeCheckInfo question) throws AnalysisException {
        PType type = MableAstFactory.newAIntNumericPrimitiveType();
        node.setType(type);
        return type;
    }

    @Override
    public PType caseARealLiteralExp(ARealLiteralExp node, TypeCheckInfo question) throws AnalysisException {
        PType type = MableAstFactory.newARealNumericPrimitiveType();
        node.setType(type);
        return type;
    }


    @Override
    public PType caseAUIntLiteralExp(AUIntLiteralExp node, TypeCheckInfo question) throws AnalysisException {
        PType type = MableAstFactory.newAUIntNumericPrimitiveType();
        node.setType(type);
        return type;
    }

    /**
     * Used for <, <=, >, >=
     *
     * @param node
     * @param question
     * @return
     * @throws AnalysisException
     */
    public PType caseBinaryExpBase(SBinaryExpBase node, TypeCheckInfo question) throws AnalysisException {
        PType leftType = node.getLeft().apply(this, question);
        PType rightType = node.getLeft().apply(this, question);
        if (leftType.getClass().equals(rightType.getClass()) && leftType instanceof SNumericPrimitiveBase) {
            PType type = MableAstFactory.newABoleanPrimitiveType();
            node.setType(type);
            return type;
        } else {
            throw new AnalysisException("typechecker: caseBinaryExpBase");
        }
    }

    @Override
    public PType caseALessBinaryExp(ALessBinaryExp node, TypeCheckInfo question) throws AnalysisException {
        return caseBinaryExpBase(node, question);
    }


    @Override
    public PType caseAGreaterBinaryExp(AGreaterBinaryExp node, TypeCheckInfo question) throws AnalysisException {
        return caseBinaryExpBase(node, question);
    }

    @Override
    public PType caseALessEqualBinaryExp(ALessEqualBinaryExp node, TypeCheckInfo question) throws AnalysisException {
        return caseBinaryExpBase(node, question);
    }

    @Override
    public PType caseAGreaterEqualBinaryExp(AGreaterEqualBinaryExp node, TypeCheckInfo question) throws AnalysisException {
        return caseBinaryExpBase(node, question);
    }

    /**
     * Used for == and !=
     *
     * @param node
     * @param question
     * @return
     * @throws AnalysisException
     */
    public PType caseEquality(SBinaryExpBase node, TypeCheckInfo question) throws AnalysisException {
        PType leftType = node.getLeft().apply(this, question);
        PType rightType = node.getLeft().apply(this, question);
        if (leftType.getClass() == rightType.getClass()) {
            PType type = MableAstFactory.newABoleanPrimitiveType();
            node.setType(type);
            return type;
        } else {
            throw new AnalysisException("typechecker: caseEquality");
        }
    }

    @Override
    public PType caseAEqualBinaryExp(AEqualBinaryExp node, TypeCheckInfo question) throws AnalysisException {
        return caseEquality(node, question);
    }

    @Override
    public PType caseANotEqualBinaryExp(ANotEqualBinaryExp node, TypeCheckInfo question) throws AnalysisException {
        return caseEquality(node, question);
    }

    @Override
    public PType caseANotUnaryExp(ANotUnaryExp node, TypeCheckInfo question) throws AnalysisException {
        if (node.apply(this, question).getClass() == ABooleanPrimitiveType.class) {
            return MableAstFactory.newABoleanPrimitiveType();
        } else {
            throw new AnalysisException("typechecker: caseANotUnaryExp");
        }
    }

    @Override
    public PType caseAIfStm(AIfStm node, TypeCheckInfo question) throws AnalysisException {
        PType test = node.getTest().apply(this, question);
        if (test.getClass() == ABooleanPrimitiveType.class) {
            node.getThen().apply(this, question);
            node.getElse().apply(this, question);
            return MableAstFactory.newAVoidType();
        } else {
            throw new AnalysisException("typechecker: caseAIfStm");
        }
    }

    @Override
    public PType caseString(String node, TypeCheckInfo question) throws AnalysisException {
        return MableAstFactory.newAStringPrimitiveType();
    }

    @Override
    public PType caseBoolean(Boolean node, TypeCheckInfo question) throws AnalysisException {
        return MableAstFactory.newABoleanPrimitiveType();
    }

    @Override
    public PType caseInteger(Integer node, TypeCheckInfo question) throws AnalysisException {
        return MableAstFactory.newAIntNumericPrimitiveType();
    }

    @Override
    public PType caseDouble(Double node, TypeCheckInfo question) throws AnalysisException {
        return MableAstFactory.newARealNumericPrimitiveType();
    }

    @Override
    public PType caseLong(Long node, TypeCheckInfo question) throws AnalysisException {
        return MableAstFactory.newAUIntNumericPrimitiveType();
    }

    @Override
    public PType caseAVariableDeclaration(AVariableDeclaration node, TypeCheckInfo question) throws AnalysisException {
        // Get type of variable
        // Get type of expression
        // Add to environment
        return super.caseAVariableDeclaration(node, question);
    }
}
