package org.intocps.orchestration.coe.webapi;

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

import static org.mockito.ArgumentMatchers.any;
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

        when(service.create(any())).thenReturn(coe);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void pingTest() throws Exception {
        mockMvc.perform(post(baseUrl + "/ping").contentType(APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value()));
    }


    @Test
    public void initialize() throws Exception {

        String body = "{\n" + " \"fmus\":{\n" + " \"{controllerFmu}\":\"file://controller.fmu\",\n" + " \"{tankFmu}\":\"file ://tank.fmu\"\n" + " " + "},\n" + " \"connections\":{\n" + " \"{controllerFmu}.crtlIns.valve\":[\"{tankFmu}.tankIns.valve\"],\n" + " \"{tankFmu}.tankIns" + ".level\":[\"{controllerFmu}.crtlIns.level\"]\n" + " },\n" + " \"parameters\":{\n" + " \"{controllerFmu}.crtlIns.maxLevel\":8,\n" + " \"{controllerFmu}.crtlIns.minLevel\":2\n" + " },\n" + "\"inputs\":{\n" + " \"{tankFmu}.tankIns.valve\":true\n" + " },\n" + " \"requested_outputs\":{\n" + " \"{controllerFmu}.crtlIns\":[\"valve\"],\n" + " \"{controllerFmu}.tankIns\":[\"level\"]\n" + " },\n" + " \"log_levels\":{\n" + " \"{controllerFmu}.crtlIns\":[\"logAll\", \"logError\",\"VdmErr\"],\n" + " \"{tankFmu}.tankIns\":[]\n" + " },\n" + " \"step_size\":0.2,\n" + " \"end_time\":10,\n" + " \"simulator_log_level\":\"TRACE\"\n" + "}";
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));
    }


    @Test
    public void initializeFailure() throws Exception {

        when(service.create(any())).thenThrow(new RuntimeException());

        String body = "{\n" + " \"fmus\":{\n" + " \"{controllerFmu}\":\"file://controller.fmu\",\n" + " \"{tankFmu}\":\"file ://tank.fmu\"\n" + " " + "},\n" + " \"connections\":{\n" + " \"{controllerFmu}.crtlIns.valve\":[\"{tankFmu}.tankIns.valve\"],\n" + " \"{tankFmu}.tankIns" + ".level\":[\"{controllerFmu}.crtlIns.level\"]\n" + " },\n" + " \"parameters\":{\n" + " \"{controllerFmu}.crtlIns.maxLevel\":8,\n" + " \"{controllerFmu}.crtlIns.minLevel\":2\n" + " },\n" + "\"inputs\":{\n" + " \"{tankFmu}.tankIns.valve\":true\n" + " },\n" + " \"requested_outputs\":{\n" + " \"{controllerFmu}.crtlIns\":[\"valve\"],\n" + " \"{controllerFmu}.tankIns\":[\"level\"]\n" + " },\n" + " \"log_levels\":{\n" + " \"{controllerFmu}.crtlIns\":[\"logAll\", \"logError\",\"VdmErr\"],\n" + " \"{tankFmu}.tankIns\":[]\n" + " },\n" + " \"step_size\":0.2,\n" + " \"end_time\":10,\n" + " \"simulator_log_level\":\"TRACE\"\n" + "}";
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }


    //    @Test
    //    public void simulate() throws Exception {
    //
    //        String body = "{\n" + " \"time_step\":0.2,\n" + "\"inputs\":{\n" + " \"{tankFmu}.tankIns.valve\":true\n" + " }\n" + "}";
    //        mockMvc.perform(post(baseUrl + "/simulate").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));
    //    }
}
