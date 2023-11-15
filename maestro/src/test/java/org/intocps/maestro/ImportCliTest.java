package org.intocps.maestro;

import org.apache.commons.io.FileUtils;
import org.intocps.maestro.fmi3.Fmi3ModuleReferenceFmusTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class ImportCliTest {

    @BeforeAll
    public static void downloadFmus() throws IOException {
        Fmi3ModuleReferenceFmusTest.downloadReferenceFmus();

    }

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
    public void importTestExistingFmu3Ft() {

        String arguments = String.format(Locale.US, "import sg1 --interpret --dump-intermediate --inline-framework-config -output %s %s",
                getOutputPath().toAbsolutePath(), "/Users/kgl/data/au/into-cps-association/maestro/maestro/src/test/resources/fmi3/reference/siggen-feedthrough/mm.json");
        String[] s = arguments.split(" ");

        int exitCode = new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(s);
        Assert.assertEquals(0, exitCode);

    }

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
        FileUtils.copyFile(Paths.get("src", "test", "resources", "singlewatertank-20sim.fmu").toFile(), destFile);
        FileUtils.copyFile(Paths.get("src", "test", "resources", "watertankcontroller-c.fmu").toFile(),
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
        FileUtils.copyFile(Paths.get("src", "test", "resources", "singlewatertank-20sim.fmu").toFile(), destFile);
        FileUtils.copyFile(Paths.get("src", "test", "resources", "watertankcontroller-c.fmu").toFile(),
                new File(outputPath.toFile(), "watertankcontroller-c.fmu"));

        String arguments = String.format(Locale.US, "import sg1 --inline-framework-config --fmu-search-path %s --fmu-search-path %s -output %s %s %s",
                Paths.get("src", "test", "resources"), outputPath.toAbsolutePath(), outputPath.toAbsolutePath(),
                simulationConfigPath.getAbsolutePath(), new File(resourcesConfigPrefix, "config-relative-model-swap.json").toPath());
        String[] s = arguments.split(" ");

        int exitCode = new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(s);
        Assert.assertEquals(0, exitCode);

    }
}
