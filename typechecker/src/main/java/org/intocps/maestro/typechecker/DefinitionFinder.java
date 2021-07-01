package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptorQuestion;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.typechecker.context.Context;

public class DefinitionFinder {
    private final TypeFetcher typeFetcher;

    public DefinitionFinder(TypeFetcher typeFetcher) {
        this.typeFetcher = typeFetcher;
    }


    //    public PDeclaration find(PExp exp) {
    //        if (exp instanceof AIdentifierExp) {
    //            AIdentifierExp id = exp;
    //            id.getName()
    //        }
    //    }
    //
    //    public PDeclaration findName(PExp exp) {
    //        if (exp instanceof AIdentifierExp) {
    //            AIdentifierExp id = (AIdentifierExp) exp;
    //            return (PDeclaration) id.getName();
    //        } else if (exp instanceof AArrayIndexExp) {
    //            AArrayIndexExp v = (AArrayIndexExp) exp;
    //            return findName(v.getArray());
    //        } else if (exp instanceof AParExp) {
    //            AParExp p = (AParExp) exp;
    //            return findName(p.getExp());
    //        } else if (exp instanceof AFieldExp) {
    //            AFieldExp fieldExp = (AFieldExp) exp;
    //            return findName(fieldExp.getField();
    //        } return null;
    //    }

    public PDeclaration find(PExp exp, Context ctxt) throws AnalysisException {
        DefFinderVisistor finder = new DefFinderVisistor(typeFetcher);
        exp.apply(finder, ctxt);
        return finder.def;
    }

    public interface TypeFetcher {
        PType getType(INode node);
    }

    class DefFinderVisistor extends DepthFirstAnalysisAdaptorQuestion<Context> {
        final TypeFetcher typeFetcher;
        PDeclaration def;

        public DefFinderVisistor(TypeFetcher typeFetcher) {
            this.typeFetcher = typeFetcher;
        }

        void tryLookup(Context question, LexIdentifier id) {
            PDeclaration d = question.findDeclaration(id);
            if (d != null) {
                def = d;
            }
        }

        @Override
        public void caseAIdentifierExp(AIdentifierExp node, Context question) throws AnalysisException {

            tryLookup(question, node.getName());
        }

        @Override
        public void caseAArrayIndexExp(AArrayIndexExp node, Context question) throws AnalysisException {
            node.getArray().apply(this, question);
        }

        @Override
        public void caseAFieldExp(AFieldExp node, Context question) throws AnalysisException {
            node.getRoot().apply(this, question);
            if (def != null) {
                PType type = typeFetcher.getType(def);
                if (type instanceof AModuleType) {
                    String fieldName = node.getField().getText();
                    def = question.findDeclaration(((AModuleType) type).getName(), new LexIdentifier(fieldName, null));
                }

            }
        }

    }
}
