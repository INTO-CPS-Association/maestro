package org.intocps.orchestration.coe.webapi.esav1;

import org.assertj.core.util.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("main")
@RunWith(SpringRunner.class)
@SpringBootTest
public class Stp3Instance1Test {
    final static String baseUrl = "/api/esav1/simulator";
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Before
    public void before() {

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void pingTest() throws Exception {
        mockMvc.perform(post(baseUrl + "/ping").contentType(APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void initializeTest() throws Exception {

        String data = Files.contentOf(Paths.get("src", "test", "resources", "esa", "STP3", "1-initialize.json").toFile(), StandardCharsets.UTF_8);
        data = data.replace("watertankController.fmu",
                "file:" + Paths.get("src", "test", "resources", "esa", "fmus", "watertankController.fmu").toAbsolutePath().toString());
        data = data.replace("singlewatertank-20sim.fmu",
                "file:" + Paths.get("src", "test", "resources", "esa", "fmus", "singlewatertank-20sim.fmu").toAbsolutePath().toString());

        mockMvc.perform(post(baseUrl + "/initialize").content(data).contentType(APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void simulateTest() throws Exception {
        initializeTest();

        String data = Files.contentOf(Paths.get("src", "test", "resources", "esa", "STP3", "1-simulateFor.json").toFile(), StandardCharsets.UTF_8);

        mockMvc.perform(post(baseUrl + "/simulate").content(data).contentType(APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value()));
    }


    @Test
    public void simulateAndGetResultTest() throws Exception {
        simulateTest();


        mockMvc.perform(post(baseUrl + "/stop").contentType(APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value()));

        String result = mockMvc.perform(get(baseUrl + "/result/plain").contentType(APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        System.out.println(result);
    }
}
