package org.intocps.maestro;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.INode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.intocps.maestro.parser.MablParserUtil.parse;


@RunWith(Parameterized.class)
public class FullSpecOnlineTest extends FullSpecTest {


    public FullSpecOnlineTest(String name, File directory) {
        super(name, directory);
    }

    // TODO: Re-enable build_varstep_ZC_single once derivatives is fixed. See #159
    @Parameterized.Parameters(name = "{index} {0}")
    public static Collection<Object[]> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "specifications", "online").toFile().listFiles()))
                .filter(n -> !n.getName().startsWith(".") && !n.getName().contains("build_varstep_ZC_single")).map(f -> new Object[]{f.getName(), f})
                .collect(Collectors.toList());
    }

    @Override
    @Test
    public void test() throws Exception {
        for (INode spec : parse(getSpecificationFiles(directory))) {
            OnlineTestUtils.download(OnlineTestUtils.collectFmus(spec, false));
        }
        super.test();
    }

    @Override
    protected void postParse(Mabl mabl) throws AnalysisException {
        OnlineTestUtils.collectFmus(mabl.getMainSimulationUnit(), true);
    }
}
