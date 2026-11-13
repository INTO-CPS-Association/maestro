package org.intocps.maestro.fmi3;

import org.intocps.maestro.FullSpecTest;
import org.intocps.maestro.TestFmuDownloads;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.provider.Arguments;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.fmi3.Fmi3ModuleTest.hasMablSpec;

public class Fmi3ModuleReferenceFmusTest extends FullSpecTest {

    private static Stream<Arguments> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "fmi3", "reference").toFile().listFiles()))
                .filter(n -> !n.getName().startsWith(".")).filter(hasMablSpec).map(f -> Arguments.arguments(f.getName(), f));
    }

    protected List<File> getSpecificationFiles(File specFolder) {

        List<File> specFiles = Arrays.stream(Objects.requireNonNull(specFolder.listFiles((file, s) -> s.toLowerCase().endsWith(".mabl"))))
                .collect(Collectors.toList());

        //lets make sure we have replaced any relative URIs for fmu loading
        for (File f : specFiles) {
            Path path = f.toPath();
            Charset charset = StandardCharsets.UTF_8;

            String content = null;
            try {
                content = Files.readString(path, charset);
                content = content.replace("file:", destination.toAbsolutePath().toUri().toString());
                Files.write(path, content.getBytes(charset));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        return specFiles;
    }

    public final static Path destination = Paths.get("target", Fmi3ModuleReferenceFmusTest.class.getSimpleName(), "cache", "test-fmus-0.0.2");

    @BeforeAll
    public static void downloadReferenceFmus() throws IOException {

        TestFmuDownloads.download(List.of(TestFmuDownloads.fmuUrl("BouncingBall.fmu"), TestFmuDownloads.fmuUrl("Feedthrough.fmu"),
            TestFmuDownloads.fmuUrl("SignalGenerator.fmu")), destination);
    }

}
