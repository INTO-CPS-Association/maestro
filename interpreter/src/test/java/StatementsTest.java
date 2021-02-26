import org.antlr.v4.runtime.CharStreams;
import org.apache.commons.io.FileUtils;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.parser.MablParserUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;


@RunWith(Parameterized.class)
public class StatementsTest {

    final String spec;
    private final String name;

    public StatementsTest(String name, String spec) {
        this.name = name;
        this.spec = spec;
    }

    @Parameterized.Parameters(name = "{index} \"{0}\"")
    public static Collection<Object> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "statements").toFile().listFiles())).map(f -> {
            try {
                return new Object[]{f.getName(), FileUtils.readFileToString(f, StandardCharsets.UTF_8)};
            } catch (IOException e) {
                e.printStackTrace();
                return new Vector<Object[]>();
            }
        }).collect(Collectors.toList());
    }

    @Test
    public void test() throws IOException, AnalysisException {

        String templateSpec =
                FileUtils.readFileToString(Paths.get("src", "test", "resources", "templates", "with-assert.mabl").toFile(), StandardCharsets.UTF_8);

        String aggrigatedSpec = templateSpec.replace("@replaceme", spec);

        ARootDocument doc = MablParserUtil.parse(CharStreams.fromStream(new ByteArrayInputStream(aggrigatedSpec.getBytes())));


    }
}
