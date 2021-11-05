package org.intocps.maestro;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

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
    public void importDTTutorial() throws IOException {
        File dtTutorialDirPath = Paths.get("src", "test", "resources", "org", "into-cps", "maestro", "faultinjection", "dt-tutorial").toFile();
        File faultInjectMabl = new File(dtTutorialDirPath, "FaultInject.mabl");
        File multiModel = new File(dtTutorialDirPath, "multimodel.json");
        File executionParametersPath = new File(dtTutorialDirPath, "executionParameters.json");
        File resultsPath = new File("importDTTutorial.resultsFolder");
        if (resultsPath.exists()) {
            FileUtils.deleteDirectory(resultsPath);
        }

        String arguments = String.format(Locale.US, "import sg1 %s %s -output %s ", faultInjectMabl, multiModel, resultsPath);
        String[] s = arguments.split(" ");

        int exitCode = new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(s);
        Assert.assertEquals(0, exitCode);
    }
}
