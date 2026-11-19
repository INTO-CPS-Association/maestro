package org.intocps.maestro.fmi3;

import org.intocps.maestro.FullSpecTest;
import org.intocps.maestro.testutils.RequiresFmu;
import org.intocps.maestro.testutils.TestFmu;
import org.junit.jupiter.params.provider.Arguments;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static org.intocps.maestro.fmi3.Fmi3ModuleTest.hasMablSpec;

@RequiresFmu({TestFmu.BOUNCING_BALL, TestFmu.FEEDTHROUGH, TestFmu.SIGNAL_GENERATOR})
public class Fmi3ModuleReferenceFmusTest extends FullSpecTest {

    private static Stream<Arguments> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "fmi3", "reference").toFile().listFiles()))
                .filter(n -> !n.getName().startsWith(".")).filter(hasMablSpec).map(f -> Arguments.arguments(f.getName(), f));
    }

}
