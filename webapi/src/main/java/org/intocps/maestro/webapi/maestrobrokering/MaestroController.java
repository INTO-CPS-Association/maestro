package org.intocps.maestro.webapi.maestrobrokering;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.intocps.maestro.MableSpecificationGenerator;
import org.intocps.maestro.ast.ARootDocument;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.webapi.controllers.MaestroSimulationController;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MaestroController {

    public String CreateMablSpecFromLegacyMM(MaestroSimulationController.InitializationData initializationData,
            MaestroSimulationController.SimulateRequestBody simulateRequestBody) throws IOException, URISyntaxException {
        ObjectMapper mapper = new ObjectMapper();
        //Create the parser configuration
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        ParserConfiguration.ParserPluginConfiguration initializerConfig = InitializerUsingCoeConfigCreator
                .createInitializationJsonNode(initializationData, simulateRequestBody);
        parserConfiguration.addParserPluginConfiguration(initializerConfig);
        InputStream context = new ByteArrayInputStream(mapper.writeValueAsBytes(parserConfiguration));

        // Load the initial spec
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
