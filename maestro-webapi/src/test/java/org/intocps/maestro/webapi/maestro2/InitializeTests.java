package org.intocps.maestro.webapi.maestro2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.webapi.maestro2.dto.InitializeStatusModel;
import org.intocps.maestro.webapi.maestro2.dto.StatusModel;
import org.junit.Assert;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("main")
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@SpringBootTest
public class InitializeTests {

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @BeforeEach
    public void before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void fixedStepSimulation() throws Exception {

        ObjectMapper om = new ObjectMapper();
        StatusModel statusModel = om.readValue(
                mockMvc.perform(get("/createSession")).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(),
                StatusModel.class);
        InitializeStatusModel initializeResponse = om.readValue(mockMvc.perform(
                        post("/initialize/" + statusModel.sessionId).content(CreateMablSpecTests.getWaterTankMMJson(true))
                                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse()
                .getContentAsString(), InitializeStatusModel.class);


        mockMvc.perform(get("/destroy/" + statusModel.sessionId)).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse()
                .getContentAsString();

        Assert.assertTrue("AvailableLogLevels contains two keys", initializeResponse.availableLogLevels.keySet().size() == 2);
        Assert.assertTrue("AvailableLogLevels {crtl} contains 4 logcategories", initializeResponse.availableLogLevels.get("{crtl}").size() == 4);


    }
}
