import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.RelationVariable;
import org.intocps.maestro.plugin.verificationsuite.graph.GraphDrawer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

@Disabled
public class GraphPlotterTest {
    InputStream envWaterTankJson = this.getClass().getResourceAsStream("PrologVerifierTest/env.json");
    InputStream envThreeTankJson = this.getClass().getResourceAsStream("PrologVerifierTest/threetank_env.json");

    @Test
    public void plotGraphWatertankTest() throws Exception {
        var graphDrawer = new GraphDrawer();
        var unitRelationship = Fmi2SimulationEnvironment.of(envWaterTankJson, new IErrorReporter.SilentReporter());
        var components = Arrays.asList("crtlInstance", "wtInstance");
        var relations = new HashSet<Fmi2SimulationEnvironment.Relation>();
        components.forEach(c -> relations.addAll(unitRelationship.getRelations(new LexIdentifier(c, null))));

        graphDrawer.plotGraph(relations, "WaterTankGraph");
    }

    //@Ignore
    @Test
    public void plotGraphThreeTankTest() throws Exception {
        var graphDrawer = new GraphDrawer();
        var unitRelationship = Fmi2SimulationEnvironment.of(envThreeTankJson, new IErrorReporter.SilentReporter());
        var components = Arrays.asList("controller", "tank1", "tank2");
        var relations = new HashSet<Fmi2SimulationEnvironment.Relation>();
        components.forEach(c -> relations.addAll(unitRelationship.getRelations(new LexIdentifier(c, null))));

        graphDrawer.plotGraph(relations, "ThreeTankGraph");
    }

    @Test
    public void plotGraphWithSimpleLoop() throws Exception {
        var graphDrawer = new GraphDrawer();
        var unitRelationship = Fmi2SimulationEnvironment.of(envThreeTankJson, new IErrorReporter.SilentReporter());
        var relations = new HashSet<Fmi2SimulationEnvironment.Relation>();

        var variable1 = createVariable("Ctrl", "Sig", unitRelationship);
        var variable2 = createVariable("Ctrl", "Input", unitRelationship);
        var variable3 = createVariable("CE", "Level", unitRelationship);
        var variable4 = createVariable("Tank", "Level", unitRelationship);

        HashMap<LexIdentifier, Fmi2SimulationEnvironment.Variable> target1 = new HashMap<>();
        target1.put(new LexIdentifier(variable1.scalarVariable.instance.getText(), null), variable1);
        HashMap<LexIdentifier, Fmi2SimulationEnvironment.Variable> target2 = new HashMap<>();
        target2.put(new LexIdentifier(variable2.scalarVariable.instance.getText(), null), variable2);
        HashMap<LexIdentifier, Fmi2SimulationEnvironment.Variable> target3 = new HashMap<>();
        target3.put(new LexIdentifier(variable3.scalarVariable.instance.getText(), null), variable3);
        HashMap<LexIdentifier, Fmi2SimulationEnvironment.Variable> target4 = new HashMap<>();
        target4.put(new LexIdentifier(variable4.scalarVariable.instance.getText(), null), variable4);
        relations.add(new Fmi2SimulationEnvironment.Relation.RelationBuilder(variable1, target2).build());
        relations.add(new Fmi2SimulationEnvironment.Relation.RelationBuilder(variable3, target2)
                .setInternalOrExternal(Fmi2SimulationEnvironment.Relation.InternalOrExternal.Internal).build());
        relations.add(new Fmi2SimulationEnvironment.Relation.RelationBuilder(variable3, target1).build());
        relations.add(new Fmi2SimulationEnvironment.Relation.RelationBuilder(variable3, target4).build());

        graphDrawer.plotGraph(relations, "LoopGraph");
    }


    private Fmi2SimulationEnvironment.Variable createVariable(String fmuName, String variableName, Fmi2SimulationEnvironment unitRelationship) {
        var scalarVar = new Fmi2ModelDescription.ScalarVariable();
        scalarVar.name = variableName;
        return new Fmi2SimulationEnvironment.Variable(new RelationVariable(scalarVar, new LexIdentifier(fmuName, null)));
    }

}

