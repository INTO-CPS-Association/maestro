package org.intocps.maestro;

import org.apache.commons.io.FileUtils;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.framework.fmi2.api.mabl.BaseApiTest;
import org.intocps.maestro.interpreter.InterpreterException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;


public class InterpreterTest extends BaseApiTest {

    public static Collection<Object> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "InterpreterTest", "statements").toFile().listFiles()))
                .map(f -> {
                    try {
                        return new Object[]{f.getName(), FileUtils.readFileToString(f, StandardCharsets.UTF_8)};
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new Vector<Object[]>();
                    }
                }).collect(Collectors.toList());
    }

    public static Collection<Object> expectedExceptionData() {
        return Arrays
                .stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "InterpreterTest", "expectedException").toFile().listFiles()))
                .map(f -> {
                    try {
                        return new Object[]{f.getName(), FileUtils.readFileToString(f, StandardCharsets.UTF_8)};
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new Vector<Object[]>();
                    }
                }).collect(Collectors.toList());
    }

    @ParameterizedTest(name = "{index} \"{0}\"")
    @MethodSource("data")
    public void test(String name,
            String spec) throws IOException, AnalysisException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

        String templateSpec = FileUtils
                .readFileToString(Paths.get("src", "test", "resources", "InterpreterTest", "templates", "with-assert.mabl").toFile(),
                        StandardCharsets.UTF_8);

        String aggrigatedSpec = templateSpec.replace("@replaceme", spec);

        check(aggrigatedSpec, "");
    }

    @ParameterizedTest(name = "{index} \"{0}\"")
    @MethodSource("expectedExceptionData")
    public void test2(String name,
            String spec) throws IOException, AnalysisException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

        Assertions.assertThrows(InterpreterException.class, () -> {
            check(spec, "");
        });
    }
}
