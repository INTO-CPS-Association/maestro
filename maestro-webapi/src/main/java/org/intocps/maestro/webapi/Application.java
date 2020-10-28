package org.intocps.maestro.webapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.CommandLine;
import org.intocps.maestro.ErrorReporter;
import org.intocps.maestro.MaestroV1CliProxy;
import org.intocps.maestro.webapi.maestro2.Maestro2Broker;
import org.intocps.maestro.webapi.maestro2.Maestro2SimulationController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

@SpringBootApplication
public class Application {

    final static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException, InterruptedException {

        CommandLine cmd = MaestroV1CliProxy.parse(args);

        if (cmd.hasOption(MaestroV1CliProxy.oneShotOpt.getOpt()) || cmd.hasOption(MaestroV1CliProxy.portOpt.getOpt()) ||
                cmd.hasOption(MaestroV1CliProxy.versionOpt.getOpt())) {

            MaestroV1CliProxy.process(cmd, new MableV1ToV2ProxyRunner(), port -> {
                SpringApplication app = new SpringApplication(Application.class);
                app.run("--server.port=" + port);
            });
            return;
        }
        SpringApplication.run(Application.class, args);
    }

    static class MableV1ToV2ProxyRunner implements MaestroV1CliProxy.OneShotRunner {

        @Override
        public boolean run(boolean verbose, File configFile, File simulationConfigFile, Double startTime, Double endTime,
                File outputFile) throws IOException {
            Maestro2SimulationController.InitializationData initializationData =
                    mapper.readValue(configFile, Maestro2SimulationController.InitializationData.class);

            Maestro2SimulationController.SimulateRequestBody simulationData = null;
            if (simulationConfigFile != null && simulationConfigFile.exists()) {
                simulationData = mapper.readValue(configFile, Maestro2SimulationController.SimulateRequestBody.class);
            } else {
                simulationData = new Maestro2SimulationController.SimulateRequestBody(startTime, endTime, new HashMap<>(), false, 0d);
            }

            //use parent to output as working directory, if no output is specified this will default to execution directory
            File workingDirectory = outputFile.getParentFile();
            ErrorReporter reporter = new ErrorReporter();
            Maestro2Broker mc = new Maestro2Broker(workingDirectory, reporter);
            mc.setVerbose(verbose);
            try {
                mc.buildAndRun(initializationData, simulationData, null, outputFile);
                return true;
            } catch (Exception e) {
                if (reporter.getErrorCount() > 0) {
                    reporter.printWarnings(new PrintWriter(System.err, true));
                    reporter.printErrors(new PrintWriter(System.err, true));
                }
                e.printStackTrace();
                return false;
            }
        }
    }
}
