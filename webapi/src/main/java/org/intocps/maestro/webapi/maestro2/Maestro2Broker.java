package org.intocps.maestro.webapi.maestro2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ErrorReporter;
import org.intocps.maestro.Mabl;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.api.FixedStepSizeAlgorithm;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.template.MaBLTemplateConfiguration;
import org.intocps.maestro.webapi.maestro2.interpreter.WebApiInterpreterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Maestro2Broker {
    private final static Logger logger = LoggerFactory.getLogger(Maestro2Broker.class);
    final Mabl mabl;
    final File workingDirectory;
    final ErrorReporter reporter;

    public Maestro2Broker(File workingDirectory, ErrorReporter reporter) {
        this.workingDirectory = workingDirectory;
        this.mabl = new Mabl(workingDirectory, null);
        this.reporter = reporter;

        mabl.setReporter(this.reporter);
        mabl.getSettings().dumpIntermediateSpecs = false;
        mabl.getSettings().inlineFrameworkConfig = true;
    }

    public void buildAndRun(Maestro2SimulationController.InitializationData initializeRequest, Maestro2SimulationController.SimulateRequestBody body,
            WebSocketSession socket, File csvOutputFile) throws Exception {

        Map<String, Object> initialize = new HashMap<>();
        initialize.put("parameters", initializeRequest.parameters);

        Fmi2SimulationEnvironmentConfiguration simulationConfiguration = new Fmi2SimulationEnvironmentConfiguration();
        simulationConfiguration.fmus = initializeRequest.getFmus();
        simulationConfiguration.connections = initializeRequest.getConnections();
        simulationConfiguration.logVariables = initializeRequest.getLogVariables();
        simulationConfiguration.livestream = initializeRequest.livestream;

        Fmi2SimulationEnvironment simulationEnvironment = Fmi2SimulationEnvironment.of(simulationConfiguration, this.reporter);

        // Loglevels from app consists of {key}.instance: [loglevel1, loglevel2,...] but have to be: instance: [loglevel1, loglevel2,...].
        Map<String, List<String>> removedFMUKeyFromLogLevels = body.logLevels.entrySet().stream().collect(Collectors
                .toMap(entry -> MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder.getFmuInstanceFromFmuKeyInstance(entry.getKey()),
                        Map.Entry::getValue));

        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder builder =
                MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder.getBuilder().setFrameworkConfig(Framework.FMI2, simulationConfiguration)
                        .useInitializer(true, new ObjectMapper().writeValueAsString(initialize)).setFramework(Framework.FMI2)
                        .setLogLevels(removedFMUKeyFromLogLevels).setVisible(initializeRequest.visible).setLoggingOn(initializeRequest.loggingOn).
                        setStepAlgorithm(new FixedStepSizeAlgorithm(body.endTime,
                                ((Maestro2SimulationController.FixedStepAlgorithmConfig) initializeRequest.getAlgorithm()).getSize()));


        MaBLTemplateConfiguration configuration = builder.build();
        generateSpecification(configuration);

        Function<Map<String, List<String>>, List<String>> flattenFmuIds =
                map -> map.entrySet().stream().flatMap(entry -> entry.getValue().stream().map(v -> entry.getKey() + "." + v))
                        .collect(Collectors.toList());


        List<String> connectedOutputs = simulationEnvironment.getConnectedOutputs().stream().map(x -> {
            ComponentInfo i = simulationEnvironment.getUnitInfo(new LexIdentifier(x.instance.getText(), null), Framework.FMI2);
            return String.format("%s.%s.%s", i.fmuIdentifier, x.instance.getText(), x.scalarVariable.getName());
        }).collect(Collectors.toList());


        executeInterpreter(socket, Stream.concat(connectedOutputs.stream(),
                (initializeRequest.logVariables == null ? new Vector<String>() : flattenFmuIds.apply(initializeRequest.logVariables)).stream())
                        .collect(Collectors.toList()),
                initializeRequest.livestream == null ? new Vector<>() : flattenFmuIds.apply(initializeRequest.livestream), body.liveLogInterval,
                csvOutputFile);

    }

    public void generateSpecification(MaBLTemplateConfiguration config) throws Exception {
        mabl.generateSpec(config);
        mabl.expand();
        mabl.dump(workingDirectory);
        //logger.debug(PrettyPrinter.printLineNumbers(mabl.getMainSimulationUnit()));
    }

    public void executeInterpreter(WebSocketSession webSocket, List<String> csvFilter, List<String> webSocketFilter, double interval,
            File csvOutputFile) throws IOException, AnalysisException {
        WebApiInterpreterFactory factory;
        if (webSocket != null) {
            factory = new WebApiInterpreterFactory(workingDirectory, webSocket, interval, webSocketFilter, new File(workingDirectory, "outputs.csv"),
                    csvFilter);
        } else {
            factory = new WebApiInterpreterFactory(workingDirectory, csvOutputFile, csvFilter);
        }
        new MableInterpreter(factory).execute(mabl.getMainSimulationUnit());
    }

    public void setVerbose(boolean verbose) {
        mabl.setVerbose(verbose);
    }
}
