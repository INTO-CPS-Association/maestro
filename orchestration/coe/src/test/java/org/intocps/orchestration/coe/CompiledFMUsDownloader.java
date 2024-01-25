package org.intocps.orchestration.coe;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class CompiledFMUsDownloader {
    private static String repo = " https://github.com/INTO-CPS-Association/compiled-fmus/raw/maestro/";
    public static String destinationDirectory = "target/compiled-fmus/";
    public static List<String> FMUs = Arrays.asList(
            "singleWaterTankController.fmu",
            "singlewatertank-20sim.fmu");

    public static void DownloadDefaultCompiledFMUs() throws IOException {
        for (String fmu : FMUs) {
            File destination = new File(destinationDirectory + fmu);
            URL url = new URL(repo + fmu);
            if (destination.exists() == false) {
                System.out.println("Downloading: " + url + " as: "
                        + destination);
                org.apache.commons.io.FileUtils.copyURLToFile(url, destination);
            } else {
                System.out.println("Skipped - Downloading: " + fmu + " as: "
                        + destination);
            }
        }
    }
}
