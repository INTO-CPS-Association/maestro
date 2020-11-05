package org.intocps.maestro.ast;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.INode;

import java.util.List;
import java.util.Optional;
import java.util.Vector;

public class NodeCollector {

    public static <T extends INode> Optional<List<T>> collect(INode node, Class<T> type) {
        List<T> nodes = new Vector<>();

        try {
            node.apply(new DepthFirstAnalysisAdaptor() {
                @Override
                public void defaultInINode(INode node) {
                    if (type.isAssignableFrom(node.getClass())) {
                        nodes.add((T) node);
                    }
                }
            });
            return Optional.of(nodes);
        } catch (AnalysisException e) {
            return Optional.empty();
        }


    }


}
