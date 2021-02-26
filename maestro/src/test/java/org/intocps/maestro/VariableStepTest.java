package org.intocps.maestro;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class VariableStepTest {
    @Test
    public void testMablStepSize() throws Exception {
        File testFilesDirectory = new File(VariableStepTest.class.getClassLoader().getResource("variable_step").getPath());

        List<File> sourceFiles = Arrays.asList(new File(testFilesDirectory, "variableStepTest.mabl"));
        File specificationDirectory = new File("target", "variable_step/spec");
        File workingDirectory = new File("target", "variable_step/working");

        //FileUtils.deleteDirectory(workingDirectory);

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

            //FullSpecTest.compareCsvResults(new File(testFilesDirectory, "outputs.csv"), new File(workingDirectory, "outputs.csv"));
        }

    }
}
