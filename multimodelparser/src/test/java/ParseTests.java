import org.intocps.maestro.multimodelparser.MultiModelMessage;
import org.intocps.maestro.multimodelparser.MultiModelParser;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class ParseTests {
    @Test
    public void ParsesMultiModelMessage(){
        InputStream multimodelJson = this.getClass().getResourceAsStream("watertankmultimodel.json");
        MultiModelParser mmp = new MultiModelParser();
        MultiModelMessage msg = mmp.ParseMultiModel(multimodelJson);
        Assert.assertTrue(msg.fmus.size() == 2);
        Assert.assertTrue(msg.parameters.size() == 2);
        Assert.assertTrue(msg.connections.size() == 2);
    }
}
