package org.intocps.maestro;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.INode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static org.intocps.maestro.parser.MablParserUtil.parse;


public class FullSpecOnlineTest extends FullSpecTest {

    private static Stream<Arguments> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "specifications", "online").toFile().listFiles()))
                .filter(n -> !n.getName().startsWith(".")).map(f -> Arguments.arguments(f.getName(), f));
    }

    @Override
    @ParameterizedTest(name = "{index} \"{0}\"")
    @MethodSource("data")
    public void test(String name, File directory) throws Exception {
        for (INode spec : parse(getSpecificationFiles(directory))) {
            OnlineTestUtils.download(OnlineTestUtils.collectFmus(spec, false));
        }
        super.test(name, directory);
    }

    @Override
    protected void postParse(Mabl mabl) throws AnalysisException {
        OnlineTestUtils.collectFmus(mabl.getMainSimulationUnit(), true);
    }
}
