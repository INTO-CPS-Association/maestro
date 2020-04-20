package org.intocps.orchestration.coe.webapi.esav1;

import org.apache.commons.io.IOUtils;
import org.intocps.orchestration.coe.webapi.BaseTest;
import org.intocps.orchestration.coe.webapi.ConditionalIgnoreRule;
import org.junit.Before;
import org.junit.Rule;
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

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("main")
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiAndServiceInitializeTest {
    final static String baseUrl = "/api/esav1/simulator";
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Rule
    public final ConditionalIgnoreRule ConditionalIgnore = new ConditionalIgnoreRule();

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    //TODO: Overture toolwrapping FMUs has to be updated for mac catalina
    //See: https://github.com/overturetool/overture-fmu/issues/87
    @Test
    //@ConditionalIgnoreRule.ConditionalIgnore(condition = BaseTest.NonMac.class)
    public void initializeSingleFMU() throws Exception {
        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/singleFmuTest1/initialize.json"));
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));
    }

    //TODO: Overture toolwrapping FMUs has to be updated for mac catalina
    //See: https://github.com/overturetool/overture-fmu/issues/87
    @Test
    //@ConditionalIgnoreRule.ConditionalIgnore(condition = BaseTest.NonMac.class)
    public void initializeMultipleFMUs() throws Exception {
        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/twoFmusInitialize.json"));
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));
    }

}
