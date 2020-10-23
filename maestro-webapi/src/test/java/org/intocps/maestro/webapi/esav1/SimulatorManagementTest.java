package org.intocps.maestro.webapi.esav1;


import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.intocps.maestro.webapi.services.SimulatorManagementService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ActiveProfiles("mocked_coe_service")
@RunWith(SpringRunner.class)
@SpringBootTest
public class SimulatorManagementTest {

    final static String baseUrl = "/api/esav1/orchestrator";
    private final static Logger logger = LoggerFactory.getLogger(SimulatorManagementTest.class);
    @Autowired
    SimulatorManagementService service;
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Before
    public void before() throws Exception {
        Mockito.reset(service);
        when(service.create(any())).thenReturn("localhost:8000");
        when(service.getSimulatorDirectory(anyString())).thenReturn(new File("."));
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @Ignore("See #85")
    public void pingTest() throws Exception {

        Mockito.reset(service);
        mockMvc.perform(post(baseUrl + "/ping").contentType(APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    @Ignore("See #85")
    public void createTest() throws Exception {

        String res = mockMvc.perform(post(baseUrl + "/").accept(APPLICATION_JSON).contentType(APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.OK.value())).andExpect(jsonPath("$.instance_url", new UrlMather("localhost")))
                .andExpect(jsonPath("$.instance_id", org.hamcrest.Matchers.notNullValue())).andReturn().getResponse().getContentAsString();
        logger.debug("create => {}", res);

    }

    @Test
    @Ignore("See #85")
    public void createTestXml() throws Exception {

        String res = mockMvc.perform(post(baseUrl + "/").accept(APPLICATION_XML).contentType(APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString();
        logger.debug("create => {}", res);

    }


    @Test
    @Ignore("FIX ME")
    public void createFailureTest() throws Exception {

        when(service.create(any())).thenThrow(new Exception());
        mockMvc.perform(post(baseUrl + "/").accept(APPLICATION_JSON).contentType(APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()));

    }

    @Test
    @Ignore("See #85")
    public void createDelete() throws Exception {

        String uid = UUID.randomUUID().toString();

        when(service.delete(eq(uid))).thenReturn(true);

        mockMvc.perform(delete(baseUrl + "/" + uid).accept(APPLICATION_JSON).contentType(APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.OK.value()));

    }

    @Test
    @Ignore("FIX ME")
    public void createDeleteNoExisting() throws Exception {

        String uid = UUID.randomUUID().toString();

        when(service.delete(eq(uid))).thenReturn(false);

        mockMvc.perform(delete(baseUrl + "/" + uid).accept(APPLICATION_JSON).contentType(APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()));

    }

    @Test
    @Ignore("FIX ME")
    public void createDeleteFailure() throws Exception {

        String uid = UUID.randomUUID().toString();

        when(service.delete(eq(uid))).thenThrow(new RuntimeException());

        mockMvc.perform(delete(baseUrl + "/" + uid).accept(APPLICATION_JSON).contentType(APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()));

    }


    @Test
    @Ignore("See #85")
    public void terminate() throws Exception {

        mockMvc.perform(post(baseUrl + "/terminate").accept(APPLICATION_JSON).contentType(APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.OK.value()));

    }

    @Test
    @Ignore("FIX ME")
    public void terminateFailure() throws Exception {

        doThrow(new RuntimeException()).when(service).terminateApplication();

        mockMvc.perform(post(baseUrl + "/terminate").accept(APPLICATION_JSON).contentType(APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()));

    }

    static class UrlMather extends BaseMatcher<String> {

        final String expectedHost;

        UrlMather(String expectedHost) {
            this.expectedHost = expectedHost;
        }

        @Override
        public boolean matches(Object item) {

            if (item instanceof String) {
                String[] parts = ((String) item).split(":");

                try {
                    return parts.length == 2 && expectedHost.equals(parts[0]) && Integer.parseInt(parts[1]) > 0;
                } catch (NumberFormatException e) {
                    return false;
                }
            }

            return false;
        }

        @Override
        public void describeTo(Description description) {

        }
    }
}
