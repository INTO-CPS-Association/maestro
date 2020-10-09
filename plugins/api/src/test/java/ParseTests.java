import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.FrameworkVariableInfo;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Set;

public class ParseTests {
    @Test
    public void ParsesMultiModelMessage() throws Exception {
        InputStream multimodelJson = this.getClass().getResourceAsStream("watertankmultimodel.json");
        IErrorReporter reporter = new IErrorReporter.SilentReporter();
        FmiSimulationEnvironment env = FmiSimulationEnvironment.of(multimodelJson, reporter);
        Set<? extends FrameworkVariableInfo> relations = env.getRelations(new LexIdentifier("controller", null), new LexIdentifier("tank", null));
        // Todo: Improve test
        Assert.assertTrue(relations.size() == 5);
    }
}
