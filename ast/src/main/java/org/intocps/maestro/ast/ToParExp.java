package org.intocps.maestro.ast;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.SBinaryExp;

import static org.intocps.maestro.ast.MableAstFactory.newAParExp;

public class ToParExp extends DepthFirstAnalysisAdaptor {
    @Override
    public void defaultInSBinaryExp(SBinaryExp node) throws AnalysisException {
        node.getLeft().apply(this);
        node.getRight().apply(this);
        if (node.parent() instanceof SBinaryExp) {
            INode parent = node.parent();
            node.parent().replaceChild(node, newAParExp(node.clone()));
            parent.toString();
        }
    }
}
