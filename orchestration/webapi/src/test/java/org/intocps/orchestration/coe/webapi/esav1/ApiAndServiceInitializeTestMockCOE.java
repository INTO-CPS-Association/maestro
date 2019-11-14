package org.intocps.orchestration.coe.webapi.esav1;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.orchestration.coe.config.CoeConfiguration;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.webapi.services.CoeService;
import org.junit.After;
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("mocked_coe")
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiAndServiceInitializeTestMockCOE {
    final static String baseUrl = "/api/esav1/simulator";
    @Autowired
    private CoeService service;
    private Coe mockedCoe;
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;
    private File root;


    @Before
    public void before() {

        mockedCoe = service.get();

        String session = UUID.randomUUID().toString();
        root = Paths.get("target", session).toFile();
        root.mkdirs();
        when(mockedCoe.getResultRoot()).thenReturn(root);
        CoeConfiguration coeConfiguration = new CoeConfiguration();
        when(mockedCoe.getConfiguration()).thenReturn(coeConfiguration);
        List<ModelDescription.LogCategory> logCategories = new ArrayList<>();
        logCategories.add(new ModelDescription.LogCategory("logAll", ""));
        logCategories.add(new ModelDescription.LogCategory("logError", ""));
        logCategories.add(new ModelDescription.LogCategory("VdmErr", ""));
        HashMap<String, List<ModelDescription.LogCategory>> debugLoggingLevels = new HashMap<>();
        debugLoggingLevels.put("{controllerFmu}.crtlIns", logCategories);
        when(mockedCoe.initialize(any(), any(), any(), any(), any())).thenReturn(debugLoggingLevels);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @After
    public void after() throws IOException {
        if (root != null && root.exists()) {
            FileUtils.deleteDirectory(root);
        }
    }


    @Test
    public void initializeSingleFMU() throws Exception {
        ArgumentCaptor<Map<String, URI>> captor = ArgumentCaptor.forClass(Map.class);

        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/singleFmuTest1/initialize.json"));
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));
    }

}
