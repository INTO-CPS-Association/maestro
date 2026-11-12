package org.intocps.maestro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.intocps.maestro.ast.LexLocation;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.ALoadExp;
import org.intocps.maestro.ast.node.AStringLiteralExp;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.parser.MablParserUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipFile;

public final class TestFmuDownloads {
    static final String ONLINE_MODELS_URL = "https://github.com/INTO-CPS-Association/maestro/releases/download/OnlineTestModels/";
    static final String TEST_FMU_RELEASE_URL = "https://github.com/INTO-CPS-Association/test-fmus/releases/download/Release%2F0.0.2/";
    static final Path TEST_FMU_CACHE = Paths.get("target", "online-cache", "test-fmus-0.0.2");
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

    private TestFmuDownloads() {
    }

    public static void download(List<URL> urls) throws IOException {
        download(urls, TEST_FMU_CACHE);
    }

    public static void download(List<URL> urls, Path releaseCache) throws IOException {
        for (URL url : urls) {
            String assetName = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);
            boolean releaseAsset = RELEASE_ASSETS.containsValue(assetName);
            String fileName = releaseAsset ? assetName.substring(assetName.lastIndexOf("--") + 2) : assetName;
            Path directory = releaseAsset ? releaseCache : Paths.get("target", "online-cache");
            Path destination = directory.resolve(fileName);
            if (Files.isRegularFile(destination)) {
                continue;
            }
            Files.createDirectories(directory);
            Path temporary = Files.createTempFile(directory, "fmu-", ".tmp");
            try {
                URL downloadUrl = releaseAsset ? url : URI.create(url.toString().replace(".fmu", ".zip")).toURL();
                FileUtils.copyURLToFile(downloadUrl, temporary.toFile(), 30000, 120000);
                try (ZipFile zip = new ZipFile(temporary.toFile())) {
                    if (zip.getEntry("modelDescription.xml") == null) {
                        throw new IOException("Missing modelDescription.xml in " + downloadUrl);
                    }
                }
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }

    public static void downloadJniFmuTestFmus() throws IOException {
        download(List.of(fmuUrl("fmi2functiontest.fmu")));
    }

    public static URL fmuUrl(String fileName) throws MalformedURLException {
        String asset = RELEASE_ASSETS.get(fileName);
        return URI.create(asset == null ? ONLINE_MODELS_URL + fileName : TEST_FMU_RELEASE_URL + asset).toURL();
    }

    public static Path cachePath(String fileName) {
        return RELEASE_ASSETS.containsKey(fileName) ? TEST_FMU_CACHE.resolve(fileName)
                : Paths.get("target", "online-cache", fileName);
    }

    public static boolean isReleaseAsset(String fileName) {
        return RELEASE_ASSETS.containsKey(fileName);
    }

    public static void downloadFromDirectory(File directory) throws IOException, AnalysisException {
        List<URL> urls = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        try (var files = Files.walk(directory.toPath())) {
            for (Path path : files.filter(Files::isRegularFile).toList()) {
                String name = path.getFileName().toString().toLowerCase();
                if (name.endsWith(".mabl")) {
                    for (INode document : MablParserUtil.parse(List.of(path.toFile()))) {
                        urls.addAll(collectFmus(document, false));
                    }
                } else if (name.endsWith(".json")) {
                    collectJsonFmus(mapper.readTree(path.toFile()), urls);
                }
            }
        }
        download(urls);
    }

    private static void collectJsonFmus(JsonNode node, List<URL> urls) throws MalformedURLException {
        if (node.isTextual()) {
            String value = node.asText();
            if (value.startsWith("file:")) {
                value = value.substring("file:".length());
            }
            String fileName = value.substring(value.lastIndexOf('/') + 1);
            if (isReleaseAsset(fileName)) {
                urls.add(fmuUrl(fileName));
            }
        } else if (node.isContainerNode()) {
            for (JsonNode child : node) {
                collectJsonFmus(child, urls);
            }
        }
    }

    public static List<URL> collectFmus(INode spec, boolean updatePath) throws AnalysisException {
        if (spec == null) {
            return List.of();
        }
        class FmuCollector extends DepthFirstAnalysisAdaptor {
            final List<URL> fmus = new Vector<>();

            @Override
            public void caseALoadExp(ALoadExp node) throws AnalysisException {
                if (node.getArgs() == null || node.getArgs().size() != 3 || !(node.getArgs().get(0) instanceof AStringLiteralExp) ||
                        !(node.getArgs().get(2) instanceof AStringLiteralExp)) {
                    return;
                }
                String type = ((AStringLiteralExp) node.getArgs().get(0)).getValue();
                if (!type.equals("FMI2") && !type.equals("FMI3")) {
                    return;
                }
                String path = ((AStringLiteralExp) node.getArgs().get(2)).getValue();
                boolean fileUri = path.startsWith("file:");
                if (fileUri) {
                    path = path.substring("file:".length());
                }
                String fileName = path.substring(path.lastIndexOf('/') + 1);
                if (!fileName.endsWith(".fmu") || (!isReleaseAsset(fileName) && (fileUri || path.contains("src/test/resources")))) {
                    return;
                }
                try {
                    fmus.add(fmuUrl(fileName));
                    if (updatePath && isReleaseAsset(fileName)) {
                        List<PExp> args = new Vector<>(node.getArgs());
                        args.set(2, new AStringLiteralExp(new LexLocation("unknown", 0, 0), cachePath(fileName).toString().replace('\\', '/')));
                        node.setArgs(args);
                    }
                } catch (MalformedURLException exception) {
                    throw new AnalysisException(exception);
                }
            }
        }
        FmuCollector collector = new FmuCollector();
        spec.apply(collector);
        return collector.fmus;
    }
}