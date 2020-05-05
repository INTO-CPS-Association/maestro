import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.PStm;
import org.intocps.maestro.plugin.IMaestroPlugin;
import org.intocps.maestro.plugin.IPluginConfiguration;
import org.intocps.maestro.plugin.InitializerWrapCoe.InitializerUsingCOE;
import org.intocps.maestro.plugin.InitializerWrapCoe.SpecGen;
import org.intocps.maestro.plugin.UnfoldException;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;

public class PluginInterfaceTests {

    InputStream minimalConfiguration = this.getClass().getResourceAsStream("pluginConfigMinimal.json");
    @Test
    public void ParseConfig() throws IOException {
        InputStream pluginConfiguration = minimalConfiguration;
        IMaestroPlugin plugin = new InitializerUsingCOE();
        plugin.parseConfig(minimalConfiguration);
    }

    @Test
    public void UnfoldCallsSpecGen() throws IOException, UnfoldException {
        SpecGen specGenMock = Mockito.mock(SpecGen.class);
        InputStream pluginConfiguration = minimalConfiguration;
        IMaestroPlugin plugin = new InitializerUsingCOE(specGenMock);
        AFunctionDeclaration funcDecl = plugin.getDeclaredUnfoldFunctions().iterator().next();
        IPluginConfiguration parsedPluginConfiguration = plugin.parseConfig(pluginConfiguration);
        PStm stm = plugin.unfold(funcDecl, null, parsedPluginConfiguration);
        // matchers is just to ensure strings are not empy.
        verify(specGenMock).run(contains("test"), contains("startTime"));

    }
}
