package org.intocps.maestro;

import org.junit.jupiter.params.provider.Arguments;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class Fmi3ModuleTest extends FullSpecTest {

    private static Stream<Arguments> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "fmi3").toFile().listFiles()))
                .filter(n -> !n.getName().startsWith(".")).map(f -> Arguments.arguments(f.getName(), f));
    }
}
