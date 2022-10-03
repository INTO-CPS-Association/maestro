package org.intocps.maestro;

import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.cli.MablCliUtil;
import org.intocps.maestro.interpreter.*;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

import java.io.*;
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

        ITransitionManager.ISpecificationProvider specificationProvider = new ITransitionManager.ISpecificationProvider() {
            final Map<Path, ARootDocument> candidates = new HashMap<>() {{
                put(stage2Spec.toPath(), utilStage2.getMainSimulationUnit());
            }};

            @Override
            public Map<Path, ARootDocument> get() {
                return candidates;
            }

            @Override
            public Map<Path, ARootDocument> get(String name) {
                return get();
            }

            @Override
            public void remove(ARootDocument specification) {
                candidates.entrySet().stream().filter(map -> map.getValue().equals(specification)).map(Map.Entry::getKey).findFirst()
                        .ifPresent(candidates::remove);
            }
        };

        ITransitionManager tm = new TransitionManager(specificationProvider) {

            @Override
            public void transfer(Interpreter interpreter, ITTransitionInfo info) {
                try {
                    super.transfer(new Interpreter(interpreter.getLoadFactory().changeWorkingDirectory(stageOutput2, null), this), info);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        new MableInterpreter(new DefaultExternalValueFactory(stageOutput.toFile(), c), tm).execute(util.getMainSimulationUnit());

        // check that the two stages was taken
        Assertions.assertTrue(stageOutput.resolve("outputs.csv").toFile().exists());
        Assertions.assertTrue(stageOutput2.resolve("outputs.csv").toFile().exists());

        List<List<String>> records0 = readCsv(stageOutput.resolve("outputs.csv").toFile(), 10);
        Assertions.assertEquals(2, records0.size());
        Assertions.assertEquals("0.0", records0.get(1).get(0));
        List<List<String>> records2 = readCsv(stageOutput2.resolve("outputs.csv").toFile(), 100);
        Assertions.assertTrue(2 < records2.size());
        Assertions.assertEquals("0.0", records0.get(1).get(0));
        Assertions.assertTrue(records2.get(0).stream().anyMatch(col -> col.equals("{x3}.crtlInstance3.valve")));

    }

    List<List<String>> readCsv(File path, int rowLimit) throws IOException {
        List<List<String>> records = new ArrayList<>();
        int rowCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
                if (rowCount++ > rowLimit - 1) {
                    break;
                }
            }
        }
        return records;
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
