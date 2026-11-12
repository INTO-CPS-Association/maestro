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
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class FmuTestModelRepository {
    public static final String TEST_FMU_RELEASE_URL = "https://github.com/INTO-CPS-Association/test-fmus/releases/download/Release%2F0.0.2/";
    public static final Path TEST_FMU_CACHE = Paths.get("target", "online-cache", "test-fmus-0.0.2");
    private static final Map<String, String> RELEASE_ASSETS = Map.ofEntries(
            Map.entry("fmi2functiontest.fmu", "fmi2--jni--fmi2functiontest.fmu"),
            Map.entry("singlewatertank-20sim.fmu", "fmi2--maestro--singlewatertank-20sim.fmu"),
            Map.entry("watertankcontroller-c.fmu", "fmi2--maestro--watertankcontroller-c.fmu"),
            Map.entry("rollback-end.fmu", "fmi2--maestro--rollback-end.fmu"),
            Map.entry("rollback-test.fmu", "fmi2--maestro--rollback-test.fmu"),
            Map.entry("pump.fmu", "fmi2--maestro--pump.fmu"),
            Map.entry("sink.fmu", "fmi2--maestro--sink.fmu"),
            Map.entry("MassSpringDamper1.fmu", "fmi2--mass-spring-damper--MassSpringDamper1.fmu"),
            Map.entry("MassSpringDamper2.fmu", "fmi2--mass-spring-damper--MassSpringDamper2.fmu"),
            Map.entry("threewatertank1.fmu", "fmi2--three-tank--threewatertank1.fmu"),
            Map.entry("threewatertank2.fmu", "fmi2--three-tank--threewatertank2.fmu"),
            Map.entry("WaterTankController_Standalone.fmu", "fmi2--three-tank--WaterTankController_Standalone.fmu"),
            Map.entry("Body_Block.fmu", "fmi2--line-follow-robot--Body_Block.fmu"),
            Map.entry("Sensor_Block.fmu", "fmi2--line-follow-robot--Sensor_Block.fmu"),
            Map.entry("Sensor_Block_01.fmu", "fmi2--line-follow-robot--Sensor_Block_01.fmu"),
            Map.entry("Sensor_Block_02.fmu", "fmi2--line-follow-robot--Sensor_Block_02.fmu"),
            Map.entry("LineFollower_Examples_SensorBlock1.fmu", "fmi2--line-follow-robot--LineFollower_Examples_SensorBlock1.fmu"),
            Map.entry("LineFollower_Examples_SensorBlock2.fmu", "fmi2--line-follow-robot--LineFollower_Examples_SensorBlock2.fmu"),
            Map.entry("BouncingBall.fmu", "fmi3--reference--BouncingBall.fmu"),
            Map.entry("Feedthrough.fmu", "fmi3--reference--Feedthrough.fmu"),
            Map.entry("fmi3functiontest.fmu", "fmi3--jni--fmi3functiontest.fmu"),
            Map.entry("SignalGenerator.fmu", "fmi3--siggen-feedthrough--SignalGenerator.fmu"));

    private FmuTestModelRepository() {
    }

    public static URL url(String fileName) throws MalformedURLException {
        String asset = RELEASE_ASSETS.get(fileName);
        if (asset == null) {
            throw new IllegalArgumentException("FMU is not in test-fmus release 0.0.2: " + fileName);
        }
        return URI.create(TEST_FMU_RELEASE_URL + asset).toURL();
    }

    public static Path cachePath(String fileName) {
        if (!RELEASE_ASSETS.containsKey(fileName)) {
            throw new IllegalArgumentException("FMU is not in test-fmus release 0.0.2: " + fileName);
        }
        return TEST_FMU_CACHE.resolve(fileName);
    }

    public static boolean isReleaseAsset(String fileName) {
        return RELEASE_ASSETS.containsKey(fileName);
    }

    public static void download(List<URL> urls) throws IOException {
        download(urls, TEST_FMU_CACHE);
    }

    public static void download(List<URL> urls, Path releaseCache) throws IOException {
        for (URL url : urls) {
            String assetName = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);
            if (!RELEASE_ASSETS.containsValue(assetName)) {
                throw new IOException("FMU is not in test-fmus release 0.0.2: " + assetName);
            }
            String fileName = assetName.substring(assetName.lastIndexOf("--") + 2);
            Path destination = releaseCache.resolve(fileName);
            if (Files.isRegularFile(destination)) {
                continue;
            }
            Files.createDirectories(releaseCache);
            Path temporary = Files.createTempFile(releaseCache, "fmu-", ".tmp");
            try {
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(120000);
                try (InputStream input = connection.getInputStream()) {
                    Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
                }
                if (!containsModelDescription(temporary)) {
                    throw new IOException("Missing modelDescription.xml in " + url);
                }
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
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