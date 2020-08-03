package org.intocps.orchestration.coe.Api;

import fi.iki.elonen.NanoHTTPD;
import junit.framework.Assert;
import org.intocps.orchestration.coe.CoeMain;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.intocps.orchestration.coe.CompiledFMUsDownloader.DownloadDefaultCompiledFMUs;

public class MainTest {

    final static String resourcesConfigPrefix = "src/test/resources/api/maintest/";
    String configPath = resourcesConfigPrefix + "config.json";
    String simulationConfigPath = resourcesConfigPrefix + "simulation-config.json";

    @BeforeClass
    public static void before() throws IOException {
        DownloadDefaultCompiledFMUs();
    }

    @Test
    public void OneShotStartEndTimeDefined() throws InterruptedException, IOException, NanoHTTPD.ResponseException {
        Path outFile = Files.createTempFile(null, null);
        double startTime = 0;
        double endTime = 10;
        String arguments = String.format("-o -c %s -s %f -e %f -r %s", configPath, startTime, endTime, outFile.toString());
        String[] s = arguments.split(" ");
        boolean result = CoeMain.CMDHandler(s);
        org.junit.Assert.assertTrue(result);
    }

    @Test
    public void OneShotSimulationConfigDefined() throws InterruptedException, IOException, NanoHTTPD.ResponseException {
        Path outFile = Files.createTempFile(null, null);
        String arguments = String.format("-o -c %s -sc %s -r %s", configPath, simulationConfigPath, outFile.toString());
        String[] s = arguments.split(" ");
        boolean result = CoeMain.CMDHandler(s);
        org.junit.Assert.assertTrue(result);
    }

    @Test
    public void OneShotSimulationConfigError() throws InterruptedException, IOException, NanoHTTPD.ResponseException {
        String configPath = resourcesConfigPrefix + "configError.json";
        Path outFile = Files.createTempFile(null, null);
        String arguments = String.format("-o -c %s -sc %s -r %s", configPath, simulationConfigPath, outFile.toString());
        String[] s = arguments.split(" ");
        boolean result = CoeMain.CMDHandler(s);
        org.junit.Assert.assertFalse(result);
    }
}
