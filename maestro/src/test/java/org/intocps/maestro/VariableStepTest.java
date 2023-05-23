package org.intocps.maestro;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.typechecker.TypeChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class VariableStepTest {
    @Test
    public void variableStepSizeMablTest() throws Exception {
        File testFilesDirectory = new File(Objects.requireNonNull(VariableStepTest.class.getClassLoader().getResource("variable_step")).getPath());

        List<File> sourceFiles = Collections.singletonList(
                new File(testFilesDirectory.toPath().resolve("variableStepSizeMablTest").resolve("variableStepTest.mabl").toString()));
        File specificationDirectory = new File("target", "variable_step/spec");
        File workingDirectory = new File("target", "variable_step/working");

        FileUtils.deleteDirectory(workingDirectory);

        IErrorReporter reporter = new ErrorReporter();

        Mabl mabl = new Mabl(specificationDirectory, workingDirectory);
        mabl.setReporter(reporter);
        mabl.setVerbose(true);

        mabl.parse(sourceFiles);
        var tcRes = mabl.typeCheck();
        mabl.verify(Framework.FMI2);
        if (reporter.getErrorCount() > 0) {
            reporter.printErrors(new PrintWriter(System.err, true));
            assert (false);
        } else {
            reporter.printWarnings(new PrintWriter(System.out, true));
            new MableInterpreter(new DefaultExternalValueFactory(workingDirectory, name -> TypeChecker.findModule(tcRes.getValue(), name),
                    IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8))).execute(mabl.getMainSimulationUnit());

            FullSpecTest.compareCsvResults(
                    new File(testFilesDirectory.toPath().resolve("variableStepSizeMablTest").resolve("expectedoutputs.csv").toString()),
                    new File(workingDirectory, "outputs.csv"));
        }

    }

    // This only tests the interface i.e. that values are passed correctly
    @Test
    public void variableStepWithDerivativesTest() {
        Assertions.assertDoesNotThrow(() -> interpretMablSpec(
                (f) -> Collections.singletonList(new File(f.toPath().resolve("variableStepWithDerivativesTest").resolve("spec.mabl").toString()))));
    }

    // This only tests the interface i.e. if the values are passed correctly
    @Test
    public void invalidDerivativesArraySizeTest() {
        Assertions.assertThrows(Exception.class, () -> interpretMablSpec(
                (f) -> Collections.singletonList(new File(f.toPath().resolve("invalidDerivativesArrayLengthTest").resolve("spec.mabl").toString()))));
    }

    private void interpretMablSpec(Function<File, List<File>> generateSourceFilesFunc) throws Exception {
        List<File> sourceFiles = generateSourceFilesFunc.apply(
                new File(Objects.requireNonNull(VariableStepTest.class.getClassLoader().getResource("variable_step")).getPath()));

        IErrorReporter reporter = new ErrorReporter();
        File specificationDirectory = new File("target", "variable_step/spec");
        File workingDirectory = new File("target", "variable_step/working");

        FileUtils.deleteDirectory(workingDirectory);

        Mabl mabl = new Mabl(specificationDirectory, workingDirectory);
        mabl.setReporter(reporter);
        mabl.setVerbose(true);

        mabl.parse(sourceFiles);
        var tcRes = mabl.typeCheck();
        mabl.verify(Framework.FMI2);
        if (reporter.getErrorCount() > 0) {
            reporter.printErrors(new PrintWriter(System.err, true));
            Assertions.fail("Spec has errors");
        }

        reporter.printWarnings(new PrintWriter(System.out, true));
        new MableInterpreter(new DefaultExternalValueFactory(workingDirectory, name -> TypeChecker.findModule(tcRes.getValue(), name),
                IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8))).execute(mabl.getMainSimulationUnit());
    }
}
