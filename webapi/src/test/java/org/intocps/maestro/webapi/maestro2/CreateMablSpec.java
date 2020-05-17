package org.intocps.maestro.webapi.maestro2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.intocps.maestro.webapi.controllers.Maestro2SimulationController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("main")
@RunWith(SpringRunner.class)
@SpringBootTest
public class CreateMablSpec {

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    //@ConditionalIgnoreRule.ConditionalIgnore(condition = BaseTest.NonMac.class)
    public void initializeSingleFMU() throws Exception {
        ObjectMapper om = new ObjectMapper();
        Maestro2SimulationController.StatusModel statusModel = om.readValue(
                mockMvc.perform(get("/createSession")).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(),
                Maestro2SimulationController.StatusModel.class);
        Maestro2SimulationController.InitializeStatusModel initializeResponse = om.readValue(
                mockMvc.perform(post("/initialize/" + statusModel.sessionId).content(getWaterTankMMJson()).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(),
                Maestro2SimulationController.InitializeStatusModel.class);
        File start_messageFile =
                new File(Paths.get("src", "test", "resources", "maestro2", "watertankexample", "start_message.json").toAbsolutePath().toString());
        byte[] start_messageContent = FileUtils.readFileToByteArray(start_messageFile);

        Maestro2SimulationController.InitializeStatusModel simulateResponse = om.readValue(
                mockMvc.perform(post("/simulate/" + statusModel.sessionId).content(start_messageContent).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(),
                Maestro2SimulationController.InitializeStatusModel.class);

        mockMvc.perform(get("/destroy/" + statusModel.sessionId)).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse()
                .getContentAsString();


    }

    public String getWaterTankMMJson() {
        String singlewatertank_20simfmu =
                Paths.get("src", "test", "resources", "maestro2", "watertankexample", "singlewatertank-20sim.fmu").toAbsolutePath().toString();
        String watertankcontroller_cfmu =
                Paths.get("src", "test", "resources", "maestro2", "watertankexample", "watertankcontroller-c.fmu").toAbsolutePath().toString();

        String json = "{\n" + "  \"fmus\": {\n" + "    \"{crtl}\": \"" + watertankcontroller_cfmu + "\",\n" + "    \"{wt}\": \"" +
                singlewatertank_20simfmu + "\"\n" + "  },\n" + "  \"connections\": {\n" + "    \"{crtl}.crtlInstance.valve\": [\n" +
                "      \"{wt}.wtInstance.valvecontrol\"\n" + "    ],\n" + "    \"{wt}.wtInstance.level\": [\n" +
                "      \"{crtl}.crtlInstance.level\"\n" + "    ]\n" + "  },\n" + "  \"parameters\": {\n" +
                "    \"{crtl}.crtlInstance.maxlevel\": 2,\n" + "    \"{crtl}.crtlInstance.minlevel\": 1\n" + "  },\n" + "  \"algorithm\": {\n" +
                "    \"type\": \"fixed-step\",\n" + "    \"size\": 0.1\n" + "  }\n" + "}";
        return json;

    }
}
