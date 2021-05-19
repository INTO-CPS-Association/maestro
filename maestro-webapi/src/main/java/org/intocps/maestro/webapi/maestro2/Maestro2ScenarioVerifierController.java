package org.intocps.maestro.webapi.maestro2;

import core.MasterModel;
import core.ScenarioLoader;
import core.ScenarioModel;
import org.intocps.maestro.webapi.ScenarioModelMapper;
import org.intocps.maestro.webapi.dto.MasterModelDTO;
import org.intocps.maestro.webapi.dto.MasterModelWithMultiModelDTO;
import org.intocps.maestro.webapi.dto.ScenarioDTO;
import org.intocps.maestro.webapi.maestro2.dto.MultiModelScenarioVerifier;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import scala.jdk.javaapi.CollectionConverters;
import synthesizer.ConfParser.ScenarioConfGenerator;
import synthesizer.LoopStrategy;

import java.io.ByteArrayInputStream;

@RestController
@Component
public class Maestro2ScenarioVerifierController {

    @RequestMapping(value = "/generateAlgorithmFromScenario", method = RequestMethod.POST, consumes = {"text/plain"})
    public String generateAlgorithmFromScenario(@RequestBody String scenario) {

        MasterModel masterModel = ScenarioModelMapper.Companion.scenarioToMasterModel(scenario);

        return ScenarioConfGenerator.generate(masterModel, masterModel.name());
    }

    @RequestMapping(value = "/generateAlgorithmFromMultiModel", method = RequestMethod.POST, consumes = {"application/json"})
    public String generateAlgorithmFromMultiModel(@RequestBody MultiModelScenarioVerifier multiModel) {

        MasterModel masterModel = ScenarioModelMapper.Companion.multiModelToMasterModel(multiModel, 3);

        return ScenarioConfGenerator.generate(masterModel, masterModel.name());
    }

    @RequestMapping(value = "/executeAlgorithm", method = RequestMethod.POST, consumes = {"application/json"})
    public String executeAlgorithm(@RequestBody MasterModelWithMultiModelDTO mm) {

        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(mm.getMasterModel().getBytes()));
        MultiModelScenarioVerifier mms = mm.getMultiModel();

        return null;
    }

}
