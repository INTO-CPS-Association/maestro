package org.intocps.maestro.fmi3;

import org.apache.commons.io.FileUtils;
import org.intocps.maestro.FullSpecTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.provider.Arguments;

import java.io.*;
import java.net.URL;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    final static Path destination = Paths.get("target", Fmi3ModuleReferenceFmusTest.class.getSimpleName(), "cache");

    @BeforeAll
    public static void downloadReferenceFmus() throws IOException {

        final String referenceFmuBundle = "https://github.com/modelica/Reference-FMUs/releases/download/v0.0.23/Reference-FMUs-0.0.23.zip";
        Path referenceFmuZipPath = destination.resolve("Reference-FMUs.zip");
        if (referenceFmuZipPath.toFile().exists()) {
            return;
        }

        referenceFmuZipPath.toFile().getParentFile().mkdirs();

        FileUtils.copyURLToFile(new URL(referenceFmuBundle), referenceFmuZipPath.toFile());

        //        Path destination = referenceFmuZipPath.getParent();

        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(referenceFmuZipPath.toFile()));
        ZipEntry entry = zipIn.getNextEntry();
        while (entry != null) {
            if (entry.getName().startsWith("3.0") && entry.getName().endsWith(".fmu")) {
                extractFile(zipIn, destination.resolve(Paths.get(entry.getName()).getFileName().toString()));
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    private static final int BUFFER_SIZE = 4096;

    static private void extractFile(ZipInputStream zipIn, Path filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

}
