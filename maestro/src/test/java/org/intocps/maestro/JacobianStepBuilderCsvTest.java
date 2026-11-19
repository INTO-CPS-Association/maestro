package org.intocps.maestro;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JacobianStepBuilderCsvTest {
    @TempDir
    Path directory;

    @Test
    void allowsFloatingPointRounding() throws Exception {
        Path expected = directory.resolve("expected.csv");
        Path actual = directory.resolve("actual.csv");
        Files.writeString(expected, "value,state\n0.43664045489294856,true\n");
        Files.writeString(actual, "value,state\n0.4366404548929485,true\n");
        assertDoesNotThrow(() -> JacobianStepBuilderTest.csvCompare(expected.toFile(), actual.toFile()));
    }

    @Test
    void rejectsMeaningfulNumericAndTextDifferences() throws Exception {
        Path expected = directory.resolve("expected.csv");
        Path actual = directory.resolve("actual.csv");
        Files.writeString(expected, "value,state\n1.0,true\n");
        Files.writeString(actual, "value,state\n1.001,true\n");
        assertThrows(AssertionFailedError.class, () -> JacobianStepBuilderTest.csvCompare(expected.toFile(), actual.toFile()));
        Files.writeString(actual, "value,state\n1.0,false\n");
        assertThrows(AssertionFailedError.class, () -> JacobianStepBuilderTest.csvCompare(expected.toFile(), actual.toFile()));
    }
}