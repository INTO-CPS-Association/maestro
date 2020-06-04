import org.intocps.maestro.MableSpecificationGenerator;
import org.intocps.maestro.ast.ARootDocument;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InitializePluginTest {

    @Test
    public void UnfoldInitialize() throws Exception {
        File directory = new File(Paths.get("src", "test", "resources", "InitializePluginTest").toAbsolutePath().toString());

        InputStream contextFile = createConfigJson();

        UnitRelationship env = UnitRelationship.of(new File(directory, "env.json"));

        ARootDocument doc = new MableSpecificationGenerator(Framework.FMI2, true, env).generate(
                Stream.of(Paths.get("src", "test", "resources", "libraries/FMI2.mabl").toAbsolutePath().toString(),
                        Paths.get("src", "test", "resources", "InitializePluginTest", "foldedspec.mabl").toAbsolutePath().toString()).map(File::new)
                        .collect(Collectors.toList()), contextFile);
    }

    public InputStream createConfigJson() {
        String singlewatertank_20simfmu = Paths.get("src", "test", "resources", "singlewatertank-20sim.fmu").toFile().toURI().toString();
        String watertankcontroller_cfmu = Paths.get("src", "test", "resources", "watertankcontroller-c.fmu").toFile().toURI().toString();

        String config =
                "[\n" + "  {\n" + "    \"identification\": {\n" + "      \"name\": \"InitializerUsingCOE\",\n" + "      \"version\": \"0.0.0\"\n" +
                        "    },\n" + "    \"config\": {\n" + "      \"configuration\": {\n" + "        \"fmus\": {\n" + "          \"{crtl}\": \"" +
                        watertankcontroller_cfmu + "\",\n" + "          \"{wt}\": \"" + singlewatertank_20simfmu + "\"\n" + "        },\n" +
                        "        \"connections\": {\n" + "          \"{crtl}.crtlInstance.valve\": [\n" +
                        "            \"{wt}.wtInstance.valvecontrol\"\n" + "          ],\n" + "          \"{wt}.wtInstance.level\": [\n" +
                        "            \"{crtl}.crtlInstance.level\"\n" + "          ]\n" + "        },\n" + "        \"parameters\": {\n" +
                        "          \"{crtl}.crtlInstance.maxlevel\": 2,\n" + "          \"{crtl}.crtlInstance.minlevel\": 1\n" + "        },\n" +
                        "        \"algorithm\":{\n" + "          \"type\":\"fixed-step\",\n" + "          \"size\":0.1\n" + "        }\n" +
                        "      },\n" +
                        "      \"start_message\" : {\"startTime\":0,\"endTime\":10,\"reportProgress\":true,\"liveLogInterval\":0,\"logLevels\":{}}\n" +
                        "    }\n" + "  }\n" + "]";
        return new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8));
    }


}
