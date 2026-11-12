package org.intocps.maestro;

import org.apache.commons.io.FileUtils;
import org.intocps.maestro.ast.LexLocation;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.ALoadExp;
import org.intocps.maestro.ast.node.AStringLiteralExp;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PExp;

import java.io.IOException;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipFile;

public class OnlineTestUtils {

    public final static String baseDownloadUrl = "https://github.com/INTO-CPS-Association/maestro/releases/download/OnlineTestModels/";
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
            Map.entry("BouncingBall.fmu", "fmi3--reference--BouncingBall.fmu"),
            Map.entry("Feedthrough.fmu", "fmi3--reference--Feedthrough.fmu"));
    final static String prefix = "/online-models";

    public static void download(List<URL> urls) throws IOException {
        download(urls, TEST_FMU_CACHE);
    }

    public static void download(List<URL> urls, Path releaseCache) throws IOException {
        System.out.println("Downloading FMUs");
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
                System.out.println("Downloading: " + downloadUrl + " as: " + destination);
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
        return URI.create(asset == null ? baseDownloadUrl + fileName : TEST_FMU_RELEASE_URL + asset).toURL();
    }

    public static Path cachePath(String fileName) {
        return RELEASE_ASSETS.containsKey(fileName) ? TEST_FMU_CACHE.resolve(fileName)
                : Paths.get("target", "online-cache", fileName);
    }


    public static List<URL> collectFmus(INode spec, boolean updatePath) throws AnalysisException {
        class FmuCollector extends DepthFirstAnalysisAdaptor {
            final List<URL> fmus = new Vector<>();

            @Override
            public void caseALoadExp(ALoadExp node) throws AnalysisException {
                if (node.getArgs() != null && node.getArgs().size() == 3 && node.getArgs().get(0) instanceof AStringLiteralExp &&
                        node.getArgs().get(2) instanceof AStringLiteralExp && ((AStringLiteralExp) node.getArgs().get(0)).getValue().equals("FMI2")) {
                    String fmuPath = ((AStringLiteralExp) node.getArgs().get(2)).getValue();
                    String fmuName = fmuPath.substring(fmuPath.lastIndexOf('/') + 1);
                    if (fmuName.endsWith(".fmu")) {
                        try {
                            fmus.add(fmuUrl(fmuName));
                            if (updatePath) {
                                List<PExp> newArgs = new Vector<>();
                                newArgs.addAll(node.getArgs());
                                newArgs.set(2, new AStringLiteralExp(new LexLocation("unknown",0,0),cachePath(fmuName).toString().replace('\\', '/')));
                                node.setArgs(newArgs);
                            }
                        } catch (MalformedURLException e) {
                            throw new AnalysisException(e);
                        }
                    } else {
                        System.err.println("Invalid fmu name: " + fmuName);
                    }
                }
            }
        }

        FmuCollector collector = new FmuCollector();
        spec.apply(collector);
        return collector.fmus;
    }
}
