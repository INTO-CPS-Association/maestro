package org.intocps.maestro.webapi.maestro2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ErrorReporter;
import org.intocps.maestro.ast.ARootDocument;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.EnvironmentMessage;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.plugin.PluginFactory;
import org.intocps.maestro.webapi.maestro2.interpreter.WebApiInterpreterFactory;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Maestro2Broker {
    public ARootDocument createMablSpecFromLegacyMM(Maestro2SimulationController.InitializationData initializationData,
            Maestro2SimulationController.SimulateRequestBody simulateRequestBody, boolean withWs, File rootDirectory,
            Consumer<ISimulationEnvironment> simulationEnvironmentConsumer) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Create the configuration for the initializer plugin
        PluginFactory.PluginConfiguration pluginConfiguration =
                InitializerConfigCreator.createInitializationJsonNode(initializationData, simulateRequestBody);

        // Create the context for the MaBL parser
        List<PluginFactory.PluginConfiguration> contextObj = new ArrayList<>();
        contextObj.add(pluginConfiguration);
        InputStream context = new ByteArrayInputStream(mapper.writeValueAsBytes(contextObj));

        // Create the environment for the MaBL parser
        EnvironmentMessage msg = new EnvironmentMessage();
        msg.fmus = initializationData.getFmus();
        msg.connections = initializationData.getConnections();
        msg.livestream = initializationData.livestream;
        msg.liveLogInterval = simulateRequestBody.liveLogInterval;
        msg.visible = initializationData.visible;
        msg.loggingOn = initializationData.loggingOn;
        IErrorReporter reporter = new ErrorReporter();
        FmiSimulationEnvironment simulationEnvironment = FmiSimulationEnvironment.of(msg, reporter);


        simulationEnvironmentConsumer.accept(simulationEnvironment);

        if (initializationData.getAlgorithm() instanceof Maestro2SimulationController.FixedStepAlgorithmConfig) {
            //            Maestro2SimulationController.FixedStepAlgorithmConfig algorithm =
            //                    (Maestro2SimulationController.FixedStepAlgorithmConfig) initializationData.getAlgorithm();
            //            MaBLTemplateConfiguration templateConfiguration =
            //                    MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder.getBuilder().useInitializer(true)
            //                            .setStepAlgorithm(new FixedStepSizeAlgorithm(simulateRequestBody.endTime, algorithm.getSize()))
            //                            .setUnitRelationship(simulationEnvironment).build();
            //            String mablTemplateSpec = PrettyPrinter.print(MaBLTemplateGenerator.generateTemplate(templateConfiguration));
            //            System.out.println(mablTemplateSpec);


            //            Mabl mabl = new Mabl(directory, null);
            //            mabl.setReporter(reporter);
            //            mabl.setVerbose(true);

            //FIXME enable generation
            //            mabl.generateSpec();
            //mabl.parse(getSpecificationFiles());

            //            mabl.expand();
            //
            //
            //            if (reporter.getErrorCount() > 0) {
            //                reporter.printErrors(new PrintWriter(System.err, true));
            //
            //            }
            //
            //            mabl.dump(workingDirectory);


            //Create unfolded mabl spec
            //            MableSpecificationGenerator mableSpecificationGenerator = new MableSpecificationGenerator(Framework.FMI2, true, simulationEnvironment);
            //            ARootDocument doc = mableSpecificationGenerator.generateFromStreams(Arrays.asList(CharStreams.fromString(mablTemplateSpec)), context);
            return null;
        }

        throw new Exception("Algorithm not supported: " + initializationData.getAlgorithm());

    }

    public void executeInterpreter(ARootDocument doc, WebSocketSession ws, File rootDirectory,
            ISimulationEnvironment environment) throws AnalysisException {

        MableInterpreter interpreter = null;
        if (ws != null) {
            new MableInterpreter(new WebApiInterpreterFactory(ws, new File(rootDirectory, "outputs.csv"))).execute(doc);
        } else {
            //FIXME
            //            new MableInterpreter(new DefaultExternalValueFactory(rootDirectory)).execute(doc);
        }

    }


}
