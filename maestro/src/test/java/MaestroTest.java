import org.intocps.maestro.MableSpecificationGenerator;
import org.intocps.maestro.ast.ARootDocument;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MaestroTest {

    @Test(expected = RuntimeException.class)
    public void simpleParseTest() throws IOException {


        new MableSpecificationGenerator(true).generate(Stream.of(Paths.get("src", "test", "resources", "FMI2.mabl").toAbsolutePath().toString(),
                Paths.get("src", "test", "resources", "jacobian.mabl").toAbsolutePath().toString()).map(File::new).collect(Collectors.toList()));

    }

    @Test
    public void singleExternal() throws IOException, AnalysisException {

        ARootDocument doc = new MableSpecificationGenerator(true).generate(
                Stream.of(Paths.get("src", "test", "resources", "FMI2.mabl").toAbsolutePath().toString(),
                        Paths.get("src", "test", "resources", "single_external.mabl").toAbsolutePath().toString()).map(File::new)
                        .collect(Collectors.toList()));


        new MableInterpreter().execute(doc);
    }
}
