package org.intocps.maestro.webapi.esav1;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.webapi.services.CoeService;
import org.intocps.maestro.webapi.services.EnvironmentFMUFactory;
import org.intocps.orchestration.coe.config.CoeConfiguration;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.scala.Coe;
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("mocked_coe")
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiAndServiceInitializeTestMockCOE {
    final static String baseUrl = "/api/esav1/simulator";
    HashMap<String, List<ModelDescription.LogCategory>> debugLoggingLevels = new HashMap<>();
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

        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/singleFmuTest1/initialize.json"));
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void initializeTwoFmus() throws Exception {

        String body = IOUtils.toString(this.getClass().getResourceAsStream("/esa/twoFmusInitialize.json"));
        mockMvc.perform(post(baseUrl + "/initialize").contentType(APPLICATION_JSON).content(body)).andExpect(status().is(HttpStatus.OK.value()));

        ArgumentCaptor<List<ModelConnection>> captorConnections = ArgumentCaptor.forClass(List.class);

        verify(mockedCoe, times(1)).initialize(any(), captorConnections.capture(), any(), any(), any());

        ModelConnection.ModelInstance environmentFMUInstance = new ModelConnection.ModelInstance("{" + EnvironmentFMUFactory.EnvironmentFmuName + "}",
                EnvironmentFMUFactory.EnvironmentComponentIdentificationId);

        List<ModelConnection> actual = captorConnections.getValue();
        assertTrue(actual.stream()
                .anyMatch(x -> x.from.instance.toString().equals(environmentFMUInstance.toString()) && x.from.variable.startsWith("valvecontrol")));
        //FIXME Extend with checking the following connections
        // - modelConnection.to of the existing test
        // - inputs to environment FMU

    }

}
