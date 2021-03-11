package org.intocps.maestro.webapi.esav1;

import org.apache.commons.io.IOUtils;
import org.intocps.maestro.webapi.services.CoeService;
import org.intocps.orchestration.coe.scala.Coe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("mocked_coe_service")
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class EsaApiTest {
    final static String baseUrl = "/api/esav1/simulator";
    private final Coe coe = Mockito.mock(Coe.class);
    @Autowired
    private CoeService service;
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;


    @BeforeEach
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
    @Disabled
    public void initialize() throws Exception {

        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/test1/initialize.json"));
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));
    }


    @Test
    @Disabled("FIX ME")
    public void initializeFailure() throws Exception {

        when(service.get()).thenThrow(new RuntimeException());

        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/test1/initialize.json"));
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }


    @Test
    @Disabled
    public void simulate() throws Exception {

        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/test1/simulate.json"));
        mockMvc.perform(post(baseUrl + "/simulate").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));
    }
}
