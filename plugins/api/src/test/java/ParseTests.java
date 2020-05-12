import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Set;

public class ParseTests {
    @Test
    public void ParsesMultiModelMessage() throws Exception {
        InputStream multimodelJson = this.getClass().getResourceAsStream("watertankmultimodel.json");
        UnitRelationship env = new UnitRelationship(multimodelJson);
        Set<UnitRelationship.Relation> relations = env.getRelations(new LexIdentifier("controller", null), new LexIdentifier("tank", null));
        // Todo: Improve test
        Assert.assertTrue(relations.size() == 5);
    }
}
