package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.parser.MablParserUtil;
import org.intocps.maestro.testutils.FmuTestModelRepository;
import org.intocps.maestro.testutils.TestFmu;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestFmuCollectorTest {
    @Test
    void threeTankControllerUsesDownloadedReleaseFmu() throws Exception {
        String cachePath = FmuTestModelRepository.cachePath(TestFmu.THREE_WATER_TANK_CONTROLLER).toString().replace('\\', '/');
        Path directory = Path.of("src/test/resources/specifications/full/initialize_threetanks");
        var environment = new ObjectMapper().readTree(directory.resolve("env.json").toFile());
        assertEquals(cachePath, environment.path("fmus").path("{controller}").asText());
        var document = MablParserUtil.parse(List.of(directory.resolve("three_wt.mabl").toFile())).get(0);
        assertTrue(TestFmuCollector.collectFmus(document, false).contains(TestFmu.THREE_WATER_TANK_CONTROLLER));
        TestFmuCollector.collectFmus(document, true);
        assertTrue(PrettyPrinter.print(document).contains(cachePath));
    }

    @Test
    void rewritesOnlineLoadsToStableCache() throws Exception {
        var document = MablParserUtil.parse(List.of(
                Path.of("src/test/resources/specifications/online/rollback/fixedstep_rolback.mabl").toFile())).get(0);
        assertEquals(2, TestFmuCollector.collectFmus(document, false).size());
        TestFmuCollector.collectFmus(document, true);
        String program = PrettyPrinter.print(document);
        assertTrue(program.contains("target/online-cache/test-fmus/rollback-end.fmu"));
        assertTrue(program.contains("target/online-cache/test-fmus/rollback-test.fmu"));
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
                    for (var entry : mapper.readTree(environment.toFile()).path("fmus").properties()) {
                        String path = entry.getValue().asText();
                        TestFmu model = TestFmu.fromFileName(Path.of(path).getFileName().toString()).orElseThrow();
                        assertEquals(FmuTestModelRepository.cachePath(model).toString().replace('\\', '/'), path);
                        assertTrue(program.contains(path));
                    }
                }
            }
        }
    }
}