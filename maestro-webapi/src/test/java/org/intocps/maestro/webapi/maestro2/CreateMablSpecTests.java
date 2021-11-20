package org.intocps.maestro.webapi.maestro2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.webapi.maestro2.dto.InitializeStatusModel;
import org.intocps.maestro.webapi.maestro2.dto.StatusModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
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
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@SpringBootTest
public class CreateMablSpecTests {

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    public static String getWaterTankMMJson(boolean useFixedStep) {
        String singlewatertank_20simfmu = "file:///" +
                Paths.get("src", "test", "resources", "maestro2", "watertankexample", "singlewatertank-20sim.fmu").toAbsolutePath().toString()
                        .replace("\\", "/");
        String watertankcontroller_cfmu = "file:///" +
                Paths.get("src", "test", "resources", "maestro2", "watertankexample", "watertankcontroller-c.fmu").toAbsolutePath().toString()
                        .replace("\\", "/");

        String VariableStepalgorithm = "\"algorithm\":{\"type\":\"var-step\",\"initsize\":0,\"size\":[1e-16,1]," +
                "\"constraints\":{\"max\":{\"type\":\"zerocrossing\",\"ports\":[\"{wt}.wtInstance.level\",\"{crtl}.crtlInstance.maxlevel\"],\"order\":1,\"abstol\":0" +
                ".01},\"min\":{\"type\":\"zerocrossing\",\"ports\":[\"{wt}.wtInstance.level\",\"{crtl}.crtlInstance.minlevel\"],\"order\":1,\"abstol\":0.01,\"safety\":1}}}";

        String FixedStepAlgorithm = "  \"algorithm\": {\n \"type\": \"fixed-step\",\n \"size\": 0.1\n}";

        String json = "{\n" + "  \"fmus\": {\n" + "    \"{crtl}\": \"" + watertankcontroller_cfmu + "\",\n" + "    \"{wt}\": \"" +
                singlewatertank_20simfmu + "\"\n" + "  },\n" + "  \"connections\": {\n" + "    \"{crtl}.crtlInstance.valve\": [\n" +
                "      \"{wt}.wtInstance.valvecontrol\"\n" + "    ],\n" + "    \"{wt}.wtInstance.level\": [\n" +
                "      \"{crtl}.crtlInstance.level\"\n" + "    ]\n" + "  },\n" + "  \"parameters\": {\n" +
                "    \"{crtl}.crtlInstance.maxlevel\": 2,\n" + "    \"{crtl}.crtlInstance.minlevel\": 1\n" + "  },\n" +
                (useFixedStep ? FixedStepAlgorithm : VariableStepalgorithm) + "\n}";
        return json;

    }

    @BeforeEach
    public void before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void variableStepSimulation() throws Exception {

        ObjectMapper om = new ObjectMapper();
        StatusModel statusModel = om.readValue(
                mockMvc.perform(get("/createSession")).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(),
                StatusModel.class);
        InitializeStatusModel initializeResponse = om.readValue(mockMvc.perform(
                        post("/initialize/" + statusModel.sessionId).content(getWaterTankMMJson(false)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(), InitializeStatusModel.class);
        File start_messageFile =
                new File(Paths.get("src", "test", "resources", "maestro2", "watertankexample", "start_message.json").toAbsolutePath().toString());
        byte[] start_messageContent = FileUtils.readFileToByteArray(start_messageFile);

        InitializeStatusModel simulateResponse = om.readValue(
                mockMvc.perform(post("/simulate/" + statusModel.sessionId).content(start_messageContent).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(), InitializeStatusModel.class);

        byte[] zippedResult =
                mockMvc.perform(get("/result/" + statusModel.sessionId + "/zip")).andExpect(status().is(HttpStatus.OK.value())).andReturn()
                        .getResponse().getContentAsByteArray();
        ZipInputStream istream = new ZipInputStream(new ByteArrayInputStream(zippedResult));
        List<ZipEntry> entries = new ArrayList<>();
        ZipEntry entry = istream.getNextEntry();
        String mablSpec = null;
        while (entry != null) {
            entries.add(entry);
            if (entry.getName().equals("spec.mabl")) {
                mablSpec = IOUtils.toString(istream, StandardCharsets.UTF_8);
            }
            entry = istream.getNextEntry();

        }
        istream.closeEntry();
        istream.close();

        mockMvc.perform(get("/destroy/" + statusModel.sessionId)).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse()
                .getContentAsString();

        List<String> filesInZip = entries.stream().map(l -> l.getName()).collect(Collectors.toList());

        assertThat(filesInZip).containsExactlyInAnyOrder("initialize.json", "simulate.json", "spec.mabl", "outputs.csv", "spec.runtime.json",
                "crtlInstance.log", "wtInstance.log");


    }

    @Test
    public void fixedStepSimulation() throws Exception {

        ObjectMapper om = new ObjectMapper();
        StatusModel statusModel = om.readValue(
                mockMvc.perform(get("/createSession")).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(),
                StatusModel.class);
        InitializeStatusModel initializeResponse = om.readValue(mockMvc.perform(
                        post("/initialize/" + statusModel.sessionId).content(getWaterTankMMJson(true)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(), InitializeStatusModel.class);
        File start_messageFile =
                new File(Paths.get("src", "test", "resources", "maestro2", "watertankexample", "start_message.json").toAbsolutePath().toString());
        byte[] start_messageContent = FileUtils.readFileToByteArray(start_messageFile);

        InitializeStatusModel simulateResponse = om.readValue(
                mockMvc.perform(post("/simulate/" + statusModel.sessionId).content(start_messageContent).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(), InitializeStatusModel.class);

        byte[] zippedResult =
                mockMvc.perform(get("/result/" + statusModel.sessionId + "/zip")).andExpect(status().is(HttpStatus.OK.value())).andReturn()
                        .getResponse().getContentAsByteArray();
        ZipInputStream istream = new ZipInputStream(new ByteArrayInputStream(zippedResult));
        List<ZipEntry> entries = new ArrayList<>();
        ZipEntry entry = istream.getNextEntry();
        String mablSpec = null;
        while (entry != null) {
            entries.add(entry);
            if (entry.getName().equals("spec.mabl")) {
                mablSpec = IOUtils.toString(istream, StandardCharsets.UTF_8);
            }
            entry = istream.getNextEntry();

        }
        istream.closeEntry();
        istream.close();

        mockMvc.perform(get("/destroy/" + statusModel.sessionId)).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse()
                .getContentAsString();

        List<String> filesInZip = entries.stream().map(l -> l.getName()).collect(Collectors.toList());

        assertThat(filesInZip).containsExactlyInAnyOrder("initialize.json", "simulate.json", "spec.mabl", "outputs.csv", "spec.runtime.json",
                "crtlInstance.log", "wtInstance.log");


    }
}
