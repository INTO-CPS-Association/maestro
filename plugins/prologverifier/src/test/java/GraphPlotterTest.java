import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.maestro.plugin.prologverifier.graph.GraphDrawer;
import org.intocps.maestro.plugin.prologverifier.graph.VDMChecker;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public class GraphPlotterTest {
    InputStream envWaterTankJson = this.getClass().getResourceAsStream("PrologVerifierTest/env.json");
    InputStream envThreeTankJson = this.getClass().getResourceAsStream("PrologVerifierTest/threetank_env.json");

    @Test
    public void PlotGraphWatertankTest() throws Exception {
        var graphDrawer = new GraphDrawer();
        var unitRelationship = new UnitRelationship(envWaterTankJson);
        var components = Arrays.asList("crtlInstance", "wtInstance");
        var relations = new HashSet<UnitRelationship.Relation>();
        components.forEach(c -> relations.addAll(unitRelationship.getRelations(new LexIdentifier(c, null))));

        graphDrawer.plotGraph(relations, "WaterTankGraph");
    }

    //@Ignore
    @Test
    public void PlotGraphThreeTankTest() throws Exception {
        var graphDrawer = new GraphDrawer();
        var unitRelationship = new UnitRelationship(envThreeTankJson);
        var components = Arrays.asList("controller", "tank1", "tank2");
        var relations = new HashSet<UnitRelationship.Relation>();
        components.forEach(c -> relations.addAll(unitRelationship.getRelations(new LexIdentifier(c, null))));

        graphDrawer.plotGraph(relations, "ThreeTankGraph");
    }


    @Test
    public void VDMCheckerWaterTank() throws Exception {
        var VDMCheck = new VDMChecker();
        var unitRelationship = new UnitRelationship(envWaterTankJson);
        var fmus = unitRelationship.getFmuToUri();
        var fmuPaths  = fmus.stream().map(o -> String.valueOf(o.getValue())).collect(Collectors.toList());
        VDMCheck.CheckFMUS(fmuPaths);
    }

    @Test
    public void VDMCheckerThreeTank() throws Exception {
        var VDMCheck = new VDMChecker();
        var unitRelationship = new UnitRelationship(envThreeTankJson);
        var fmus = unitRelationship.getFmuToUri();
        var fmuPaths  = fmus.stream().map(o -> String.valueOf(o.getValue())).collect(Collectors.toList());
        VDMCheck.CheckFMUS(fmuPaths);
    }

}