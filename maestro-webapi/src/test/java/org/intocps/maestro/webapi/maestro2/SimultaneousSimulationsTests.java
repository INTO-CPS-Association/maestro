package org.intocps.maestro.webapi.maestro2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.intocps.maestro.webapi.maestro2.dto.InitializeStatusModel;
import org.intocps.maestro.webapi.maestro2.dto.StatusModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runner.RunWith;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ActiveProfiles("main")
@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest
public class SimultaneousSimulationsTests {
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;
    private File simulationJson, initJson, expectedOutputCSV;

    @BeforeEach
    public void before()
    {
        // setup the webapp so that we can send things to it
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        simulationJson = new File(SimultaneousSimulationsTests.class.getClassLoader().getResource("maestro2/ThreadedTest/simulate.json").getPath());
        initJson = new File(SimultaneousSimulationsTests.class.getClassLoader().getResource("maestro2/ThreadedTest/initialize.json").getPath());
        expectedOutputCSV = new File(SimultaneousSimulationsTests.class.getClassLoader().getResource("maestro2/ThreadedTest/outputs.csv").getPath());
    }

    @ParameterizedTest
    @ValueSource(ints = {1})
    public void TestMultithreadedPerformance(int threadCount) throws Exception
    {
        var threads = new ArrayList<Thread>();
        final AssertionFailedError[] failedTest = new AssertionFailedError[1];

        for(var t = 0; t < threadCount; t++)
        {
            var thread = new Thread() {
                public void run() {
                    try {
                        RunSingleSimulation();
                    } catch (AssertionFailedError f)
                    {
                        failedTest[0] = f;
                        // specifically catch assertion errors since junit wont detect anything that dosn't come from the main thread
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            threads.add(thread);

            thread.start();
        }

        while(threads.size() != 0)
        {
            threads.remove(0).join();
            if(failedTest[0] != null)
                throw failedTest[0]; // rethrow any failed tests so that the test actually fail
        }
    }

    private void RunSingleSimulation() throws Exception
    {
        var om = new ObjectMapper();
        var statusModel =
                om.readValue(mockMvc.perform(get("/createSession")).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(), StatusModel.class);

        var sessionId = statusModel.sessionId;
        CheckSimpleResponse("session exists", sessionId, statusModel);

        var initResponse =
                om.readValue(mockMvc.perform(post("/initialize/" + sessionId).content(FileUtils.readFileToByteArray(initJson)).contentType(MediaType.APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(),
                InitializeStatusModel.class);

        CheckSimpleResponse("initialized", sessionId, initResponse);

        var simulateMessageContent = FileUtils.readFileToByteArray(simulationJson);
        var simResponse =
                om.readValue(mockMvc.perform(post("/simulate/" + sessionId).content(simulateMessageContent).contentType(MediaType.APPLICATION_JSON)).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsString(), InitializeStatusModel.class);

        CheckSimpleResponse("simulation completed", sessionId, simResponse);

        var zippedResult =
                mockMvc.perform(get("/result/" + sessionId + "/zip")).andExpect(status().is(HttpStatus.OK.value())).andReturn().getResponse().getContentAsByteArray();
        Assertions.assertNotNull(zippedResult);

        var tempZipDir = Files.createTempDirectory("testZip"); // need a place to extract the zip to to ensure that the FMU was executed properly
        tempZipDir.toFile().deleteOnExit(); // means the folder and content will be deleted no matter if we crash or not
        var iStream = new ZipInputStream(new ByteArrayInputStream(zippedResult));
        var entries = new ArrayList<ZipEntry>();
        ZipEntry entry;

        while((entry = iStream.getNextEntry()) != null)
        {
            entries.add(entry);
            WriteZipContent(iStream, entry, tempZipDir.toFile());
        }
        iStream.closeEntry();
        iStream.close();

        var zipEntryNames = entries.stream().map(ZipEntry::getName).collect(Collectors.toList());
        assertThat(zipEntryNames)
                .containsExactlyInAnyOrder("initialize.json", "simulate.json", "spec.mabl", "outputs.csv", "spec.runtime.json", "equations.log");
        CheckZipFileContent(tempZipDir.toFile());

        mockMvc.perform(get("/destroy/" + sessionId)).andExpect(status().is(HttpStatus.OK.value()));
    }

    private void CheckZipFileContent(File extractedPath) throws IOException
    {
        // TODO: Possibly look into a nicer way of doing this

        for(File f : extractedPath.listFiles())
        {
            if(f.getName().equals("outputs.csv"))
            {
                var f1 = new HashSet<>(FileUtils.readLines(f));
                var f2 = new HashSet<>(FileUtils.readLines(expectedOutputCSV));

                Assertions.assertEquals(f2.size(), f1.size(), "Comparing results files");
            }
        }
    }

    private void WriteZipContent(ZipInputStream zipInputStream, ZipEntry entry, File unzipPath) throws Exception
    {
        var newFile = new File(unzipPath, entry.getName());
        newFile.deleteOnExit();

        var buffer = new byte[1024];
        var fos = new FileOutputStream(newFile);
        int len;
        while((len = zipInputStream.read(buffer)) > 0)
            fos.write(buffer, 0, len);
        fos.close();
    }

    private void CheckSimpleResponse(String expectedStatus, String sessionId, StatusModel reponse)
    {
        Assertions.assertEquals(expectedStatus.toLowerCase(), reponse.status.toLowerCase(), "Status Response");
        // check we arnt getting confused between the threads so check we always have the correct session id
        Assertions.assertEquals(sessionId, reponse.sessionId, "Session ID");
    }
}
