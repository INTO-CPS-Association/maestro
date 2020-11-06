package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.AnswerAdaptor;
import org.intocps.maestro.ast.node.AFunctionType;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PType;

import java.util.stream.Collectors;

public class TypeCheckVisitor extends AnswerAdaptor<PType> {
    @Override
    public PType createNewReturnValue(INode node) throws AnalysisException {
        return null;
    }

    @Override
    public PType createNewReturnValue(Object node) throws AnalysisException {
        return null;
    }

    @Override
    public PType caseAFunctionDeclaration(AFunctionDeclaration node) throws AnalysisException {
        AFunctionType type = new AFunctionType();
        type.setResult(node.getReturnType());
        type.setParameters(node.getFormals().stream().map(x -> x.getType()).collect(Collectors.toList()));
        return type;
    }
}
