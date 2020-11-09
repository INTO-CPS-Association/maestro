import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.parser.MablParserUtil;
import org.intocps.maestro.typechecker.TypeChecker;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class parseFmi2Module {
    @Test
    public void parseFMI2ModuleTest() throws IOException, AnalysisException {
        TypeChecker checker = new TypeChecker(new IErrorReporter.SilentReporter());
        List<ARootDocument> rootDoc = MablParserUtil.parse(Arrays.asList(new File("src/main/resources/FMI2.mabl")));
        checker.addModules(rootDoc);
    }

    @Test
    public void parseSpecification() throws IOException, AnalysisException {
        TypeChecker checker = new TypeChecker(new IErrorReporter.SilentReporter());
        List<ARootDocument> modules =
                MablParserUtil.parse(Arrays.asList(new File("src/main/resources/FMI2.mabl"), new File("src/main/resources" + "/Math.mabl")));
        checker.addModules(modules);
        List<ARootDocument> parse = MablParserUtil.parse(Arrays.asList(new File("src/test/resources/specification.mabl")));
        checker.typecheck(parse.get(0));
    }
}
