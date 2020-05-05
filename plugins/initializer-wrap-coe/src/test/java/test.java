import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.ast.PStm;
import org.intocps.maestro.plugin.InitializerWrapCoe.FMIStatementInterface.StatementFactory;
import org.intocps.maestro.plugin.InitializerWrapCoe.Spec.StatementContainer;
import org.intocps.maestro.plugin.InitializerWrapCoe.SpecGen;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class test {

    @Test
    public void testWatertankC() throws IOException, NanoHTTPD.ResponseException {
        InputStream configurationDataStream = this.getClass().getResourceAsStream("watertankconfig.json");
        String configurationData = IOUtils.toString(configurationDataStream);
        InputStream startMsgStream = this.getClass().getResourceAsStream("watertankconfig-startmsg.json");
        String startMsg = IOUtils.toString(startMsgStream);

        SpecGen sg = new SpecGen();
        sg.run(configurationData, startMsg);
        List<PStm> statements = StatementContainer.getInstance().getStatements();
    }
}
