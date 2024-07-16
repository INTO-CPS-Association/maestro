package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.AFieldExp;
import org.intocps.maestro.ast.node.AIdentifierExp;
import org.intocps.maestro.ast.node.PExp;

import java.util.HashMap;
import java.util.Map;

class IdentifierReplacer {
    public static PExp replaceIdentifier(PExp tree, String oldIdentifier, String newIdentifier) {
        return replaceIdentifier(tree, new HashMap<>() {
            {
                put(oldIdentifier, newIdentifier);
            }
        });
    }

    static public PExp replaceIdentifier(PExp tree, Map<String, String> old2New) {
        try {
            tree.apply(new IdentifierReplace(old2New));
        } catch (AnalysisException e) {
            throw new RuntimeException(e);
        }
        return tree;
    }

    static public PExp replaceFields(PExp tree, Map<String, PExp> old2New) {
        try {
            tree.apply(new FieldExpReplace(old2New));
        } catch (AnalysisException e) {
            throw new RuntimeException(e);
        }
        return tree;
    }

    static class IdentifierReplace extends DepthFirstAnalysisAdaptor {
        final Map<String, String> old2New;

        public IdentifierReplace(Map<String, String> old2New) {
            this.old2New = old2New;
        }

        @Override
        public void caseAIdentifierExp(AIdentifierExp node) throws AnalysisException {
            for (Map.Entry<String, String> replacing : old2New.entrySet()) {
                if (node.getName().getText().equals(replacing.getKey())) {
                    node.parent().replaceChild(node, new AIdentifierExp(new LexIdentifier(replacing.getValue(), null)));
                }
            }
        }
    }

    static class FieldExpReplace extends DepthFirstAnalysisAdaptor {
        final Map<String, PExp> old2New;

        public FieldExpReplace(Map<String, PExp> old2New) {
            this.old2New = old2New;
        }

        @Override
        public void caseAFieldExp(AFieldExp node) throws AnalysisException {
            for (Map.Entry<String, PExp> replacing : old2New.entrySet()) {
                String[] parts = replacing.getKey().split("\\.");
                if (node.getRoot() instanceof AIdentifierExp && ((AIdentifierExp) node.getRoot()).getName().getText().equals(parts[0]) &&
                        node.getField().getText().equals(parts[1])) {
                    node.parent().replaceChild(node, replacing.getValue().clone());
                }
            }
        }


    }

}
