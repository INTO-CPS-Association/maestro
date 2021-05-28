package org.intocps.maestro.optimization;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.ALocalVariableStm;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.SBlockStm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UnusedDeclarationOptimizer extends DepthFirstAnalysisAdaptor {

    @Override
    public void caseALocalVariableStm(ALocalVariableStm node) throws AnalysisException {
        LexIdentifier name = node.getDeclaration().getName();
        //so this is a statement and for it to be used anywhere it must be in a block so lets search the remainder of the block

        SBlockStm block = node.getAncestor(SBlockStm.class);
        if (block != null) {

            boolean used = false;
            for (int i = block.getBody().indexOf(node) + 1; i < block.getBody().size(); i++) {
                PStm stm = block.getBody().get(i);
                used |= IdentifierUseFinder.find(stm, name);
            }

            if (!used) {
                block.getBody().remove(node);
            }
        }
    }


    static class IdentifierUseFinder extends DepthFirstAnalysisAdaptor {
        final LexIdentifier identifier;
        private final Predicate<INode> ignoreFilter;
        boolean found = false;

        public IdentifierUseFinder(LexIdentifier identifier, Predicate<INode> ignoreFilter) {
            this.identifier = identifier;
            this.ignoreFilter = ignoreFilter;
        }

        public static boolean find(INode tree, LexIdentifier identifier) throws AnalysisException {
            IdentifierUseFinder finder = new IdentifierUseFinder(identifier, (n) -> n == tree);
            try {
                tree.apply(finder);
            } catch (StopAnalysisException e) {
                //ignore
            }
            return finder.found;
        }

        @Override
        public void defaultInINode(INode node) throws AnalysisException {
            if (ignoreFilter.test(node)) {
                return;
            }
            //the lexidentifier is not searchable so we attempt to find any use through reflection
            List<Method> potentialMatches = Arrays.stream(node.getClass().getMethods())
                    .filter(m -> m.getName().startsWith("get") && m.getParameterCount() == 0 &&
                            m.getReturnType().isAssignableFrom(this.identifier.getClass())).collect(Collectors.toList());
            for (Method m : potentialMatches) {
                try {
                    if (m.invoke(node).equals(this.identifier)) {
                        this.found = true;
                        throw new StopAnalysisException();
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new AnalysisException("Could not access field to search for identifier", e);
                }
            }

        }

        class StopAnalysisException extends AnalysisException {
        }
    }
}
