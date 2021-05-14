package org.intocps.maestro.webapi.maestro2;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@RestController
@Component
public class Maestro2ScenarioVerifierController {

    @RequestMapping(value = "/GenAlgorithm", method = RequestMethod.POST, consumes = {"text/plain"})
    public String generateAlgorithmFromScenario(@RequestBody String scenario){

        return null;
    }

    @RequestMapping(value = "/GenAlgorithm", method = RequestMethod.POST, consumes = {"application/json"})
    public String generateAlgorithmFromMultiModel(@RequestBody String mm){

        return null;
    }
}
