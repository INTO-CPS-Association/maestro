package org.intocps.maestro.webapi.maestro2;

import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.webapi.Application;
import org.intocps.maestro.webapi.controllers.ControllerAdvisor;
import org.intocps.maestro.webapi.controllers.SessionLogic;
import org.intocps.maestro.webapi.maestro2.dto.InitializationData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SimulationFailureTests {
    @TempDir
    Path directory;
    private Maestro2SimulationController controller;
    private MockMvc mvc;
    private String sessionId;
    private SessionLogic logic;

    @BeforeEach
    void setup() {
        controller = spy(new Maestro2SimulationController());
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ControllerAdvisor()).build();
        sessionId = controller.createSession().sessionId;
        logic = Maestro2SimulationController.sessionController.getSessionLogic(sessionId);
        logic.setExecTime(-1);
        logic.setInitializationData(mock(InitializationData.class));
    }

    @AfterEach
    void cleanup() throws IOException {
        Files.deleteIfExists(logic.rootDirectory.toPath().resolve("simulate.json"));
        Files.deleteIfExists(logic.rootDirectory.toPath());
        Maestro2SimulationController.sessionController.removeSession(sessionId);
    }

    @Test
    void brokerExceptionsReturnServerErrorAndMarkBothSimulationModesFailed() throws Exception {
        try (var brokers = mockConstruction(Maestro2Broker.class, (broker, context) -> {
            doThrow(new IOException("Simulation failed")).when(broker).buildAndRun(any(), any(), any(), any());
            doThrow(new IOException("Simulation failed")).when(broker).buildAndRunMasterModel(any(), any(), any(), any(), any());
        })) {
            for (String endpoint : new String[]{"/simulate/", "/sigverSimulate/"}) {
                logic.setExecTime(-1);
                request(endpoint, 500);
                assertFailed();
            }
            assertEquals(2, brokers.constructed().size());
        }
    }

    @Test
    void reporterErrorsReturnServerErrorAndMarkSessionFailed() throws Exception {
        try (var brokers = mockConstruction(Maestro2Broker.class, (broker, context) ->
                ((ErrorReporter) context.arguments().get(1)).report(1, "Generation failed", null))) {
            request("/simulate/", 500);
            assertFailed();
            assertEquals(1, brokers.constructed().size());
        }
    }

    @Test
    void successfulInProcessSimulationIsUnchanged() throws Exception {
        try (var brokers = mockConstruction(Maestro2Broker.class)) {
            request("/simulate/", 200);
            assertEquals(SessionLogic.SessionStatus.Finished, logic.getStatus());
            assertEquals(1, brokers.constructed().size());
        }
    }

    @Test
    void nonzeroCliExitFailsEvenWithoutStderr() throws Exception {
        cliProcess(1, "");
        request("/simulate/", 500);
        assertFailed();
    }

    @Test
    void nonzeroCliExitIncludesDiagnostics() throws Exception {
        cliProcess(2, "FMU failed");
        mvc.perform(post("/simulate/" + sessionId).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("FMU failed")));
        assertFailed();
    }

    @Test
    void successfulCliExitWithStderrStillSucceeds() throws Exception {
        cliProcess(0, "A harmless warning");
        request("/simulate/", 200);
        assertEquals(SessionLogic.SessionStatus.Finished, logic.getStatus());
        assertTrue(logic.getLastExecTime() >= 0);
    }

    @Test
    void processLaunchFailureMarksSessionFailed() throws Exception {
        logic.setCliExecution(true);
        doThrow(new IOException("Cannot launch Java")).when(controller).startSimulationProcess(anyList());
        request("/simulate/", 500);
        assertFailed();
    }

    @Test
    void cliEntryPointPropagatesCommandFailureToProcessExit() throws Exception {
        Path output = directory.resolve("cli.log");
        Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"), Application.class.getName(), "cliMain", "--unknown-option")
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(child.waitFor(30, TimeUnit.SECONDS), "CLI did not exit");
            assertNotEquals(0, child.exitValue(), Files.readString(output));
            assertTrue(Files.readString(output).contains("--unknown-option"), Files.readString(output));
        } finally {
            if (child.isAlive()) {
                child.destroyForcibly();
            }
        }
    }

    private void cliProcess(int exitCode, String stderr) throws Exception {
        logic.setCliExecution(true);
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(stderr.getBytes(StandardCharsets.UTF_8)));
        when(process.waitFor()).thenReturn(exitCode);
        doReturn(process).when(controller).startSimulationProcess(anyList());
    }

    private void request(String endpoint, int expectedStatus) throws Exception {
        mvc.perform(post(endpoint + sessionId).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is(expectedStatus));
    }

    private void assertFailed() {
        assertEquals(SessionLogic.SessionStatus.Error, logic.getStatus());
        assertTrue(logic.getLastExecTime() >= 0, "Failed execution duration was not recorded");
    }
}
