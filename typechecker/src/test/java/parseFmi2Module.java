import org.intocps.maestro.ErrorReporter;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.parser.MablParserUtil;
import org.intocps.maestro.typechecker.TypeCheckVisitor;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class parseFmi2Module {

    @Test
    public void parseSpecification() throws IOException, AnalysisException {
        IErrorReporter errorReporter = new ErrorReporter();
        List<ARootDocument> allDocuments = MablParserUtil.parse(Arrays
                .asList(new File("src/main/resources/FMI2.mabl"), new File("src/main/resources" + "/Math.mabl"),
                        new File("src/main/resources/CSV.mabl"), new File("src/main/resources/DataWriter.mabl"),
                        new File("src/main/resources" + "/Logger.mabl"), new File("src/test/resources/singlewatertank.mabl")));
        TypeCheckVisitor typeCheckVisitor = new TypeCheckVisitor(errorReporter);
        typeCheckVisitor.typecheck(allDocuments);
        errorReporter.printErrors(new PrintWriter(System.out, true));
        assert (errorReporter.getErrorCount() == 0);

    }
}
