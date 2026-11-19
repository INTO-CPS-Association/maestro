package org.intocps.maestro.webapi.maestro2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("main")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OpenApiDocumentationTests {
    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    public void publishedReferenceMatchesGeneratedOpenApi() throws Exception {
        ObjectNode api = (ObjectNode) mapper.readTree(mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        // The offline document is independent of the test server's host and random port.
        api.remove("servers");
        Path reference = Path.of("../docs/user/api/openapi.json");
        if (Boolean.getBoolean("maestro.openapi.update")) {
            Files.createDirectories(reference.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(reference.toFile(), api);
        }
        assertEquals(mapper.readTree(reference.toFile()), api,
                "Regenerate the API reference with -Dmaestro.openapi.update=true after intentional API documentation changes.");
    }

    @Test
    public void operationsAndKeyPropertiesHaveDescriptions() throws Exception {
        JsonNode api = mapper.readTree(mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        assertEquals("Maestro Web API", api.at("/info/title").asText());
        api.path("paths").fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(method -> {
            assertFalse(method.getValue().path("summary").asText().isBlank(), path.getKey() + " needs a summary");
            assertFalse(method.getValue().path("description").asText().isBlank(), path.getKey() + " needs a description");
        }));
        for (String property : new String[]{
                "SimulateRequestBody/properties/startTime",
                "SimulateRequestBody/properties/endTime",
                "SigverSimulateRequestBody/properties/masterModel",
                "InitializationData/properties/fmus",
                "InitializationData/properties/livestream",
                "StatusModel/properties/errors",
                "VerificationResult/properties/correct"}) {
            assertFalse(api.at("/components/schemas/" + property + "/description").asText().isBlank(), property);
        }
        JsonNode size = api.at("/components/schemas/FixedStepAlgorithmConfig").findValue("size");
        assertNotNull(size);
        assertFalse(size.path("description").asText().isBlank());
    }
}
