package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.core.messages.IErrorReporter;

import java.util.HashMap;
import java.util.Map;

public class TypeChecker extends DepthFirstAnalysisAdaptor {

    final static PType VOID_TYPE = new AVoidType();
    final IErrorReporter errorReporter;
    Map<INode, PType> types = new HashMap<>();

    public TypeChecker(IErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @Override
    public void caseABreakStm(ABreakStm node) throws AnalysisException {

        if (node.getAncestor(AWhileStm.class) == null) {
            errorReporter.report(0, "Break must be enclosed in a while statement", node.getToken());
        }
        types.put(node, VOID_TYPE);
    }


    @Override
    public void defaultOutPStm(PStm node) throws AnalysisException {
        types.put(node, VOID_TYPE);
    }
}
