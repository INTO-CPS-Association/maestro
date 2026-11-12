package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.parser.MablParserUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class OnlineTestUtilsTest {
    @TempDir
    Path directory;

    @Test
    void mapsOnlyKnownModelsToPinnedRelease() throws Exception {
        assertEquals(TestFmuDownloads.TEST_FMU_RELEASE_URL + "fmi2--maestro--rollback-end.fmu",
            TestFmuDownloads.fmuUrl("rollback-end.fmu").toExternalForm());
        assertEquals(TestFmuDownloads.TEST_FMU_RELEASE_URL + "fmi2--jni--fmi2functiontest.fmu",
            TestFmuDownloads.fmuUrl("fmi2functiontest.fmu").toExternalForm());
        assertEquals(TestFmuDownloads.TEST_FMU_RELEASE_URL + "fmi3--reference--BouncingBall.fmu",
            TestFmuDownloads.fmuUrl("BouncingBall.fmu").toExternalForm());
        assertEquals(TestFmuDownloads.TEST_FMU_RELEASE_URL + "fmi3--jni--fmi3functiontest.fmu",
            TestFmuDownloads.fmuUrl("fmi3functiontest.fmu").toExternalForm());
        assertEquals(TestFmuDownloads.TEST_FMU_RELEASE_URL + "fmi2--three-tank--WaterTankController_Standalone.fmu",
                TestFmuDownloads.fmuUrl("WaterTankController_Standalone.fmu").toExternalForm());
        assertThrows(IllegalArgumentException.class, () -> TestFmuDownloads.fmuUrl("other.fmu"));
        assertThrows(IllegalArgumentException.class, () -> TestFmuDownloads.cachePath("other.fmu"));
    }

    @Test
    void threeTankControllerUsesDownloadedReleaseFmu() throws Exception {
        String cachePath = TestFmuDownloads.TEST_FMU_CACHE.resolve("WaterTankController_Standalone.fmu").toString().replace('\\', '/');
        Path directory = Path.of("src/test/resources/specifications/full/initialize_threetanks");
        var environment = new ObjectMapper().readTree(directory.resolve("env.json").toFile());
        assertEquals(cachePath, environment.path("fmus").path("{controller}").asText());

        var document = MablParserUtil.parse(List.of(directory.resolve("three_wt.mabl").toFile())).get(0);
        var urls = TestFmuDownloads.collectFmus(document, false);
        var controllerUrl = TestFmuDownloads.fmuUrl("WaterTankController_Standalone.fmu");
        assertTrue(urls.contains(controllerUrl));
        TestFmuDownloads.collectFmus(document, true);
        assertTrue(PrettyPrinter.print(document).contains(cachePath));
    }

    @Test
    void rewritesOnlineLoadsToTheVersionedCache() throws Exception {
        var documents = MablParserUtil.parse(List.of(Path.of("src/test/resources/specifications/online/rollback/fixedstep_rolback.mabl").toFile()));
        var document = documents.get(0);
        assertEquals(2, TestFmuDownloads.collectFmus(document, false).size());
        TestFmuDownloads.collectFmus(document, true);
        String program = PrettyPrinter.print(document);
        assertTrue(program.contains("target/online-cache/test-fmus-0.0.2/rollback-end.fmu"));
        assertTrue(program.contains("target/online-cache/test-fmus-0.0.2/rollback-test.fmu"));
    }

    @Test
    void jacobianConfigurationsUseDownloadedReleaseFmus() throws Exception {
        Path root = Path.of("src/test/resources/jacobian_step_builder");
        ObjectMapper mapper = new ObjectMapper();
        try (var files = Files.walk(root)) {
            var environments = files.filter(path -> path.getFileName().toString().equals("env.json")).toList();
            assertEquals(8, environments.size());
            for (Path environment : environments) {
                try (var siblings = Files.list(environment.getParent())) {
                    var mablFiles = siblings.filter(path -> path.toString().endsWith(".mabl")).map(Path::toFile).toList();
                    String program = PrettyPrinter.print(MablParserUtil.parse(mablFiles).get(0));
                    var fmus = mapper.readTree(environment.toFile()).path("fmus");
                    assertTrue(fmus.isObject());
                    for (var entry : fmus.properties()) {
                        String path = entry.getValue().asText();
                        String fileName = Path.of(path).getFileName().toString();
                        assertEquals(TestFmuDownloads.TEST_FMU_CACHE.resolve(fileName).toString().replace('\\', '/'), path);
                        assertTrue(JacobianStepBuilderTest.FMU_NAMES.contains(fileName));
                        assertTrue(TestFmuDownloads.fmuUrl(fileName).toExternalForm().startsWith(TestFmuDownloads.TEST_FMU_RELEASE_URL));
                        assertTrue(program.contains(path));
                    }
                }
            }
        }
    }

    @Test
    void downloadsAndReusesReleaseAsset() throws Exception {
        Path asset = directory.resolve("fmi2--maestro--rollback-end.fmu");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(asset))) {
            zip.putNextEntry(new ZipEntry("modelDescription.xml"));
            zip.write("<fmiModelDescription/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        Path cache = directory.resolve("cache");
        TestFmuDownloads.download(List.of(asset.toUri().toURL()), cache);
        Path downloaded = cache.resolve("rollback-end.fmu");
        assertArrayEquals(Files.readAllBytes(asset), Files.readAllBytes(downloaded));
        Files.delete(asset);
        TestFmuDownloads.download(List.of(asset.toUri().toURL()), cache);
        assertTrue(Files.isRegularFile(downloaded));
    }

    @Test
    void rejectsInvalidDownloadWithoutLeavingCachedFile() throws Exception {
        Path asset = directory.resolve("fmi2--maestro--rollback-end.fmu");
        Files.writeString(asset, "not an FMU");
        Path cache = directory.resolve("cache");
        assertThrows(IOException.class, () -> TestFmuDownloads.download(List.of(asset.toUri().toURL()), cache));
        try (var files = Files.list(cache)) {
            assertEquals(0, files.count());
        }
    }
}