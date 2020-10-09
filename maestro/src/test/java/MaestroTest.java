import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.MableSpecificationGenerator;
import org.intocps.maestro.MaestroConfiguration;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.plugin.PluginFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MaestroTest {

    @Test(expected = RuntimeException.class)
    public void simpleParseTest() throws Exception {
        InputStream contextFile = null;
        new MableSpecificationGenerator(Framework.FMI2, true, FmiSimulationEnvironment
                .of(Paths.get("src", "test", "resources", "watertank_env.json").toAbsolutePath().toFile(), new IErrorReporter.SilentReporter()))
                .generate(Stream.of(Paths.get("src", "test", "resources", "libraries/FMI2.mabl").toAbsolutePath().toString(),
                        Paths.get("src", "test", "resources", "jacobian.mabl").toAbsolutePath().toString()).map(File::new)
                        .collect(Collectors.toList()), contextFile);

    }

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
