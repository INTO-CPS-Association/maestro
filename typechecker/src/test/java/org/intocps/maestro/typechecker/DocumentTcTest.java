package org.intocps.maestro.typechecker;

import org.antlr.v4.runtime.CharStreams;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.parser.MablParserUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


public class DocumentTcTest {

    public static Collection<Object[]> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "modules").toFile().listFiles()))
                .filter(f -> f.getName().endsWith(".mabl")).map(f -> new Object[]{f.getName(), f}).collect(Collectors.toList());
    }


    @ParameterizedTest(name = "{index} \"{0}\"")
    @MethodSource("data")
    public void test(String name, File spec) throws IOException, AnalysisException {
        IErrorReporter errorReporter = new ErrorReporter();
        ARootDocument doc = MablParserUtil.parse(CharStreams.fromPath(spec.toPath()), errorReporter);
        PrettyPrinter.printLineNumbers(doc);

        new TypeChecker(errorReporter).typeCheck(Collections.singletonList(doc), new Vector<>());

        errorReporter.printErrors(new PrintWriter(System.out, true));
        errorReporter.printWarnings(new PrintWriter(System.out, true));

        if (spec.getName().endsWith("success.mabl")) {
            Assertions.assertEquals(0, errorReporter.getErrorCount());
        } else {
            Assertions.assertTrue(errorReporter.getErrorCount() > 0);
        }
    }
}
