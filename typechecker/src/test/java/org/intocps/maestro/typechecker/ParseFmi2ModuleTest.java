package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.parser.MablParserUtil;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

@Ignore
public class ParseFmi2ModuleTest {

    @Test
    public void parseSpecification() throws IOException, AnalysisException {
        IErrorReporter errorReporter = new ErrorReporter();
        List<ARootDocument> allDocuments = MablParserUtil.parse(Arrays
                .asList(new File("src/main/resources/org/intocps/maestro/typechecker/FMI2.mabl"),
                        new File("src/main/resources/org/intocps/maestro" + "/typechecker/Math.mabl"),
                        new File("src/main/resources/org/intocps/maestro/typechecker/CSV.mabl"),
                        new File("src/main/resources/org/intocps/maestro/typechecker/DataWriter.mabl"),
                        new File("src/main/resources/org/intocps/maestro/typechecker/Logger.mabl"),
                        new File("src/test/resources" + "/singlewatertank.mabl")));

        TypeChecker typeChecker = new TypeChecker(errorReporter);
        typeChecker.typeCheck(allDocuments, null);

        errorReporter.printErrors(new PrintWriter(System.out, true));
        Assert.assertEquals(0, errorReporter.getErrorCount());

    }
}
