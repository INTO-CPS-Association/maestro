package org.intocps.maestro;

import org.apache.commons.io.IOUtils;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.cli.MablCliUtil;
import org.intocps.maestro.interpreter.*;
import org.junit.Assert;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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


        File stage2 = stages.get(1);
        Path stageOutput2 = outputPath.resolve(stage2.getName());
        MablCliUtilTesting utilStage2 = new MablCliUtilTesting(stageOutput2.toFile(), stageOutput2.toFile(), settings);
        utilStage2.setVerbose(true);
        File stage2Spec = stageOutput2.resolve("spec.mabl").toFile();
        utilStage2.parse(Arrays.asList(stage2Spec));

        ITTransitionManager.ISpecificationProvider specificationProvider = new ITTransitionManager.ISpecificationProvider() {
            final Map<String, ARootDocument> candidates = new HashMap<>() {{
                put(stage2Spec.getAbsolutePath(), utilStage2.getMainSimulationUnit());
            }};

            @Override
            public Map<String, ARootDocument> get() {
                return candidates;
            }

            @Override
            public Map<String, ARootDocument> get(String name) {
                return get();
            }

            @Override
            public void remove(ARootDocument specification) {
                candidates.entrySet().stream().filter(map -> map.getValue().equals(specification)).map(Map.Entry::getKey).findFirst()
                        .ifPresent(candidates::remove);
            }
        };

        ITTransitionManager tm = new TransitionManager() {
            @Override
            public ISpecificationProvider getSpecificationProvider() {
                return specificationProvider;
            }

            @Override
            public void transfer(Interpreter interpreter, ITTransitionInfo info) throws AnalysisException {
                try {
                    super.transfer(new Interpreter(new DefaultExternalValueFactory(stageOutput2.toFile(), c), this), info);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        new MableInterpreter(new DefaultExternalValueFactory(stageOutput.toFile(), c), tm).execute(util.getMainSimulationUnit());

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