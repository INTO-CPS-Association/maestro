package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.parser.MablParserUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class LibTest {

    @Test
    public void testFmi2() throws IOException {
        IErrorReporter errorReporter = new ErrorReporter();
        List<ARootDocument> allDocuments =
                MablParserUtil.parse(Arrays.asList(new File("src/main/resources/org/intocps/maestro/typechecker/FMI2" + ".mabl")), errorReporter);

        errorReporter.printErrors(new PrintWriter(System.err, true));
        Assertions.assertEquals(0, errorReporter.getErrorCount(), "There should be not parse errors");

        //        TypeChecker typeChecker = new TypeChecker(errorReporter);
        //        typeChecker.typeCheck(allDocuments, null);
        //
        //        errorReporter.printErrors(new PrintWriter(System.out, true));
        //        Assert.assertEquals(0, errorReporter.getErrorCount());
    }
}
