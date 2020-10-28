import com.fasterxml.jackson.databind.ObjectMapper;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.ErrorReporter;
import org.intocps.maestro.Mabl;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.api.FixedStepSizeAlgorithm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.Fmi2EnvironmentConfiguration;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.template.MaBLTemplateConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


@RunWith(Parameterized.class)
public class FullSpecTest {

    final File directory;
    private final String name;

    public FullSpecTest(String name, File directory) {
        this.name = name;
        this.directory = directory;
    }

    @Parameterized.Parameters(name = "{index} {0}")
    public static Collection<Object[]> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "specifications", "full").toFile().listFiles()))
                .map(f -> new Object[]{f.getName(), f}).collect(Collectors.toList());
    }

    private static TestJsonObject getTestJsonObject(File directory) throws java.io.IOException {
        TestJsonObject testJsonObject = null;
        File test = new File(directory, "test.json");

        if (test.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            testJsonObject = mapper.readValue(test, TestJsonObject.class);
        } else {
            testJsonObject = new TestJsonObject();
            testJsonObject.autoGenerate = false;
        }
        return testJsonObject;
    }

    static File getWorkingDirectory(File base) {
        String s = "target/" + base.getAbsolutePath().substring(
                base.getAbsolutePath().replace(File.separatorChar, '/').indexOf("src/test/resources/") + ("src" + "/test" + "/resources/").length());

        File workingDir = new File(s.replace('/', File.separatorChar));
        if (!workingDir.exists()) {
            workingDir.mkdirs();
        }
        return workingDir;
    }

    private static List<String> fileToLines(InputStream filename) {
        List<String> lines = new LinkedList<>();
        String line = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(filename));
            while ((line = in.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    public static void assertResultEqualsDiff(InputStream actual, InputStream expected) {
        if (actual != null && expected != null) {
            List<String> original = fileToLines(actual);
            List<String> revised = fileToLines(expected);

            // Compute diff. Get the Patch object. Patch is the container for computed deltas.
            Patch patch = DiffUtils.diff(original, revised);

            for (Delta delta : patch.getDeltas()) {
                System.err.println(delta);
                Assert.fail("Expected result and actual differ: " + delta);
            }

        }
    }

    public static void compareCsvResults(File expectedCsvFile, File actualCsvFile) throws IOException {

        if (Boolean.parseBoolean(System.getProperty("TEST_CREATE_OUTPUT_CSV_FILES", "false")) && actualCsvFile.exists()) {
            System.out.println("Storing outputs csv file in specification directory to be used in future tests.");
            Files.copy(actualCsvFile.toPath(), expectedCsvFile.toPath(), REPLACE_EXISTING);
        }

        boolean actualOutputsCsvExists = actualCsvFile.exists();
        boolean expectedOutputsCsvExists = expectedCsvFile.exists();
        if (actualOutputsCsvExists && expectedOutputsCsvExists) {
            assertResultEqualsDiff(new FileInputStream(actualCsvFile), new FileInputStream(expectedCsvFile));
        } else {

            StringBuilder sb = new StringBuilder();

            sb.append("Cannot compare CSV files.\n");
            if (!actualOutputsCsvExists) {
                sb.append("The actual outputs csv file does not exist.\n");
            }
            if (!expectedOutputsCsvExists) {
                sb.append("The expected outputs csv file does not exist.\n");
            }
            System.out.println(sb.toString());

        }
    }

    @Test
    public void test() throws Exception {

        File workingDirectory = getWorkingDirectory(this.directory);

        TestJsonObject testJsonObject = getTestJsonObject(directory);
        boolean useTemplate = testJsonObject != null && testJsonObject.autoGenerate;

        IErrorReporter reporter = new ErrorReporter();

        Mabl mabl = new Mabl(directory, workingDirectory);
        mabl.setReporter(reporter);
        mabl.setVerbose(true);

        mabl.parse(getSpecificationFiles());
        postParse(mabl);
        if (useTemplate) {

            Fmi2EnvironmentConfiguration simulationConfiguration =
                    new ObjectMapper().readValue(new File(directory, "env.json"), Fmi2EnvironmentConfiguration.class);


            Fmi2SimulationEnvironmentConfiguration simulationEnvironmentConfiguration =
                    new ObjectMapper().readValue(new File(directory, "env.json"), Fmi2SimulationEnvironmentConfiguration.class);

            MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder builder =
                    MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder.getBuilder().useInitializer(testJsonObject.initialize, "{}")
                            .setFramework(Framework.FMI2).setFrameworkConfig(Framework.FMI2, simulationEnvironmentConfiguration);

            Fmi2SimulationEnvironment environment = Fmi2SimulationEnvironment.of(simulationEnvironmentConfiguration, reporter);
            if (testJsonObject.useLogLevels) {
                builder.setLogLevels(environment.getLogLevels());
            }

            if (testJsonObject.simulate && simulationConfiguration.algorithm instanceof Fmi2EnvironmentConfiguration.FixedStepAlgorithmConfig) {
                Fmi2EnvironmentConfiguration.FixedStepAlgorithmConfig a =
                        (Fmi2EnvironmentConfiguration.FixedStepAlgorithmConfig) simulationConfiguration.algorithm;
                builder.setStepAlgorithm(new FixedStepSizeAlgorithm(simulationConfiguration.endTime, a.size)).setVisible(true).setLoggingOn(true);
            }

            MaBLTemplateConfiguration configuration = builder.build();

            mabl.generateSpec(configuration);
        }

        mabl.expand();


        if (reporter.getErrorCount() > 0) {
            reporter.printErrors(new PrintWriter(System.err, true));
            Assert.fail();
        }

        mabl.dump(workingDirectory);
        Assert.assertTrue("Spec file must exist", new File(workingDirectory, Mabl.MAIN_SPEC_DEFAULT_FILENAME).exists());
        Assert.assertTrue("Spec file must exist", new File(workingDirectory, Mabl.MAIN_SPEC_DEFAULT_RUNTIME_FILENAME).exists());
        System.out.println(PrettyPrinter.print(mabl.getMainSimulationUnit()));
        new MableInterpreter(
                new DefaultExternalValueFactory(workingDirectory, IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8)))
                .execute(mabl.getMainSimulationUnit());


        compareCsvResults(new File(directory, "outputs.csv"), new File(workingDirectory, "outputs.csv"));
    }

    protected void postParse(Mabl mabl) throws AnalysisException {

    }

    protected List<File> getSpecificationFiles() {
        return Arrays.stream(Objects.requireNonNull(directory.listFiles((file, s) -> s.toLowerCase().endsWith(".mabl"))))
                .collect(Collectors.toList());
    }
}
