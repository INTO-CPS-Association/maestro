package org.intocps.orchestration.coe.webapi.withCoeService;

import org.apache.commons.io.IOUtils;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.webapi.services.CoeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test2")
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiAndServiceInitializeTestMockCOE {
    final static String baseUrl = "/api/esav1/simulator";
    @Autowired
    private CoeService service;
    @Autowired
    private Coe coe;
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;


    @Before
    public void before() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }


    @Test
    public void initializeSingleFMU() throws Exception {
        ArgumentCaptor<Map<String, URI>> captor = ArgumentCaptor.forClass(Map.class);

        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/singleFmuTest1/initialize.json"));
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));
    }

}
