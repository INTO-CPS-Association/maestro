import fi.iki.elonen.NanoHTTPD;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.PStm;
import org.intocps.maestro.plugin.IMaestroUnfoldPlugin;
import org.intocps.maestro.plugin.IPluginConfiguration;
import org.intocps.maestro.plugin.InitializerWrapCoe.InitializerUsingCOE;
import org.intocps.maestro.plugin.InitializerWrapCoe.SpecGen;
import org.intocps.maestro.plugin.UnfoldException;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.verify;

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
        SpecGen specGenMock = Mockito.mock(SpecGen.class);
        InputStream pluginConfiguration = minimalConfiguration;
        IMaestroUnfoldPlugin plugin = new InitializerUsingCOE(specGenMock);
        AFunctionDeclaration funcDecl = plugin.getDeclaredUnfoldFunctions().iterator().next();
        IPluginConfiguration parsedPluginConfiguration = plugin.parseConfig(pluginConfiguration);
        PStm stm = plugin.unfold(funcDecl, null, parsedPluginConfiguration, null, null);
        // matchers is just to ensure strings are not empy.
        verify(specGenMock).run(contains("test"), contains("startTime"));

    }
}
