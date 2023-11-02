package org.intocps.maestro.plugin.verificationsuite.graph;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.RelationVariable;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;

public class GraphDrawer {
    private String getInstanceName(RelationVariable o) {
        return o.getInstance().getText() + "." + o.getName();
    }

    public void plotGraph(Set<? extends Fmi2SimulationEnvironment.Relation> relations, String name) throws IOException {
        MutableGraph g = mutGraph(name).setDirected(true);
        var connections = relations.stream().filter(o -> o.getDirection() == Fmi2SimulationEnvironment.Relation.Direction.OutputToInput)
                .collect(Collectors.toList());
        for (Fmi2SimulationEnvironment.Relation rel : connections) {
            var targets = rel.getTargets().values().stream().map(o -> mutNode(getInstanceName(o)).add(Color.BLACK)).collect(Collectors.toList());
            var source = mutNode(getInstanceName(rel.getSource())).add(Color.BLACK);

            if (rel.getOrigin() == Fmi2SimulationEnvironment.Relation.InternalOrExternal.Internal) {
                targets.forEach(t -> {
                    g.add(t.addLink(source));
                    g.nodes().stream().filter(o -> o == t).forEach(o -> o.links().forEach(r -> r.attrs().add(Style.DASHED)));
                });
            } else {
                targets.forEach(t -> g.add(source.addLink(t)));
            }
        }


        Graphviz.fromGraph(g).height(500).render(Format.PNG).toFile(new File(String.format("example/%s.png", name)));
    }
}
