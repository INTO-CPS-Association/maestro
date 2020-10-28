package org.intocps.maestro.webapi.esav1;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.webapi.services.CoeService;
import org.intocps.orchestration.coe.scala.Coe;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("mocked_coe_service")
@RunWith(SpringRunner.class)
@SpringBootTest
public class EsaApiAndCoeServiceTest {
    final static String baseUrl = "/api/esav1/simulator";
    File root = null;
    @Autowired
    private CoeService service;
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Before
    public void before() {
        String session = UUID.randomUUID().toString();
        root = Paths.get("target", session).toFile();
        root.mkdirs();
        Coe coe = new Coe(root);

        Mockito.reset(service);
        when(service.get()).thenReturn(coe);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }


    @After
    public void after() throws IOException {
        if (root != null && root.exists()) {
            FileUtils.deleteDirectory(root);
        }
    }

    @Test
    @Ignore
    public void initializeSingleFMU() throws Exception {
        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/singleFmuTest1/initialize.json"));
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));
    }

}
