package org.intocps.maestro.webapi.maestro2;

import core.MasterModel;
import core.ScenarioLoader;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.template.ScenarioConfiguration;
import org.intocps.maestro.webapi.MasterModelMapper;
import org.intocps.maestro.webapi.controllers.SessionController;
import org.intocps.maestro.webapi.dto.MasterAndMultiModelDTO;
import org.intocps.maestro.webapi.maestro2.dto.MultiModelScenarioVerifier;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import synthesizer.ConfParser.ScenarioConfGenerator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

@RestController
@Component
public class Maestro2ScenarioVerifierController {

    @RequestMapping(value = "/generateAlgorithmFromScenario", method = RequestMethod.POST, consumes = {"text/plain"})
    public String generateAlgorithmFromScenario(@RequestBody String scenario) {

        MasterModel masterModel = MasterModelMapper.Companion.scenarioToMasterModel(scenario);

        return ScenarioConfGenerator.generate(masterModel, masterModel.name());
    }

    @RequestMapping(value = "/generateAlgorithmFromMultiModel", method = RequestMethod.POST, consumes = {"application/json"})
    public MasterAndMultiModelDTO generateAlgorithmFromMultiModel(@RequestBody MultiModelScenarioVerifier multiModel) {

        MasterModel masterModel = MasterModelMapper.Companion.multiModelToMasterModel(multiModel, 3);

        return new MasterAndMultiModelDTO(ScenarioConfGenerator.generate(masterModel, masterModel.name()), multiModel);
    }

    @RequestMapping(value = "/executeAlgorithm", method = RequestMethod.POST, consumes = {"application/json"})
    public String executeAlgorithm(@RequestBody MasterAndMultiModelDTO mm) throws Exception {

        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(mm.getMasterModel().getBytes()));
        ErrorReporter reporter = new ErrorReporter();
        Maestro2Broker broker = new Maestro2Broker(createTempDir(), reporter);
        Fmi2SimulationEnvironmentConfiguration simulationConfiguration = new Fmi2SimulationEnvironmentConfiguration();
        simulationConfiguration.fmus = mm.getMultiModel().getFmus();
        simulationConfiguration.connections = mm.getMultiModel().getConnections();

        if (simulationConfiguration.fmus == null) {
            throw new Exception("Missing FMUs from multi-model");
        }

        Fmi2SimulationEnvironment environment = Fmi2SimulationEnvironment.of(simulationConfiguration, reporter);

        broker.generateSpecification(new ScenarioConfiguration(environment, masterModel));

        return null;
    }

    private File createTempDir() {
        int TEMP_DIR_ATTEMPTS = 10000;
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        String baseName = System.currentTimeMillis() + "-";

        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
            File tempDir = new File(baseDir, baseName + counter);
            if (tempDir.mkdir()) {
                return tempDir;
            }
        }
        throw new IllegalStateException(
                "Failed to create directory within " + TEMP_DIR_ATTEMPTS + " attempts (tried " + baseName + "0 to " + baseName +
                        (TEMP_DIR_ATTEMPTS - 1) + ')');
    }

}
