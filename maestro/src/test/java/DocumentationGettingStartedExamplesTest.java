import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.FullSpecTest;
import org.intocps.maestro.Mabl;
import org.intocps.maestro.Main;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * This test class uses files within resources/documentation_example_files
 */
public class DocumentationGettingStartedExamplesTest {

    File testFilesDirectory = new File("src/test/resources/documentation_example_files");

    @Test
    public void part1() throws Exception {
        List<File> sourceFiles = Arrays.asList(new File(testFilesDirectory, "example1.mabl"));
        File specificationDirectory = new File("target", "DocumentationGettingStartedExamplesTest/initial/specification");
        File workingDirectory = new File("target", "DocumentationGettingStartedExamplesTest/initial/working");

        FileUtils.deleteDirectory(workingDirectory);


        IErrorReporter reporter = new ErrorReporter();

        Mabl mabl = new Mabl(specificationDirectory, workingDirectory);
        mabl.setReporter(reporter);
        mabl.setVerbose(true);

        mabl.parse(sourceFiles);
        mabl.typeCheck();
        mabl.verify(Framework.FMI2);
        if (reporter.getErrorCount() > 0) {
            reporter.printErrors(new PrintWriter(System.err, true));
            assert (false);
        } else {
            reporter.printWarnings(new PrintWriter(System.out, true));
            new MableInterpreter(new DefaultExternalValueFactory(workingDirectory,
                    IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8))).execute(mabl.getMainSimulationUnit());
            FullSpecTest.compareCsvResults(new File(testFilesDirectory, "expectedoutputs.csv"), new File(workingDirectory, "outputs.csv"));
        }
    }


    @Test
    @Disabled("Ignored due to storing outputs.csv in non-target place")
    public void part2_json_parse() throws Exception {
        File configurationFile = new File(testFilesDirectory, "wt-example-config.json");
        File targetDirectory = new File("target", "DocumentationGettingStartedExamplesTest/part2");
        File intermediateDirectory = new File(targetDirectory, "intermediate");
        File specificationDirectory = new File(targetDirectory, "specification");
        File interpretSpecification = new File(specificationDirectory, "spec.mabl");

        FileUtils.deleteDirectory(targetDirectory);

        Assertions.assertTrue(
                Main.argumentHandler("import", "sg1", "--verify", "FMI2", configurationFile.getAbsolutePath(), "--verbose", "-di", "-output",
                        intermediateDirectory.getAbsolutePath(), "--interpret"));


    }


}