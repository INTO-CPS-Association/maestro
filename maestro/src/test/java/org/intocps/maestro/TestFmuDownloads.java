package org.intocps.maestro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.LexLocation;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.ALoadExp;
import org.intocps.maestro.ast.node.AStringLiteralExp;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.parser.MablParserUtil;
import org.intocps.maestro.testutils.FmuTestModelRepository;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public final class TestFmuDownloads {
    static final String TEST_FMU_RELEASE_URL = FmuTestModelRepository.TEST_FMU_RELEASE_URL;
    static final Path TEST_FMU_CACHE = FmuTestModelRepository.TEST_FMU_CACHE;

    private TestFmuDownloads() {
    }

    public static void download(List<URL> urls) throws IOException {
        FmuTestModelRepository.download(urls);
    }

    public static void download(List<URL> urls, Path releaseCache) throws IOException {
        FmuTestModelRepository.download(urls, releaseCache);
    }

    public static void downloadJniFmuTestFmus() throws IOException {
        download(List.of(fmuUrl("fmi2functiontest.fmu")));
    }

    public static URL fmuUrl(String fileName) throws MalformedURLException {
        return FmuTestModelRepository.url(fileName);
    }

    public static Path cachePath(String fileName) {
        return FmuTestModelRepository.cachePath(fileName);
    }

    public static boolean isReleaseAsset(String fileName) {
        return FmuTestModelRepository.isReleaseAsset(fileName);
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