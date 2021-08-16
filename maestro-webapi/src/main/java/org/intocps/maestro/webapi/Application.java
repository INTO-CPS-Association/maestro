package org.intocps.maestro.webapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.CommandLine;
import org.intocps.maestro.MaestroV1CliProxy;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.webapi.maestro2.Maestro2Broker;
import org.intocps.maestro.webapi.maestro2.dto.InitializationData;
import org.intocps.maestro.webapi.maestro2.dto.SimulateRequestBody;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.EventListener;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;

@SpringBootApplication
public class Application {

    final static ObjectMapper mapper = new ObjectMapper();
    private static boolean serverAcquiresPort = false;

    @EventListener
    public void onApplicationEvent(final ServletWebServerInitializedEvent event) {
        if(serverAcquiresPort){
            int port = event.getWebServer().getPort();
            System.out.println("Server acquired port: {" + port + "}");
        }
    }

    public static void main(String[] args) throws Exception {

        if (args.length > 0 && args[0].equals("cliMain")) {
            org.intocps.maestro.Main.argumentHandler(Arrays.stream(args).skip(1).toArray(String[]::new));
        } else {
            CommandLine cmd = MaestroV1CliProxy.parse(args);
            if (!MaestroV1CliProxy.process(cmd, new MableV1ToV2ProxyRunner(), port -> {
                serverAcquiresPort = port == 0;
                SpringApplication app = new SpringApplication(Application.class);
                app.run("--server.port=" + port);
            })) {
                System.exit(1);
            }
        }

    }

    static class MableV1ToV2ProxyRunner implements MaestroV1CliProxy.OneShotRunner {

        @Override
        public boolean run(boolean verbose, File configFile, File simulationConfigFile, Double startTime, Double endTime,
                File outputFile) throws IOException {
            InitializationData initializationData = mapper.readValue(configFile, InitializationData.class);

            SimulateRequestBody simulationData = null;
            if (simulationConfigFile != null && simulationConfigFile.exists()) {
                simulationData = mapper.readValue(simulationConfigFile, SimulateRequestBody.class);
            } else {
                simulationData = new SimulateRequestBody(startTime, endTime, new HashMap<>(), false, 0d);
            }

            Function<File, File> calculateWorkingDirectory = (file) -> {
                if (file == null) {
                    return new File(".");
                } else if (file.isDirectory()) {
                    return file;
                } else {
                    return file.getParentFile();
                }
            };

            File workingDirectory = calculateWorkingDirectory.apply(outputFile);
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
