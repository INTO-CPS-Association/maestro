package org.intocps.maestro.testutils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class FmuTestModelRepository {
    public static final String TEST_FMU_RELEASE_URL = TestFmu.RELEASE_URL;
    public static final Path TEST_FMU_CACHE = Paths.get("target", "online-cache", "test-fmus");

    private FmuTestModelRepository() {
    }

    public static URL url(TestFmu model) throws MalformedURLException {
        return URI.create(TEST_FMU_RELEASE_URL + model.assetName()).toURL();
    }

    public static Path cachePath(TestFmu model) {
        return TEST_FMU_CACHE.resolve(model.fileName());
    }

    public static synchronized void download(TestFmu... models) throws IOException {
        for (TestFmu model : models) {
            download(model, url(model), TEST_FMU_CACHE);
        }
    }

    static void download(TestFmu model, URL source, Path cache) throws IOException {
        Files.createDirectories(cache);
        Path marker = cache.resolve(".release");
        if (!Files.isRegularFile(marker) || !Files.readString(marker).equals(TEST_FMU_RELEASE_URL)) {
            for (TestFmu knownModel : TestFmu.values()) {
                Files.deleteIfExists(cache.resolve(knownModel.fileName()));
            }
            Files.writeString(marker, TEST_FMU_RELEASE_URL);
        }
        Path destination = cache.resolve(model.fileName());
        if (Files.isRegularFile(destination)) {
            return;
        }
        Path temporary = Files.createTempFile(cache, "fmu-", ".tmp");
        try {
            URLConnection connection = source.openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(120000);
            try (InputStream input = connection.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!containsModelDescription(temporary)) {
                throw new IOException("Missing modelDescription.xml in " + source);
            }
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static boolean containsModelDescription(Path fmuPath) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(fmuPath))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals("modelDescription.xml")) {
                    return true;
                }
            }
        }
        return false;
    }
}