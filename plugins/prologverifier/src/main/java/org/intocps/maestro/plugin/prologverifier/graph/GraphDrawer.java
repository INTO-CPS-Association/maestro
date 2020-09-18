package org.intocps.maestro.plugin.prologverifier.graph;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import org.intocps.maestro.plugin.env.UnitRelationship;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;

public class GraphDrawer {
    private String getInstanceName(UnitRelationship.Variable o) {
        return o.scalarVariable.instance.getText() + "." + o.scalarVariable.scalarVariable.getName();
    }

    public void plotGraph(Set<UnitRelationship.Relation> relations, String name) throws IOException {
        MutableGraph g = mutGraph(name).setDirected(true);
        for (UnitRelationship.Relation rel :
                relations.stream().filter(o -> o.getDirection() == UnitRelationship.Relation.Direction.OutputToInput).collect(Collectors.toList())) {
            var toNode = rel.getTargets().values().stream().map(o -> mutNode(getInstanceName(o))).collect(Collectors.toList());
            g.add(mutNode(getInstanceName(rel.getSource())).add(Color.BLACK).addLink(toNode));
        }

        Graphviz.fromGraph(g).height(200).render(Format.PNG).toFile(new File(String.format("example/%s.png", name)));
    }
}
