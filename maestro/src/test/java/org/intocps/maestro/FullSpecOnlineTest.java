package org.intocps.maestro;

import org.intocps.maestro.testutils.RequiresFmu;
import org.intocps.maestro.testutils.TestFmu;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.INode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static org.intocps.maestro.parser.MablParserUtil.parse;

@RequiresFmu(TestFmu.FMI2_FUNCTION_TEST)
public class FullSpecOnlineTest extends FullSpecTest {

    private static Stream<Arguments> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "specifications", "online").toFile().listFiles()))
                .filter(n -> !n.getName().startsWith(".")).map(f -> Arguments.arguments(f.getName(), f));
    }
    
    @Override
    protected void postParse(Mabl mabl) throws AnalysisException {
        super.postParse(mabl);
    }
}
