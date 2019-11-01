package org.intocps.orchestration.coe.webapi;

import org.apache.commons.io.IOUtils;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.webapi.services.CoeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class EsaApiTest {
    final static String baseUrl = "/api/esav1/simulator";
    private final Coe coe = Mockito.mock(Coe.class);
    @Autowired
    private CoeService service;
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;


    @Before
    public void before() throws Exception {
        Mockito.reset(service);
        Mockito.reset(coe);

        when(service.get()).thenReturn(coe);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void pingTest() throws Exception {
        mockMvc.perform(post(baseUrl + "/ping").contentType(APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value()));
    }


    @Test
    public void initialize() throws Exception {

        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/test1/initialize.json"));
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));
    }


    @Test
    public void initializeFailure() throws Exception {

        when(service.get()).thenThrow(new RuntimeException());

        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/test1/initialize.json"));
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }


    @Test
    public void simulate() throws Exception {

        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/test1/simulate.json"));
        mockMvc.perform(post(baseUrl + "/simulate").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));
    }
}
