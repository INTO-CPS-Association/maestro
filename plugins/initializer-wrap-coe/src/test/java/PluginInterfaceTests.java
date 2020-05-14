import fi.iki.elonen.NanoHTTPD;
import org.intocps.maestro.plugin.IMaestroUnfoldPlugin;
import org.intocps.maestro.plugin.InitializerWrapCoe.InitializerUsingCOE;
import org.intocps.maestro.plugin.UnfoldException;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class PluginInterfaceTests {

    InputStream minimalConfiguration = this.getClass().getResourceAsStream("pluginConfigMinimal.json");

    @Test
    public void ParseConfig() throws IOException {
        InputStream pluginConfiguration = minimalConfiguration;
        IMaestroUnfoldPlugin plugin = new InitializerUsingCOE();
        plugin.parseConfig(minimalConfiguration);
    }

    @Test
    public void UnfoldCallsSpecGen() throws IOException, UnfoldException, NanoHTTPD.ResponseException {
        //        SpecGen specGenMock = Mockito.mock(SpecGen.class);
        //        InputStream pluginConfiguration = minimalConfiguration;
        //        IMaestroUnfoldPlugin plugin = new InitializerUsingCOE(specGenMock);
        //        AFunctionDeclaration funcDecl = plugin.getDeclaredUnfoldFunctions().iterator().next();
        //        IPluginConfiguration parsedPluginConfiguration = plugin.parseConfig(pluginConfiguration);
        //        PStm stm = plugin.unfold(funcDecl, null, parsedPluginConfiguration, null, null);
        //        // matchers is just to ensure strings are not empy.
        //        verify(specGenMock).run(any(), contains("test"), contains("startTime"), any(), any());

    }
}
