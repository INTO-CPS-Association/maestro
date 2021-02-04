package org.intocps.maestro.webapi.maestro2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.core.ZipUtilities;
import org.intocps.maestro.webapi.maestro2.dto.InitializeStatusModel;
import org.intocps.maestro.webapi.maestro2.dto.StatusModel;
import org.junit.Before;
import org.junit.Test;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("main")
@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest
public class Issue188Tests {
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;


    @Before
    public void before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void fixedStepSimulationRelativeFMU() throws Exception {
        File initializePath = new File(Issue188Tests.class.getClassLoader().getResource("maestro2/188/initialize.json").getPath());
        fixedStepSimulationParameterizedInitialize(initializePath);
    }

    @Test
    public void fixedStepSimulationRelativeDirectoryFMU() throws Exception {
        File zipFile = new File("target/test-classes/maestro2/188/GATestController.fmu");
        File destDir = new File("target/test-classes/maestro2/188/GATestController");
        ZipUtilities.UnzipToDirectory(zipFile, destDir);
        File initializePath = new File(Issue188Tests.class.getClassLoader().getResource("maestro2/188/initializeRelativeDirectory.json").getPath());
        fixedStepSimulationParameterizedInitialize(initializePath);
    }


    public void fixedStepSimulationParameterizedInitialize(File initializePath) throws Exception {
        File simulatePath = new File(Issue188Tests.class.getClassLoader().getResource("maestro2/188/simulate.json").getPath());

        ObjectMapper om = new ObjectMapper();

        StatusModel statusModel = om.readValue(
                mockMvc.perform(get("/createSession")).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(),
                StatusModel.class);

        InitializeStatusModel initializeResponse = om.readValue(mockMvc.perform(
                post("/initialize/" + statusModel.sessionId).content(FileUtils.readFileToByteArray(initializePath))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse()
                .getContentAsString(), InitializeStatusModel.class);

        byte[] simulateMessageContent = FileUtils.readFileToByteArray(simulatePath);

        InitializeStatusModel simulateResponse = om.readValue(
                mockMvc.perform(post("/simulate/" + statusModel.sessionId).content(simulateMessageContent).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(), InitializeStatusModel.class);

        byte[] zippedResult =
                mockMvc.perform(get("/result/" + statusModel.sessionId + "/zip")).andExpect(status().is(HttpStatus.OK.value())).andReturn()
                        .getResponse().getContentAsByteArray();
        ZipInputStream istream = new ZipInputStream(new ByteArrayInputStream(zippedResult));
        List<ZipEntry> entries = new ArrayList<>();
        ZipEntry entry = istream.getNextEntry();
        String mablSpec = null;
        String outputs = null;
        String equations_log = null;
        while (entry != null) {
            entries.add(entry);
            if (entry.getName().equals("spec.mabl")) {
                mablSpec = IOUtils.toString(istream, StandardCharsets.UTF_8);
            }
            if (entry.getName().equals("outputs.csv")) {
                outputs = IOUtils.toString(istream, StandardCharsets.UTF_8);
            }
            if (entry.getName().equals("equations.log")) {
                equations_log = IOUtils.toString(istream, StandardCharsets.UTF_8);
            }
            entry = istream.getNextEntry();

        }
        istream.closeEntry();
        istream.close();

        mockMvc.perform(get("/destroy/" + statusModel.sessionId)).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse()
                .getContentAsString();

        List<String> filesInZip = entries.stream().map(l -> l.getName()).collect(Collectors.toList());

        assertThat(filesInZip)
                .containsExactlyInAnyOrder("initialize.json", "simulate.json", "spec.mabl", "outputs.csv", "spec.runtime.json", "equations.log");


    }
}
