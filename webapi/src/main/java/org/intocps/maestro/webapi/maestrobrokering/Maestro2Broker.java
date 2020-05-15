package org.intocps.maestro.webapi.maestrobrokering;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.intocps.maestro.MableSpecificationGenerator;
import org.intocps.maestro.ast.ARootDocument;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.plugin.env.EnvironmentMessage;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.maestro.webapi.controllers.Maestro2SimulationController;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Maestro2Broker {

    public String CreateMablSpecFromLegacyMM(Maestro2SimulationController.InitializationData initializationData,
            Maestro2SimulationController.SimulateRequestBody simulateRequestBody) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Create the configuration for the initializer plugin
        ParserConfiguration.ParserPluginConfiguration initializerConfig =
                InitializerUsingCoeConfigCreator.createInitializationJsonNode(initializationData, simulateRequestBody);

        // Create the context for the MaBL parser
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.addParserPluginConfiguration(initializerConfig);
        InputStream context = new ByteArrayInputStream(mapper.writeValueAsBytes(parserConfiguration));

        // Create the environment for the MaBL parser
        EnvironmentMessage msg = new EnvironmentMessage();
        msg.fmus = initializationData.getFmus();
        msg.connections = initializationData.getConnections();
        UnitRelationship simulationEnvironment = UnitRelationship.of(msg);

        // TODO: Create correct spec.
        String spec = new File(Resources.getResource("fixedstepspec.mabl").toURI()).getAbsolutePath();
        String fmi2 = new File(Resources.getResource("FMI2.mabl").toURI()).getAbsolutePath();

        //Create unfolded mabl spec
        ARootDocument doc = new MableSpecificationGenerator(Framework.FMI2, true, null)
                .generate(Stream.of(fmi2, spec).map(File::new).collect(Collectors.toList()), context);
        return doc.toString();

    }

    public void ExecuteInterpreter() {

    }
}
