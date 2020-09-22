import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.maestro.plugin.env.UnitRelationship.Variable;
import org.intocps.maestro.plugin.env.fmi2.RelationVariable;
import org.intocps.maestro.plugin.prologverifier.graph.GraphDrawer;
import org.intocps.maestro.plugin.prologverifier.graph.VDMChecker;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
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
    public void PlotGraphWithSimpleLoop() throws Exception {
        var graphDrawer = new GraphDrawer();
        var relations = new HashSet<UnitRelationship.Relation>();
        var scalarVar1 = new ModelDescription.ScalarVariable();
        scalarVar1.name = "var1";
        var scalarVar2 = new ModelDescription.ScalarVariable();
        scalarVar2.name = "var2";
        var scalarVar3 = new ModelDescription.ScalarVariable();
        scalarVar3.name = "var3";
        var scalarVar4 = new ModelDescription.ScalarVariable();
        scalarVar4.name = "var4";
        var variable1 = new Variable(new RelationVariable(scalarVar1, new LexIdentifier("var1", null)));
        var variable2 = new Variable(new RelationVariable(scalarVar2, new LexIdentifier("var2", null)));
        var variable3 = new Variable(new RelationVariable(scalarVar3, new LexIdentifier("var3", null)));
        var variable4 = new Variable(new RelationVariable(scalarVar4, new LexIdentifier("var4", null)));

        HashMap<LexIdentifier, Variable> target1 = new HashMap<>();
        target1.put(new LexIdentifier("var1", null), variable1);
        HashMap<LexIdentifier, Variable> target2 = new HashMap<>();
        target2.put(new LexIdentifier("var2", null), variable2);
        HashMap<LexIdentifier, Variable> target3 = new HashMap<>();
        target3.put(new LexIdentifier("var3", null), variable3);
        HashMap<LexIdentifier, Variable> target4 = new HashMap<>();
        target4.put(new LexIdentifier("var4", null), variable4);
        relations.add(new UnitRelationship.Relation(UnitRelationship.Relation.Direction.OutputToInput, variable1, target2,
                UnitRelationship.Relation.InternalOrExternal.External));
        relations.add(new UnitRelationship.Relation(UnitRelationship.Relation.Direction.OutputToInput, variable3, target2,
                UnitRelationship.Relation.InternalOrExternal.Internal));
        relations.add(new UnitRelationship.Relation(UnitRelationship.Relation.Direction.OutputToInput, variable3, target1,
                UnitRelationship.Relation.InternalOrExternal.External));
        relations.add(new UnitRelationship.Relation(UnitRelationship.Relation.Direction.OutputToInput, variable3, target4,
                UnitRelationship.Relation.InternalOrExternal.External));
        graphDrawer.plotGraph(relations, "LoopGraph");
    }


    @Test
    public void VDMCheckerWaterTank() throws Exception {
        var VDMCheck = new VDMChecker();
        var unitRelationship = new UnitRelationship(envWaterTankJson);
        var fmus = unitRelationship.getFmuToUri();
        var fmuPaths = fmus.stream().map(o -> String.valueOf(o.getValue())).collect(Collectors.toList());
        VDMCheck.CheckFMUS(fmuPaths);
    }

    @Test
    public void VDMCheckerThreeTank() throws Exception {
        var VDMCheck = new VDMChecker();
        var unitRelationship = new UnitRelationship(envThreeTankJson);
        var fmus = unitRelationship.getFmuToUri();
        var fmuPaths = fmus.stream().map(o -> String.valueOf(o.getValue())).collect(Collectors.toList());
        VDMCheck.CheckFMUS(fmuPaths);
    }

}