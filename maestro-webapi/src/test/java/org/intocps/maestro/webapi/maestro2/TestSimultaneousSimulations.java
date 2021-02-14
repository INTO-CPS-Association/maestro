package org.intocps.maestro.webapi.maestro2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.compress.archivers.zip.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.intocps.fmi.jnifmuapi.ZipUtility;
import org.intocps.maestro.core.ZipUtilities;
import org.intocps.maestro.webapi.maestro2.dto.InitializeStatusModel;
import org.intocps.maestro.webapi.maestro2.dto.StatusModel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ActiveProfiles("main")
@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest
public class TestSimultaneousSimulations {
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;
    private File simulationJson, initJson;

    @BeforeEach
    public void before()
    {
        // setup the webapp so that we can send things to it
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        simulationJson = new File(TestSimultaneousSimulations.class.getClassLoader().getResource("maestro2/ThreadedTest/simulate.json").getPath());
        initJson = new File(TestSimultaneousSimulations.class.getClassLoader().getResource("maestro2/ThreadedTest/initialize.json").getPath());
    }

    @ParameterizedTest
    @ValueSource(ints = {2})
    public void TestMultithreadedPerformance(int threads) throws Exception
    {
        RunSingleSimulation();
        Assertions.fail();
    }

    private void RunSingleSimulation() throws Exception
    {
        var om = new ObjectMapper();
        var statusModel =
                om.readValue(mockMvc.perform(get("/createSession")).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(), StatusModel.class);

        var sessionId = statusModel.sessionId;
        CheckSimpleResponse("session exists", sessionId, statusModel);

        var initResponse =
                om.readValue(mockMvc.perform(post("/initialize/" + sessionId).content(FileUtils.readFileToByteArray(initJson)).contentType(MediaType.APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(),
                InitializeStatusModel.class);

        CheckSimpleResponse("initialized", sessionId, initResponse);

        var simulateMessageContent = FileUtils.readFileToByteArray(simulationJson);
        var simResponse =
                om.readValue(mockMvc.perform(post("/simulate/" + sessionId).content(simulateMessageContent).contentType(MediaType.APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(), InitializeStatusModel.class);

        CheckSimpleResponse("simulation completed", sessionId, simResponse);

        var zippedResult =
                mockMvc.perform(get("/result/" + sessionId + "/zip")).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsByteArray();
        Assertions.assertNotNull(zippedResult);

        var iStream = new ZipInputStream(new ByteArrayInputStream(zippedResult));
        var entries = new ArrayList<ZipEntry>();
        ZipEntry entry;

        while((entry = iStream.getNextEntry()) != null)
            entries.add(entry);

        var zipEntryNames = entries.stream().map(ZipEntry::getName).collect(Collectors.toList());
        assertThat(zipEntryNames).containsExactlyInAnyOrder("initialize.json", "simulate.json", "spec.mabl", "outputs.csv", "spec.runtime.json","equations.log");
    }

    private void CheckSimpleResponse(String expectedStatus, String sessionId, StatusModel reponse)
    {
        Assertions.assertEquals(expectedStatus.toLowerCase(), reponse.status.toLowerCase());
        // check we arnt getting confused between the threads so check we always have the correct session id
        Assertions.assertEquals(sessionId, reponse.sessionId);
    }
}
