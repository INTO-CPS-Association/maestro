import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.framework.fmi2.RelationVariable;
import org.intocps.maestro.plugin.verificationsuite.PrologVerifier.InitializationPrologQuery;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public class PrologVerifierTest {
    InputStream envWaterTankJson = this.getClass().getResourceAsStream("PrologVerifierTest/env.json");
    InputStream envThreeTankJson = this.getClass().getResourceAsStream("PrologVerifierTest/threetank_env.json");

    /*
    @Test
    public void VerifyInitializationOrderWatertankTest() throws Exception {
        var prologVerifier = new InitializationPrologQuery();
        var unitRelationship = new UnitRelationship(envWaterTankJson);
        var components = Arrays.asList("crtlInstance", "wtInstance");
        var relations = new HashSet<UnitRelationship.Relation>();
        components.forEach(c -> relations.addAll(unitRelationship.getRelations(new LexIdentifier(c, null))));
        var initializationOrder = relations.stream().filter()
        var result = prologVerifier.initializationOrderIsValid();
        assert(result);
    }*/

    @Test
    public void VerifyInitializationOrderNotValidWatertankTest() throws Exception {
        var prologVerifier = new InitializationPrologQuery();
        var unitRelationship = FmiSimulationEnvironment.of(envWaterTankJson, new IErrorReporter.SilentReporter());
        var components = Arrays.asList("crtlInstance", "wtInstance");
        var relations = new HashSet<FmiSimulationEnvironment.Relation>();
        components.forEach(c -> relations.addAll(unitRelationship.getRelations(new LexIdentifier(c, null))));
        var initializationOrder = relations.stream().map(FmiSimulationEnvironment.Relation::getSource).distinct().collect(Collectors.toList());
        var result = prologVerifier.initializationOrderIsValid(initializationOrder, relations);
        assert (!result);
    }

    /*
    @Test
    public void VerifyInitializationOrderProlog() throws Exception {
        var prologVerifier = new InitializationPrologQuery();
        var unitRelationship = new UnitRelationship(envThreeTankJson);
        var relations = new ArrayList<UnitRelationship.Relation>();

        var variable1 = createVariable("Ctrl", "Sig", unitRelationship);
        var variable2 = createVariable("Ctrl", "Input", unitRelationship);
        var variable3 = createVariable("CE", "Level", unitRelationship);
        var variable4 = createVariable("Tank", "Level", unitRelationship);

        HashMap<LexIdentifier, Variable> target1 = new HashMap<>();
        target1.put(new LexIdentifier(variable1.scalarVariable.instance.getText(), null), variable1);
        HashMap<LexIdentifier, Variable> target2 = new HashMap<>();
        target2.put(new LexIdentifier(variable2.scalarVariable.instance.getText(), null), variable2);
        HashMap<LexIdentifier, Variable> target3 = new HashMap<>();
        target3.put(new LexIdentifier(variable3.scalarVariable.instance.getText(), null), variable3);
        HashMap<LexIdentifier, Variable> target4 = new HashMap<>();
        target4.put(new LexIdentifier(variable4.scalarVariable.instance.getText(), null), variable4);
        relations.add(new UnitRelationship.Relation.RelationBuilder(variable1, target2).build());
        relations.add(new UnitRelationship.Relation.RelationBuilder(variable2, target3).build());
        relations.add(new UnitRelationship.Relation.RelationBuilder(variable3, target4).build());

        var result =
        prologVerifier.initializationOrderIsValid(relations.stream().map(UnitRelationship.Relation::getSource).collect(Collectors.toList()), new HashSet<>(relations));

        assert(result);
    }
*/

    private FmiSimulationEnvironment.Variable createVariable(String fmuName, String variableName, FmiSimulationEnvironment unitRelationship) {
        var scalarVar = new ModelDescription.ScalarVariable();
        scalarVar.name = variableName;
        return new FmiSimulationEnvironment.Variable(new RelationVariable(scalarVar, new LexIdentifier(fmuName, null)));
    }

}

