package org.intocps.maestro;

import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.cli.MablCliUtil;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.interpreter.TransitionManager;
import org.junit.Assert;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TransitionTest {
    private static Stream<Arguments> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "transition-tests").toFile().listFiles()))
                .map(f -> Arguments.arguments(f.getName(), f));
    }

    Path getOutputPath() {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        StackTraceElement e = stacktrace[2];//maybe this number needs to be corrected
        String methodName = e.getMethodName();
        return Paths.get("target", this.getClass().getSimpleName(), methodName);
    }

    @ParameterizedTest(name = "{index} \"{0}\"")
    @MethodSource("data")
    public void test(String name, File directory) throws Exception {

        List<File> stages = Arrays.stream(Objects.requireNonNull(directory.listFiles(File::isDirectory))).sorted().collect(Collectors.toList());

        Assumptions.assumeTrue(!stages.isEmpty());
        if (stages.isEmpty()) {
            return;
        }

        System.out.println("Starting transition interpretation at: " + stages.get(0));
        Path outputPath = getOutputPath();


        //Make the mabl specs
        for (File stage : stages) {

            Path stageOutput = outputPath.resolve(stage.getName());
            stageOutput.toFile().mkdirs();

            File config = new File(stage, "config.json");
            File runtimeConfig = new File(stage, "simulation-config.json");

            if (!config.exists() || !runtimeConfig.exists()) {
                continue;
            }
            String arguments = String.format(Locale.US,
                    "import sg1 --dump-intermediate --inline-framework-config --fmu-search-path %s -output %s " + "%s " + "%s ",
                    Paths.get("src", "test", "resources"), stageOutput.toAbsolutePath(), config.getAbsolutePath(), runtimeConfig.getAbsolutePath());
            String[] s = arguments.split(" ");

            int exitCode = new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(s);
            Assert.assertEquals(0, exitCode);
        }

        //ok now all specs are ready. let's interpret them

        //how should be test this? One solution is to make the interpreter count @Transfer hits so we can feed in the next model after X hits as we dont
        // know anything about the model except that
        File stage0 = stages.get(0);
        Path stageOutput = outputPath.resolve(stage0.getName());
        Mabl.MableSettings settings = new Mabl.MableSettings();
        settings.dumpIntermediateSpecs = false;
        settings.preserveFrameworkAnnotations = true;
        settings.inlineFrameworkConfig = false;
        MablCliUtilTesting util = new MablCliUtilTesting(stageOutput.toFile(), stageOutput.toFile(), settings);
        util.setVerbose(true);
        util.parse(Arrays.asList(stageOutput.resolve("spec.mabl").toFile()));

        //        util.interpret(stageOutput.resolve("spec.runtime.json").toFile());
        InputStream c = new FileInputStream(stageOutput.resolve("spec.runtime.json").toFile());
        new MableInterpreter(new DefaultExternalValueFactory(stageOutput.toFile(), c), new TransitionManager()).execute(util.getMainSimulationUnit());

    }

    class MablCliUtilTesting extends MablCliUtil {
        public MablCliUtilTesting(File workingDirectory, File debugOutputFolder, Mabl.MableSettings settings) {
            super(workingDirectory, debugOutputFolder, settings);
        }

        public ARootDocument getMainSimulationUnit() {
            return super.mabl.getMainSimulationUnit();
        }
    }
}
