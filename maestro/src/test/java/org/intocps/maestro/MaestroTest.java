package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.plugin.PluginFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class MaestroTest {


    @Test
    public void jsonParseTest() throws IOException {

        try (InputStream is = this.getClass().getResourceAsStream("/plugin_configuration_1.json")) {
            Map<String, String> config = PluginFactory.parsePluginConfiguration(is);
            Assertions.assertNotNull(config);
            Assertions.assertTrue(config.containsKey("demo-0.0.1"), "key missing");
        }
    }

    @Test
    public void maestroConfigurationCorrectDefaultValue() throws IOException {
        MaestroConfiguration defaultMaestroConfiguration = new MaestroConfiguration();
        try (InputStream is = this.getClass().getResourceAsStream("/maestro_configuration_default.json")) {
            ObjectMapper mapper = new ObjectMapper();
            MaestroConfiguration maestroConfiguration = mapper.readValue(is, MaestroConfiguration.class);
            Assertions.assertNotNull(maestroConfiguration);
            Assertions.assertEquals(defaultMaestroConfiguration.getMaximumExpansionDepth(), maestroConfiguration.getMaximumExpansionDepth());
        }
    }

    @Test
    public void maestroConfigurationCorrectValue() throws IOException {
        try (InputStream is = this.getClass().getResourceAsStream("/maestro_configuration_custom.json")) {
            ObjectMapper mapper = new ObjectMapper();
            MaestroConfiguration maestroConfiguration = mapper.readValue(is, MaestroConfiguration.class);
            Assertions.assertNotNull(maestroConfiguration);
            Assertions.assertEquals(-50, maestroConfiguration.getMaximumExpansionDepth());
        }
    }
}
