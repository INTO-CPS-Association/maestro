package org.intocps.maestro;

import org.junit.jupiter.params.provider.Arguments;

import java.io.File;
import java.io.IOException;
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

public class Fmi3ModuleTest extends FullSpecTest {

    private static Stream<Arguments> data() {
        return Arrays.stream(Objects.requireNonNull(Paths.get("src", "test", "resources", "fmi3","basic").toFile().listFiles()))
                .filter(n -> !n.getName().startsWith(".")).map(f -> Arguments.arguments(f.getName(), f));
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
                content = content.replace("src/test/resources/fmi3/basic", new File("src/test/resources/fmi3/basic").getAbsolutePath());
                Files.write(path, content.getBytes(charset));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        return specFiles;
    }
}
