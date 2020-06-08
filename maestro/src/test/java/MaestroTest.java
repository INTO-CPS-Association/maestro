import org.intocps.maestro.MableSpecificationGenerator;
import org.intocps.maestro.ast.ARootDocument;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.interpreter.LoadFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.plugin.PluginFactory;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.junit.Assert;
import org.junit.Ignore;
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
        new MableSpecificationGenerator(Framework.FMI2, true,
                UnitRelationship.of(Paths.get("src", "test", "resources", "watertank_env.json").toAbsolutePath().toFile())).generate(
                Stream.of(Paths.get("src", "test", "resources", "libraries/FMI2.mabl").toAbsolutePath().toString(),
                        Paths.get("src", "test", "resources", "jacobian.mabl").toAbsolutePath().toString()).map(File::new)
                        .collect(Collectors.toList()), contextFile);

    }

    @Test
    @Ignore
    public void singleExternal() throws Exception {

        InputStream contextFile = this.getClass().getResourceAsStream("configs/singleExternal.json");
        ARootDocument doc = new MableSpecificationGenerator(Framework.FMI2, true,
                UnitRelationship.of(Paths.get("src", "test", "resources", "watertank_env.json").toAbsolutePath().toFile())).generate(
                Stream.of(Paths.get("src", "test", "resources", "libraries/FMI2.mabl").toAbsolutePath().toString(),
                        Paths.get("src", "test", "resources", "single_external.mabl").toAbsolutePath().toString()).map(File::new)
                        .collect(Collectors.toList()), contextFile);


        new MableInterpreter(new LoadFactory()).execute(doc);
    }

    //    @Test
    //    public void fullWt() throws IOException, AnalysisException {
    //
    //        InputStream contextFile = this.getClass().getResourceAsStream("configs/singleExternal.json");
    //        ARootDocument doc = new MableSpecificationGenerator(Framework.FMI2, true,
    //                UnitRelationship.of(Paths.get("src", "test", "resources", "watertank_env.json").toAbsolutePath().toFile())).generate(
    //                Stream.of(Paths.get("src", "test", "resources", "FMI2.mabl").toAbsolutePath().toString(),
    //                        Paths.get("src", "test", "resources", "full_example_wt.mabl").toAbsolutePath().toString(),
    //                        Paths.get("src", "test", "resources", "CSV.mabl").toAbsolutePath().toString()).map(File::new).collect(Collectors.toList()),
    //                contextFile);
    //
    //
    //        new MableInterpreter().execute(doc);
    //    }
    //
    //
    //    @Test
    //    public void singleWt() throws IOException, AnalysisException {
    //
    //        InputStream contextFile = this.getClass().getResourceAsStream("configs/singleExternal.json");
    //        ARootDocument doc = new MableSpecificationGenerator(Framework.FMI2, true,
    //                UnitRelationship.of(Paths.get("src", "test", "resources", "watertank_env.json").toAbsolutePath().toFile())).generate(
    //                Stream.of(Paths.get("src", "test", "resources", "FMI2.mabl").toAbsolutePath().toString(),
    //                        Paths.get("src", "test", "resources", "single_wt.mabl").toAbsolutePath().toString(),
    //                        Paths.get("src", "test", "resources", "CSV.mabl").toAbsolutePath().toString()).map(File::new).collect(Collectors.toList()),
    //                contextFile);
    //
    //
    //        new MableInterpreter().execute(doc);
    //    }
    //
    //    @Test
    //    public void fixedStepSingleWt() throws IOException, AnalysisException {
    //
    //        InputStream contextFile = this.getClass().getResourceAsStream("configs/singleExternal.json");
    //        ARootDocument doc = new MableSpecificationGenerator(Framework.FMI2, true,
    //                UnitRelationship.of(Paths.get("src", "test", "resources", "watertank_env.json").toAbsolutePath().toFile())).generate(
    //                Stream.of(Paths.get("src", "test", "resources", "FMI2.mabl").toAbsolutePath().toString(),
    //                        Paths.get("src", "test", "resources", "fixedstep_single_wt.mabl").toAbsolutePath().toString(),
    //                        Paths.get("src", "test", "resources", "CSV.mabl").toAbsolutePath().toString()).map(File::new).collect(Collectors.toList()),
    //                contextFile);
    //
    //
    //        new MableInterpreter().execute(doc);
    //    }
    //

    @Test
    public void jsonParseTest() throws IOException {

        try (InputStream is = this.getClass().getResourceAsStream("plugin_configuration_1.json")) {
            Map<String, String> config = PluginFactory.parsePluginConfiguration(is);
            Assert.assertNotNull(config);
            Assert.assertTrue("key missing", config.containsKey("demo-0.0.1"));
        }
    }
}
