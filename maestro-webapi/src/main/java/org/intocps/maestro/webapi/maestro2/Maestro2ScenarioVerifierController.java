package org.intocps.maestro.webapi.maestro2;

import core.MasterModel;
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
import synthesizer.LoopStrategy;

@RestController
@Component
public class Maestro2ScenarioVerifierController {

    @RequestMapping(value = "/generateAlgorithmFromScenario", method = RequestMethod.POST, consumes = {"application/json"})
    public MasterModelDTO generateAlgorithmFromScenario(@RequestBody ScenarioDTO scenario) {

        ScenarioModel scenarioModel = ScenarioModelMapper.Companion.scenarioDTOToScenarioModel(scenario);

        MasterModel masterModel = ScenarioModelMapper.Companion.scenarioModelToMasterModel(scenarioModel, LoopStrategy.maximum());

        return new MasterModelDTO(null, null, CollectionConverters.asJava(masterModel.initialization().map(Object::toString)),
                CollectionConverters.asJava(masterModel.cosimStep().map(Object::toString)),null);

    }

    @RequestMapping(value = "/generateAlgorithmFromMultiModel", method = RequestMethod.POST, consumes = {"application/json"})
    public MasterModelDTO generateAlgorithmFromMultiModel(@RequestBody MultiModelScenarioVerifier multiModel) {

        ScenarioModel scenarioModel = ScenarioModelMapper.Companion.multiModelToScenarioModel(multiModel, 3);

        MasterModel masterModel = ScenarioModelMapper.Companion.scenarioModelToMasterModel(scenarioModel, LoopStrategy.maximum());

        return new MasterModelDTO(null, null, CollectionConverters.asJava(masterModel.initialization().map(Object::toString)),
                CollectionConverters.asJava(masterModel.cosimStep().map(Object::toString)), null);

    }

    @RequestMapping(value = "/executeAlgorithm", method = RequestMethod.POST, consumes = {"application/json"})
    public String executeAlgorithm(@RequestBody MasterModelWithMultiModelDTO mm) {

        return null;
    }

}
