import org.apache.commons.io.FileUtils;
import org.intocps.maestro.ast.ALoadExp;
import org.intocps.maestro.ast.AStringLiteralExp;
import org.intocps.maestro.ast.INode;
import org.intocps.maestro.ast.PExp;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Vector;

public abstract class OnlineTestFmusTest {

    public final static String baseDownloadUrl = "https://overture.au.dk/into-cps/examples/public-coe-test-fmus/latest/";
    final static String prefix = "/online-models";

    //    public static void scanConfigsAndDownloadFmus(File rootPath) throws IOException {
    //        List<java.net.URL> urls = new Vector<>();
    //
    //        final ObjectMapper mapper = new ObjectMapper();
    //        File dir = rootPath;
    //        String[] extensions = new String[]{"json"};
    //        List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);
    //        for (File file : files) {
    //            System.out.println("file: " + file.getCanonicalPath());
    //
    //            InitializationMsgJson st = mapper.readValue(FileUtils.readFileToString(file), InitializationMsgJson.class);
    //
    //            for (String fmuPath : st.fmus.values()) {
    //                String fmuName = fmuPath.substring(fmuPath.lastIndexOf('/') + 1);
    //                if (fmuName.endsWith(".fmu")) {
    //                    urls.add(new URL(baseDownloadUrl + fmuName));
    //                } else {
    //                    System.err.println("Invalid fmu name: " + fmuName);
    //                }
    //            }
    //        }
    //
    //        download(urls);
    //
    //    }

    public static void download(List<URL> urls) throws IOException {
        System.out.println("Downloading FMUs");
        for (URL url : urls) {
            String file = url.getFile();
            file = file.substring(file.lastIndexOf('/') + 1);
            File destination = new File("target/online-cache/" + file);
            if (!destination.exists()) {
                System.out.println("Downloading: " + url + " as: " + destination);
                FileUtils.copyURLToFile(url, destination);
            } else {
                System.out.println("Skipped - Downloading: " + url + " as: " + destination);
            }
        }
    }

    //    @BeforeClass
    //    public static void downloadFmus() throws IOException {
    //        scanConfigsAndDownloadFmus(new File("src/test/resources" + prefix));
    //    }

    public List<URL> collectFmus(INode spec, boolean updatePath) throws AnalysisException {
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
                            fmus.add(new URL(baseDownloadUrl + fmuName));
                            if (updatePath) {
                                List<PExp> newArgs = new Vector<>();
                                newArgs.addAll(node.getArgs());
                                newArgs.set(2, new AStringLiteralExp("target/online-cache/" + fmuName));
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

    //    @Override
    //    protected void test(String configPath, double startTime,
    //            double endTime) throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, NanoHTTPD.ResponseException {
    //        super.test(prefix + configPath, startTime, endTime);
    //    }
}
