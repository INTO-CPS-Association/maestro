package org.intocps.maestro.webapi.esav1;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("main")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
public class ApiAndServiceInitializeTest {
    final static String baseUrl = "/api/esav1/simulator";

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @BeforeEach
    public void before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    //TODO: Overture toolwrapping FMUs has to be updated for mac catalina
    //See: https://github.com/overturetool/overture-fmu/issues/87
    @Test
    @Disabled
    //@ConditionalIgnoreRule.ConditionalIgnore(condition = BaseTest.NonMac.class)
    public void initializeSingleFMU() throws Exception {
        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/singleFmuTest1/initialize.json"));
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));
    }

    //TODO: Overture toolwrapping FMUs has to be updated for mac catalina
    //See: https://github.com/overturetool/overture-fmu/issues/87
    @Test
    @Disabled
    //@ConditionalIgnoreRule.ConditionalIgnore(condition = BaseTest.NonMac.class)
    public void initializeMultipleFMUs() throws Exception {
        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/twoFmusInitialize.json"));
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));
    }

}
