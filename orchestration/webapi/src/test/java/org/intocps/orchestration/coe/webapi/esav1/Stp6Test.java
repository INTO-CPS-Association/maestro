package org.intocps.orchestration.coe.webapi.esav1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Files;
import org.intocps.orchestration.coe.webapi.BaseTest;
import org.intocps.orchestration.coe.webapi.ConditionalIgnoreRule;
import org.intocps.orchestration.coe.webapi.services.CoeService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.match.ContentRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("main")
@RunWith(SpringRunner.class)
@SpringBootTest
public class Stp6Test {
    final static String baseUrl = "/api/esav1/simulator";
    @Autowired
    CoeService service;
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Rule
    public final ConditionalIgnoreRule ConditionalIgnore = new ConditionalIgnoreRule();

    @Before
    public void before() {

        service.reset();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void pingTest() throws Exception {
        mockMvc.perform(post(baseUrl + "/ping").contentType(APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value()));
    }

    //TODO: Overture toolwrapping FMUs has to be updated for mac catalina
    //See: https://github.com/overturetool/overture-fmu/issues/87
    @Test
    //@ConditionalIgnoreRule.ConditionalIgnore(condition = BaseTest.NonMac.class)
    public void initializeTest() throws Exception {

        String data = Files.contentOf(Paths.get("src", "test", "resources", "esa", "STP6", "1-initialize.json").toFile(), StandardCharsets.UTF_8);
        data = data.replace("watertankController-Standalone.fmu",
                "file:" + Paths.get("src", "test", "resources", "esa", "fmus", "watertankController-Standalone.fmu").toAbsolutePath().toString());
        data = data.replace("singlewatertank-20sim.fmu",
                "file:" + Paths.get("src", "test", "resources", "esa", "fmus", "singlewatertank-20sim.fmu").toAbsolutePath().toString());


        ContentRequestMatchers x;
        mockMvc.perform(post(baseUrl + "/initialize").content(data).contentType(APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value()));
    }

    //TODO: Overture toolwrapping FMUs has to be updated for mac catalina
    //See: https://github.com/overturetool/overture-fmu/issues/87
    @Test
    //@ConditionalIgnoreRule.ConditionalIgnore(condition = BaseTest.NonMac.class)
    public void simulateTest() throws Exception {
        initializeTest();

        String data = Files.contentOf(Paths.get("src", "test", "resources", "esa", "STP6", "1-simulateFor.json").toFile(), StandardCharsets.UTF_8);

        TypeReference<Map<String, Map<String, Object>>> valueTypeRef = new TypeReference<Map<String, Map<String, Object>>>() {
        };


        String response = mockMvc.perform(post(baseUrl + "/simulate").content(data).contentType(APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString();
        Map<String, Map<String, Object>> actualOutput = new ObjectMapper().readValue(response, valueTypeRef);

        //        Map<String, Map<String, Object>> expectedOutput = new ObjectMapper()
        //                .readValue(Paths.get("src", "test", "resources", "esa", "STP6", "simulateForResult.json").toFile(), valueTypeRef);
        //
        //        Assert.assertEquals(expectedOutput, actualOutput);
    }


    //TODO: Overture toolwrapping FMUs has to be updated for mac catalina
    //See: https://github.com/overturetool/overture-fmu/issues/87
    @Test
    //@ConditionalIgnoreRule.ConditionalIgnore(condition = BaseTest.NonMac.class)
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
