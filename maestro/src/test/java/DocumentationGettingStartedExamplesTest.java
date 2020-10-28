import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.ErrorReporter;
import org.intocps.maestro.Mabl;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * This test class uses files within resources/documentation_example_files
 */
public class DocumentationGettingStartedExamplesTest {

    @Test
    public void initial() throws Exception {
        File testFilesDirectory = new File("src/test/resources/documentation_example_files");
        List<File> sourceFiles = Arrays.asList(new File(testFilesDirectory, "example1.mabl"));
        File specificationDirectory = new File("target", "DocumentationGettingStartedExamplesTest/initial/specification");
        File workingDirectory = new File("target", "DocumentationGettingStartedExamplesTest/initial/working");

        FileUtils.deleteDirectory(workingDirectory);


        IErrorReporter reporter = new ErrorReporter();

        Mabl mabl = new Mabl(specificationDirectory, workingDirectory);
        mabl.setReporter(reporter);
        mabl.setVerbose(true);

        mabl.parse(sourceFiles);
        if (reporter.getErrorCount() > 0) {
            reporter.printErrors(new PrintWriter(System.err, true));
            assert (false);
        } else {
            new MableInterpreter(new DefaultExternalValueFactory(workingDirectory,
                    IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8))).execute(mabl.getMainSimulationUnit());
            FullSpecTest.compareCsvResults(new File(testFilesDirectory, "outputs.csv"), new File(workingDirectory, "outputs.csv"));
        }
    }


}