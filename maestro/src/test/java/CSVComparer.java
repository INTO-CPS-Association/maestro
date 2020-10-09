import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.antlr.v4.runtime.CharStreams;
import org.intocps.maestro.ErrorReporter;
import org.intocps.maestro.MableSpecificationGenerator;
import org.intocps.maestro.ast.ARootDocument;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.interpreter.DataStore;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.junit.Assert;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class CSVComparer {

    /**
     * This function compares the csv result of a simulation with a csv file stored in the configurationDirectory.
     *
     * @param configurationDirectory
     * @throws Exception
     */
    public static void csvTest(File configurationDirectory, boolean prettyPrint) throws Exception {
        File config = new File(configurationDirectory, "config.json");
        File expectedOutputs = new File(configurationDirectory, "outputs.csv");

        String s = "target/" + config.getAbsolutePath().substring(
                config.getAbsolutePath().replace(File.separatorChar, '/').indexOf("src/test/resources/") +
                        ("src" + "/test" + "/resources/").length());

        File workingDir = new File(s.replace('/', File.separatorChar)).getParentFile();
        if (!workingDir.exists()) {
            workingDir.mkdirs();
        }

        try (InputStream configStream = config.exists() ? new FileInputStream(config) : null) {


            long startTime = System.nanoTime();
            Instant start = Instant.now();

            IErrorReporter reporter = new ErrorReporter();
            FmiSimulationEnvironment environment = FmiSimulationEnvironment.of(new File(configurationDirectory, "env.json"), reporter);

            if (reporter.getErrorCount() > 0) {
                reporter.printErrors(new PrintWriter(System.err, true));
            }
            reporter.printWarnings(new PrintWriter(System.err, true));

            ARootDocument doc = new MableSpecificationGenerator(Framework.FMI2, true, environment)
                    .generate(getSpecificationFiles(configurationDirectory), configStream);

            long stopTime = System.nanoTime();
            Instant end = Instant.now();

            System.out.println("Generated spec time: " + (stopTime - startTime) + " " + Duration.between(start, end));

            System.out.println("############################################################");
            System.out.println("##################### Pretty Print #########################");
            System.out.println("############################################################");

            String sourceCode = PrettyPrinter.print(doc);
            System.out.println(sourceCode);


            ARootDocument reparsedDoc = null;
            try {
                reparsedDoc = MableSpecificationGenerator.parse(CharStreams.fromString(sourceCode));
            } catch (Exception e) {
                System.out.println(PrettyPrinter.printLineNumbers(doc));
                throw e;
            }

            DataStore.GetInstance().setSimulationEnvironment(environment);
            new MableInterpreter(new DefaultExternalValueFactory(workingDir)).execute(reparsedDoc);

            File actualOutputsCsv = new File(workingDir, "outputs.csv");
            boolean actualOutputsCsvExists = Files.exists(actualOutputsCsv.toPath());
            boolean expectedOutputsCsvExists = Files.exists(expectedOutputs.toPath());
            if (actualOutputsCsvExists && expectedOutputsCsvExists) {
                assertResultEqualsDiff(new FileInputStream(actualOutputsCsv), new FileInputStream(expectedOutputs));
            } else {
                if (System.getenv("TEST_CREATE_OUTPUT_CSV_FILES") != null && Boolean.valueOf(System.getenv("TEST_CREATE_OUTPUT_CSV_FILES")) &&
                        actualOutputsCsvExists) {
                    System.out.println("Storing outputs csv file in specification directory to be used in future tests.");
                    Files.copy(actualOutputsCsv.toPath(), expectedOutputs.toPath(), REPLACE_EXISTING);
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
        }
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

    private static List<File> getSpecificationFiles(File directory) {
        return Arrays.stream(Objects.requireNonNull(directory.listFiles((file, s) -> s.toLowerCase().endsWith(".mabl"))))
                .collect(Collectors.toList());
    }


}
