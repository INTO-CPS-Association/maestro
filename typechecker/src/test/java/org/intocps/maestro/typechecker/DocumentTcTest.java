package org.intocps.maestro.typechecker;

import org.antlr.v4.runtime.CharStreams;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.parser.MablParserUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class DocumentTcTest {

    final File spec;
    private final String name;

    public DocumentTcTest(String name, File spec) {
        this.name = name;
        this.spec = spec;
    }

    @Parameterized.Parameters(name = "{index} \"{1}\" in {0}")
    public static Collection<Object[]> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "modules").toFile().listFiles()))
                .filter(f -> f.getName().endsWith(".mabl")).map(f -> new Object[]{f.getName(), f}).collect(Collectors.toList());
    }


    @Test
    public void test() throws IOException, AnalysisException {
        IErrorReporter errorReporter = new ErrorReporter();
        ARootDocument doc = MablParserUtil.parse(CharStreams.fromPath(spec.toPath()), errorReporter);
        PrettyPrinter.printLineNumbers(doc);

        new TypeChecker(errorReporter).typeCheck(Collections.singletonList(doc), new Vector<>());

        errorReporter.printErrors(new PrintWriter(System.out, true));
        errorReporter.printWarnings(new PrintWriter(System.out, true));

        Assert.assertEquals(0, errorReporter.getErrorCount());
    }
}
