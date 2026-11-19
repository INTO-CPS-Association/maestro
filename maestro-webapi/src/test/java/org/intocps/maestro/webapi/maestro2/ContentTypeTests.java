package org.intocps.maestro.webapi.maestro2;

import org.intocps.maestro.webapi.maestro2.dto.StatusModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("main")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ContentTypeTests {
    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    public void jsonResponsesKeepTheirBodyShapes() throws Exception {
        StatusModel session = new Maestro2SimulationController().createSession();
        try {
            mvc.perform(get("/status/" + session.sessionId).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.sessionId", is(session.sessionId)));
            mvc.perform(get("/status").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[?(@.sessionId == '" + session.sessionId + "')]").exists());
            mvc.perform(get("/version").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.version", is(Maestro2SimulationController.version)));
            mvc.perform(get("/ping")).andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)).andExpect(content().string("OK"));
        } finally {
            Maestro2SimulationController.sessionController.removeSession(session.sessionId);
        }
    }

    @Test
    public void csvDownloadUsesDeclaredContentType() throws Exception {
        StatusModel session = new Maestro2SimulationController().createSession();
        var root = Maestro2SimulationController.sessionController.getSessionLogic(session.sessionId).rootDirectory.toPath();
        Files.createDirectories(root);
        try {
            Files.writeString(root.resolve("outputs.csv"), "time,value\n0,1\n", StandardCharsets.UTF_8);
            mvc.perform(get("/result/" + session.sessionId + "/plain").accept("text/csv"))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv"))
                    .andExpect(content().string("time,value\n0,1\n"));
        } finally {
            Files.deleteIfExists(root.resolve("outputs.csv"));
            Files.deleteIfExists(root);
            Maestro2SimulationController.sessionController.removeSession(session.sessionId);
        }
    }

    @Test
    public void initializeStillAcceptsJsonAndLegacyText() throws Exception {
        for (MediaType type : new MediaType[]{MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN}) {
            StatusModel session = new Maestro2SimulationController().createSession();
            var root = Maestro2SimulationController.sessionController.getSessionLogic(session.sessionId).rootDirectory.toPath();
            Files.createDirectories(root);
            try {
                mvc.perform(post("/initialize/" + session.sessionId).contentType(type)
                                .content("{\"fmus\":{},\"connections\":{}}"))
                        .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.sessionId", is(session.sessionId)));
            } finally {
                Files.deleteIfExists(root.resolve("initialize.json"));
                Files.deleteIfExists(root);
                Maestro2SimulationController.sessionController.removeSession(session.sessionId);
            }
        }
    }

    @Test
    public void openApiDescribesJsonModelsRatherThanStrings() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/status/{sessionId}'].get.responses['200'].content['application/json'].schema['$ref']",
                        is("#/components/schemas/StatusModel")))
                .andExpect(jsonPath("$.paths['/status'].get.responses['200'].content['application/json'].schema.type", is("array")))
                .andExpect(jsonPath("$.paths['/status'].get.responses['200'].content['application/json'].schema.items['$ref']",
                        is("#/components/schemas/StatusModel")))
                .andExpect(jsonPath("$.paths['/version'].get.responses['200'].content['application/json'].schema['$ref']",
                        is("#/components/schemas/VersionModel")))
                .andExpect(jsonPath("$.components.schemas.VersionModel.properties.version.type", is("string")))
                .andExpect(jsonPath("$.paths['/initialize/{sessionId}'].post.requestBody.content['application/json'].schema['$ref']",
                        is("#/components/schemas/InitializationData")))
                .andExpect(jsonPath("$.paths['/simulate/{sessionId}'].post.requestBody.content['application/json'].schema['$ref']",
                        is("#/components/schemas/SimulateRequestBody")));
    }
}
