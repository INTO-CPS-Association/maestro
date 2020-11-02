package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.CommandLine;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class MaestroV1CliTest {
    final static File resourcesConfigPrefix = Paths.get("src", "test", "resources", "cli-test").toFile();
    File configPath = new File(resourcesConfigPrefix, "config.json");
    File simulationConfigPath = new File(resourcesConfigPrefix, "simulation-config.json");

    @Test
    public void oneShotStartEndTimeDefined() throws InterruptedException, IOException {
        Path outFile = Files.createTempFile(null, null);
        double startTimeIn = 0;
        double endTimeIn = 10;
        String arguments =
                String.format(Locale.US, "--oneshot --configuration %s --starttime %f --endtime %f --result %s", configPath.getAbsolutePath(),
                        startTimeIn, endTimeIn, outFile.toString());
        String[] s = arguments.split(" ");

        CommandLine cmd = MaestroV1CliProxy.parse(s);
        MaestroV1CliProxy.process(cmd, (verbose, configFile, simulationConfigFile, startTime, endTime, outputFile) -> {
            Assert.assertNotNull("endtime must be defined", endTime);
            Assert.assertEquals(configPath.getAbsolutePath(), configFile.getAbsolutePath());
            Assert.assertEquals("start time must match expected input", startTimeIn, startTime, 0d);
            Assert.assertEquals("end time must match expected input", endTimeIn, endTime, 0d);
            Assert.assertNull("simulation config must be null", simulationConfigFile);
            return false;
        }, port -> Assert.fail());
    }

    @Test
    public void oneShotSimulationConfigDefined() throws InterruptedException, IOException {
        Path outFile = Files.createTempFile(null, null);

        double startTimeIn = new ObjectMapper().readTree(simulationConfigPath).get("startTime").asDouble();
        double endTimeIn = new ObjectMapper().readTree(simulationConfigPath).get("endTime").asDouble();


        String arguments = String.format("--oneshot --configuration %s --simulationconfiguration %s --result %s", configPath, simulationConfigPath,
                outFile.toString());
        String[] s = arguments.split(" ");
        CommandLine cmd = MaestroV1CliProxy.parse(s);
        MaestroV1CliProxy.process(cmd, (verbose, configFile, simulationConfigFile, startTime, endTime, outputFile) -> {
            Assert.assertNotNull("endtime must be defined", endTime);
            Assert.assertEquals(configPath.getAbsolutePath(), configFile.getAbsolutePath());
            Assert.assertEquals("start time must match expected input", startTimeIn, startTime, 0d);
            Assert.assertEquals("end time must match expected input", endTimeIn, endTime, 0d);
            Assert.assertNotNull("simulation config must be null", simulationConfigFile);
            Assert.assertEquals(simulationConfigPath.getAbsolutePath(), simulationConfigFile.getAbsolutePath());
            return false;
        }, port -> Assert.fail());
    }

    @Test
    public void webApiTest() throws IOException, InterruptedException {
        String arguments = String.format("--port 8888");
        String[] s = arguments.split(" ");
        CommandLine cmd = MaestroV1CliProxy.parse(s);
        MaestroV1CliProxy.process(cmd, (verbose, configFile, simulationConfigFile, startTime, endTime, outputFile) -> {
            Assert.fail();
            return false;
        }, port -> Assert.assertEquals(8888, port));
    }
}
