package org.intocps.maestro;

import org.intocps.maestro.testutils.FmuTestModelRepository;
import org.intocps.maestro.testutils.RequiresFmu;
import org.intocps.maestro.testutils.TestFmu;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.intocps.maestro.core.dto.MultiModel;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

@RequiresFmu({TestFmu.SINGLE_WATER_TANK, TestFmu.WATER_TANK_CONTROLLER, TestFmu.BOUNCING_BALL,
        TestFmu.FEEDTHROUGH, TestFmu.SIGNAL_GENERATOR})
public class ImportCliTest {

    final static File resourcesConfigPrefix = Paths.get("src", "test", "resources", "cli-test").toFile();
    File simulationConfigPath = new File(resourcesConfigPrefix, "simulation-config.json");

    Path getOutputPath() {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        StackTraceElement e = stacktrace[2];//maybe this number needs to be corrected
        String methodName = e.getMethodName();
        return Paths.get("target", this.getClass().getSimpleName(), methodName);
    }

    @Test
    public void importTestRelativeExistingFmus() {

        String arguments = String.format(Locale.US, "import sg1 --inline-framework-config -output %s %s %s", getOutputPath().toAbsolutePath(),
                simulationConfigPath.getAbsolutePath(), new File(resourcesConfigPrefix, "config.json").toPath());
        String[] s = arguments.split(" ");

        int exitCode = new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(s);
        Assert.assertEquals(0, exitCode);

    }

    @ParameterizedTest
    @EnumSource(MultiModel.InitializeLogLevel.class)
    @NullSource
    public void importTestOverrideLogLevel(MultiModel.InitializeLogLevel level, @TempDir Path tempDir) throws IOException {
        Level originalLevel = LogManager.getRootLogger().getLevel();
        try {
            Configurator.setRootLevel(Level.INFO);
            Path override = tempDir.resolve("logging.json");
            Files.writeString(override, "{\"overrideLogLevel\":" + (level == null ? "null" : "\"" + level.name() + "\"") + "}");

            int exitCode = new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(
                    "import", "sg1", "-output", tempDir.resolve("output").toString(),
                    simulationConfigPath.getPath(), new File(resourcesConfigPrefix, "config.json").getPath(), override.toString());

            Assert.assertEquals(0, exitCode);
            Assert.assertEquals(level == null ? Level.INFO : Level.valueOf(level.name()), LogManager.getRootLogger().getLevel());
        } finally {
            Configurator.setRootLevel(originalLevel);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL", "null", "", "INVALID"})
    public void importTestLogLevelBeforeFmuResolution(String level, @TempDir Path tempDir) throws IOException {
        Level originalLevel = LogManager.getRootLogger().getLevel();
        try {
            Configurator.setRootLevel(Level.INFO);
            Path config = tempDir.resolve("config.json");
            String override = level.isEmpty() ? "" : ",\"overrideLogLevel\":" + ("null".equals(level) ? "null" : "\"" + level + "\"");
            Files.writeString(config, "{\"fmus\":{\"{missing}\":\"missing.fmu\"}" + override + "}");
            StringWriter errors = new StringWriter();

            int exitCode = new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true)
                    .setErr(new PrintWriter(errors)).execute("import", "sg1", "--fmu-search-path", tempDir.toString(), config.toString());

            Assert.assertNotEquals(0, exitCode);
            boolean unchanged = level.isEmpty() || "null".equals(level) || "INVALID".equals(level);
            Assert.assertEquals(unchanged ? Level.INFO : Level.valueOf(level), LogManager.getRootLogger().getLevel());
            Assert.assertTrue(errors.toString(), errors.toString().contains(
                    "INVALID".equals(level) ? "InvalidFormatException" : "FileNotFoundException"));
        } finally {
            Configurator.setRootLevel(originalLevel);
        }
    }

    @Test
    public void importTestExistingFmu3Mix() {

        String arguments = String.format(Locale.US, "import sg1 --interpret --dump-intermediate --inline-framework-config -output %s %s %s",
                getOutputPath().toAbsolutePath(), simulationConfigPath.getAbsolutePath(),
                new File(resourcesConfigPrefix, "config-fmi3.json").toPath());
        String[] s = arguments.split(" ");

        int exitCode = new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(s);
        Assert.assertEquals(0, exitCode);

    }

    @Test
    public void importTestSchemaDump() {

        String arguments = String.format(Locale.US, "import sg1 --dump-schemas -output %s",
                getOutputPath().toAbsolutePath());
        String[] s = arguments.split(" ");

        int exitCode = new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(s);
        Assert.assertEquals(0, exitCode);

    }

//    @Test
//    public void importTestExistingFmu3Ft() {
//
//        String arguments = String.format(Locale.US, "import sg1 --interpret --dump-intermediate --inline-framework-config -output %s %s",
//                getOutputPath().toAbsolutePath(), "/Users/kgl/data/au/into-cps-association/maestro/maestro/src/test/resources/fmi3/reference/siggen-feedthrough/mm.json");
//        String[] s = arguments.split(" ");
//
//        int exitCode = new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(s);
//        Assert.assertEquals(0, exitCode);
//
//    }

    //    @Test
    //    public void importTestRelativeFmus() {
    //
    //        String arguments = String.format(Locale.US, "import sg1 --inline-framework-config -output %s %s %s", getOutputPath().toAbsolutePath(),
    //                simulationConfigPath.getAbsolutePath(), new File(resourcesConfigPrefix, "config-relative.json").toPath());
    //        String[] s = arguments.split(" ");
    //        Assertions.assertThrows(FileNotFoundException.class, () -> {
    //            int exitCode = new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(s);
    //            Assert.assertEquals(0, exitCode);
    //        });
    //
    //    }


    @Test
    public void importTestRelativeFmus() throws IOException {

        Path outputPath = getOutputPath();

        File destFile = new File(new File(outputPath.toFile(), "other"), "singlewatertank-20sim.fmu");
        destFile.getParentFile().mkdirs();
        FileUtils.copyFile(FmuTestModelRepository.cachePath(TestFmu.SINGLE_WATER_TANK).toFile(), destFile);
        FileUtils.copyFile(FmuTestModelRepository.cachePath(TestFmu.SINGLE_WATER_TANK).toFile(),
            new File(outputPath.toFile(), "singlewatertank-20sim.fmu"));
        FileUtils.copyFile(FmuTestModelRepository.cachePath(TestFmu.WATER_TANK_CONTROLLER).toFile(),
                new File(outputPath.toFile(), "watertankcontroller-c.fmu"));

        String arguments = String.format(Locale.US, "import sg1 --inline-framework-config --fmu-search-path %s --fmu-search-path %s -output %s %s %s",
                Paths.get("src", "test", "resources"), outputPath.toAbsolutePath(), outputPath.toAbsolutePath(),
                simulationConfigPath.getAbsolutePath(), new File(resourcesConfigPrefix, "config-relative.json").toPath());
        String[] s = arguments.split(" ");

        int exitCode = new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(s);
        Assert.assertEquals(0, exitCode);

    }


    @Test
    public void importTestRelativeFmusSwap() throws IOException {

        Path outputPath = getOutputPath();

        File destFile = new File(new File(outputPath.toFile(), "other"), "singlewatertank-20sim.fmu");
        destFile.getParentFile().mkdirs();
        FileUtils.copyFile(FmuTestModelRepository.cachePath(TestFmu.SINGLE_WATER_TANK).toFile(), destFile);
        FileUtils.copyFile(FmuTestModelRepository.cachePath(TestFmu.SINGLE_WATER_TANK).toFile(),
            new File(outputPath.toFile(), "singlewatertank-20sim.fmu"));
        FileUtils.copyFile(FmuTestModelRepository.cachePath(TestFmu.WATER_TANK_CONTROLLER).toFile(),
                new File(outputPath.toFile(), "watertankcontroller-c.fmu"));

        String arguments = String.format(Locale.US, "import sg1 --inline-framework-config --fmu-search-path %s --fmu-search-path %s -output %s %s %s",
                Paths.get("src", "test", "resources"), outputPath.toAbsolutePath(), outputPath.toAbsolutePath(),
                simulationConfigPath.getAbsolutePath(), new File(resourcesConfigPrefix, "config-relative-model-swap.json").toPath());
        String[] s = arguments.split(" ");

        int exitCode = new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(s);
        Assert.assertEquals(0, exitCode);

    }
}
