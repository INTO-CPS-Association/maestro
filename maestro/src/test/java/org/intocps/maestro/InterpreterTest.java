package org.intocps.maestro;

import org.apache.commons.io.FileUtils;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.framework.fmi2.api.mabl.BaseApiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class InterpreterTest extends BaseApiTest {

    final String spec;
    private final String name;

    public InterpreterTest(String name, String spec) {
        this.name = name;
        this.spec = spec;
    }

    @Parameterized.Parameters(name = "{index} \"{0}\"")
    public static Collection<Object> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "InterpreterTest" , "statements").toFile().listFiles())).map(f -> {
            try {
                return new Object[]{f.getName(), FileUtils.readFileToString(f, StandardCharsets.UTF_8)};
            } catch (IOException e) {
                e.printStackTrace();
                return new Vector<Object[]>();
            }
        }).collect(Collectors.toList());
    }

    @Test
    public void test() throws IOException, AnalysisException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

        String templateSpec =
                FileUtils.readFileToString(Paths.get("src", "test", "resources", "InterpreterTest", "templates", "with-assert.mabl").toFile(),
                        StandardCharsets.UTF_8);

        String aggrigatedSpec = templateSpec.replace("@replaceme", spec);

        check(aggrigatedSpec,"");
    }
}
