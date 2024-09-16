package org.intocps.maestro;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.ALoadExp;
import org.intocps.maestro.ast.node.AStringLiteralExp;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PExp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class OnlineTestUtils {

    public final static String baseDownloadUrl = "https://github.com/INTO-CPS-Association/maestro/releases/download/OnlineTestModels/";
    final static String prefix = "/online-models";

    public static void download(List<URL> urls) throws IOException {
        System.out.println("Downloading FMUs");
        for (URL url : urls) {
            String file = url.getFile();
            file = file.substring(file.lastIndexOf('/') + 1);
            File destination = new File("target/online-cache/" + file);
            if (!destination.exists() && !destination.getName().endsWith("functiontest.fmu")) {

                URL zipNameUrl = new URL(url.toString().replace(".fmu", ".zip"));

                System.out.println("Downloading: " + zipNameUrl + " as: " + destination);
                FileUtils.copyURLToFile(zipNameUrl, destination);
            } else {
                System.out.println("Skipped - Downloading: " + url + " as: " + destination);
            }
        }
    }

    public static void downloadJniFmuTestFmus() throws IOException {
        System.out.println("Downloading FMUs");
        URL url = new URL("https://github.com/INTO-CPS-Association/org.intocps.maestro.fmi/releases/download/Release%2F1.5.0/test-fmus.zip");
        String file = url.getFile();
        file = file.substring(file.lastIndexOf('/') + 1);
        File destination = new File("target/online-cache/" + file);
        if (!destination.exists()) {

//            URL zipNameUrl = new URL(url.toString().replace(".fmu", ".zip"));

            System.out.println("Downloading: " + url + " as: " + destination);
            FileUtils.copyURLToFile(url, destination);
            //lets unpack the fmus
            if (destination.exists() && destination.isFile()) {
                try (FileInputStream fis = new FileInputStream(destination);
                     ZipInputStream zis = new ZipInputStream(fis)) {
                    ZipEntry entry = zis.getNextEntry();
                    while (entry != null) {
                        if (entry.getName().endsWith("functiontest.fmu")) {
                            IOUtils.copy(zis, new FileOutputStream("target/online-cache/" + new File(entry.getName()).getName()));

                        }
                        zis.closeEntry();
                        entry = zis.getNextEntry();
                    }
                }
            }
        } else {
            System.out.println("Skipped - Downloading: " + url + " as: " + destination);
        }
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
}
