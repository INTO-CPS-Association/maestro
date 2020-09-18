import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.maestro.plugin.prologverifier.graph.GraphDrawer;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;

public class GraphPlotterTest {
    InputStream envJson = this.getClass().getResourceAsStream("PrologVerifierTest/env.json");

    @Test
    public void PlotGraphTest() throws Exception {
        var graphDrawer = new GraphDrawer();
        var unitRelationship = new UnitRelationship(envJson);
        unitRelationship.getInstances();
        var components = Arrays.asList("crtlInstance", "wtInstance");
        var relations = new HashSet<UnitRelationship.Relation>();
        components.forEach(c -> relations.addAll(unitRelationship.getRelations(new LexIdentifier(c, null))));

        graphDrawer.plotGraph(relations, "ExampleGraph");
    }

}