import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.MaestroConfiguration;
import org.intocps.maestro.plugin.PluginFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class MaestroTest {


    @Test
    public void jsonParseTest() throws IOException {

        try (InputStream is = this.getClass().getResourceAsStream("plugin_configuration_1.json")) {
            Map<String, String> config = PluginFactory.parsePluginConfiguration(is);
            Assert.assertNotNull(config);
            Assert.assertTrue("key missing", config.containsKey("demo-0.0.1"));
        }
    }

    @Test
    public void maestroConfigurationCorrectDefaultValue() throws IOException {
        MaestroConfiguration defaultMaestroConfiguration = new MaestroConfiguration();
        try (InputStream is = this.getClass().getResourceAsStream("maestro_configuration_default.json")) {
            ObjectMapper mapper = new ObjectMapper();
            MaestroConfiguration maestroConfiguration = mapper.readValue(is, MaestroConfiguration.class);
            Assert.assertNotNull(maestroConfiguration);
            Assert.assertEquals(defaultMaestroConfiguration.getMaximumExpansionDepth(), maestroConfiguration.getMaximumExpansionDepth());
        }
    }

    @Test
    public void maestroConfigurationCorrectValue() throws IOException {
        try (InputStream is = this.getClass().getResourceAsStream("maestro_configuration_custom.json")) {
            ObjectMapper mapper = new ObjectMapper();
            MaestroConfiguration maestroConfiguration = mapper.readValue(is, MaestroConfiguration.class);
            Assert.assertNotNull(maestroConfiguration);
            Assert.assertEquals(-50, maestroConfiguration.getMaximumExpansionDepth());
        }
    }
}
