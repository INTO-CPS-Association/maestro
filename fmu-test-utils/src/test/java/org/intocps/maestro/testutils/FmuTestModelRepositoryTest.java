package org.intocps.maestro.testutils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class FmuTestModelRepositoryTest {
    @TempDir
    Path directory;

    @Test
    void mapsOnlyKnownModelsToPinnedRelease() throws Exception {
        assertEquals(FmuTestModelRepository.TEST_FMU_RELEASE_URL + "fmi2--maestro--rollback-end.fmu",
                FmuTestModelRepository.url(TestFmu.ROLLBACK_END).toExternalForm());
        assertEquals(FmuTestModelRepository.TEST_FMU_RELEASE_URL + "fmi2--jni--fmi2functiontest.fmu",
                FmuTestModelRepository.url(TestFmu.FMI2_FUNCTION_TEST).toExternalForm());
        assertEquals(FmuTestModelRepository.TEST_FMU_RELEASE_URL + "fmi3--reference--BouncingBall.fmu",
                FmuTestModelRepository.url(TestFmu.BOUNCING_BALL).toExternalForm());
        assertEquals(FmuTestModelRepository.TEST_FMU_RELEASE_URL + "fmi3--jni--fmi3functiontest.fmu",
                FmuTestModelRepository.url(TestFmu.FMI3_FUNCTION_TEST).toExternalForm());
        assertEquals(FmuTestModelRepository.TEST_FMU_RELEASE_URL + "fmi2--three-tank--WaterTankController_Standalone.fmu",
                FmuTestModelRepository.url(TestFmu.THREE_WATER_TANK_CONTROLLER).toExternalForm());
            assertTrue(TestFmu.fromFileName("other.fmu").isEmpty());
    }

    @Test
    void downloadsAndReusesReleaseAsset() throws Exception {
        Path asset = directory.resolve("fmi2--maestro--rollback-end.fmu");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(asset))) {
            zip.putNextEntry(new ZipEntry("modelDescription.xml"));
            zip.write("<fmiModelDescription/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        Path cache = directory.resolve("cache");
        FmuTestModelRepository.download(TestFmu.ROLLBACK_END, asset.toUri().toURL(), cache);
        Path downloaded = cache.resolve("rollback-end.fmu");
        assertArrayEquals(Files.readAllBytes(asset), Files.readAllBytes(downloaded));
        Files.delete(asset);
        FmuTestModelRepository.download(TestFmu.ROLLBACK_END, asset.toUri().toURL(), cache);
        assertTrue(Files.isRegularFile(downloaded));
    }

    @Test
    void rejectsInvalidDownloadWithoutLeavingCachedFile() throws Exception {
        Path asset = directory.resolve("fmi2--maestro--rollback-end.fmu");
        Files.writeString(asset, "not an FMU");
        Path cache = directory.resolve("cache");
        assertThrows(IOException.class, () -> FmuTestModelRepository.download(TestFmu.ROLLBACK_END, asset.toUri().toURL(), cache));
        try (var files = Files.list(cache)) {
            assertEquals(0, files.filter(path -> !path.getFileName().toString().equals(".release")).count());
        }
    }

    @Test
    void refreshesUnversionedCacheWhenPinnedReleaseChanges() throws Exception {
        Path asset = directory.resolve("fixture.fmu");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(asset))) {
            zip.putNextEntry(new ZipEntry("modelDescription.xml"));
            zip.write("<fmiModelDescription/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        Path cache = Files.createDirectory(directory.resolve("cache"));
        Files.writeString(cache.resolve(".release"), "previous-release");
        Files.writeString(cache.resolve(TestFmu.ROLLBACK_END.fileName()), "stale FMU");
        FmuTestModelRepository.download(TestFmu.ROLLBACK_END, asset.toUri().toURL(), cache);
        assertArrayEquals(Files.readAllBytes(asset), Files.readAllBytes(cache.resolve(TestFmu.ROLLBACK_END.fileName())));
        assertEquals(FmuTestModelRepository.TEST_FMU_RELEASE_URL, Files.readString(cache.resolve(".release")));
        assertEquals(Path.of("target", "online-cache", "test-fmus"), FmuTestModelRepository.TEST_FMU_CACHE);
    }
}