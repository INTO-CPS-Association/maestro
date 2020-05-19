package org.intocps.maestro.webapi.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.intocps.maestro.webapi.services.SimulatorManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/esav1/orchestrator")
public class SimulatorManagementController {
    private final static Logger logger = LoggerFactory.getLogger(SimulatorManagementController.class);

    final SimulatorManagementService simulatorManagementService;

    @Autowired
    public SimulatorManagementController(SimulatorManagementService simulatorManagementService) {
        this.simulatorManagementService = simulatorManagementService;
    }

    @RequestMapping(value = "/ping")
    public String ping() {
        return "OK";
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public CreateResponse create() throws Exception {

        String simulatorId = UUID.randomUUID().toString();
        logger.debug("Creating simulator: {}", simulatorId);

        String url = simulatorManagementService.create(simulatorId);

        return new CreateResponse(simulatorId, url, simulatorManagementService.getSimulatorDirectory(simulatorId).getAbsolutePath());
    }


    @RequestMapping(value = "/{simulatorId}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String simulatorId) throws IOException, SimulatorNotFoundException {
        logger.debug("Deleting simulator: {}", simulatorId);

        if (!this.simulatorManagementService.delete(simulatorId)) {
            throw new SimulatorNotFoundException("Unknown simulator id: " + simulatorId);
        }
    }

    @RequestMapping(value = "/terminate", method = RequestMethod.POST)
    public void terminate() throws Exception {
        logger.info("System terminating. NOW");
        this.simulatorManagementService.terminateApplication();

    }


    public class CreateResponse {
        @JsonProperty("instance_id")
        public final String instanceId;
        @JsonProperty("instance_url")
        public final String instanceUrl;
        @JsonProperty("working_directory")
        public final String workingDirectory;

        public CreateResponse(String instanceId, String instanceUrl, String workingDirectory) {
            this.instanceId = instanceId;
            this.instanceUrl = instanceUrl;
            this.workingDirectory = workingDirectory;
        }

    }
}