/*
* This file is part of the INTO-CPS toolchain.
*
* Copyright (c) 2017-CurrentYear, INTO-CPS Association,
* c/o Professor Peter Gorm Larsen, Department of Engineering
* Finlandsgade 22, 8200 Aarhus N.
*
* All rights reserved.
*
* THIS PROGRAM IS PROVIDED UNDER THE TERMS OF GPL VERSION 3 LICENSE OR
* THIS INTO-CPS ASSOCIATION PUBLIC LICENSE VERSION 1.0.
* ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS PROGRAM CONSTITUTES
* RECIPIENT'S ACCEPTANCE OF THE OSMC PUBLIC LICENSE OR THE GPL 
* VERSION 3, ACCORDING TO RECIPIENTS CHOICE.
*
* The INTO-CPS toolchain  and the INTO-CPS Association Public License 
* are obtained from the INTO-CPS Association, either from the above address,
* from the URLs: http://www.into-cps.org, and in the INTO-CPS toolchain distribution.
* GNU version 3 is obtained from: http://www.gnu.org/copyleft/gpl.html.
*
* This program is distributed WITHOUT ANY WARRANTY; without
* even the implied warranty of  MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE, EXCEPT AS EXPRESSLY SET FORTH IN THE
* BY RECIPIENT SELECTED SUBSIDIARY LICENSE CONDITIONS OF
* THE INTO-CPS ASSOCIATION.
*
* See the full INTO-CPS Association Public License conditions for more details.
*/

/*
* Author:
*		Kenneth Lausdahl
*		Casper Thule
*/
package org.intocps.orchestration.coe.JsonTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.orchestration.coe.json.InitializationMsgJson;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.io.IOUtil;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by ctha on 15-03-2016.
 */
public class InitializationMsgTests {

    public InitializationMsgJson mappedJsonData;

    @Before
    public void initializeMappedJsonData() throws IOException {
        String jsonData = "{\n" +
                "\t\"fmus\":{\n" +
                "\t\t\"{x1}\" : \"../fmus/tankcontroller.fmu\",\n" +
                "\t\t\"{x2}\" : \"../fmus/tank.fmu\"\n" +
                "\t},\n" +
                "\t\"connections\":{\n" +
                "\t\t\"{x1}.crtl.valve\":[\"{x2}.tank.valve\"],\n" +
                "\t\t\"{x2}.tank.level\":[\"{x1}.crtl.level\"]\n" +
                "\t},\n" +
                "\t\"parameters\":{\n" +
                "        \"{x1}.crtl.maxLevel\":8,\n" +
                "        \"{x1}.crtl.minLevel\":2\n" +
                "    },\n" +
                "\t\"algorithm\":{\n" +
                "\t\t\"type\":\"fixed-step\",\n" +
                "\t\t\"size\":0.1\n" +
                "\t}\n" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        mappedJsonData = mapper.readValue(jsonData, InitializationMsgJson.class);
    }

    @Test
    public void fmus_validJsonTwoFmus_mappedCorrectly() throws Exception {
        Map<String, String> expectedFmus = new HashMap<>();
        expectedFmus.put("{x1}", "../fmus/tankcontroller.fmu");
        expectedFmus.put("{x2}", "../fmus/tank.fmu");
        Assert.assertEquals(expectedFmus, mappedJsonData.fmus);
    }

    @Test
    public void getFmuFiles_validJsonTwoFmus_ReturnsFiles() throws Exception {
        Map<String, URI> fmuFiles = new HashMap<>();
        fmuFiles.put("{x1}", new URI("../fmus/tankcontroller.fmu"));
        fmuFiles.put("{x2}", new URI("../fmus/tank.fmu"));
        Assert.assertEquals(fmuFiles, mappedJsonData.getFmuFiles());
    }

    @Test
    public void connections_validJson2Times1To1Connection_mappedCorrectly() throws Exception {
        Map<String, List<String>> expectedConnections = new HashMap<>();
        List<String> expectedInputX1Valve = new Vector<>();
        expectedInputX1Valve.add("{x2}.tank.valve");
        expectedConnections.put("{x1}.crtl.valve", expectedInputX1Valve);
        List<String> expectedInputX2Level = new Vector<>();
        expectedInputX2Level.add("{x1}.crtl.level");
        expectedConnections.put("{x2}.tank.level", expectedInputX2Level);
        Assert.assertEquals(expectedConnections, mappedJsonData.connections);
    }

    @Test
    public void parameters_validJson2Parameters_mappedCorrectly() throws Exception {
        Map<String, Object> expectedParameters = new HashMap<>();
        expectedParameters.put("{x1}.crtl.maxLevel", 8);
        expectedParameters.put("{x1}.crtl.minLevel", 2);
        Assert.assertEquals(expectedParameters, mappedJsonData.parameters);
    }

    @Test
    public void algorithm_validJsonFixedStep01_mappedCorrectly() throws Exception {
        Map<String, Object> expectedAlgorithm = new HashMap<>();
        expectedAlgorithm.put("type", "fixed-step");
        expectedAlgorithm.put("size", 0.1);
        Assert.assertEquals(expectedAlgorithm, mappedJsonData.algorithm);
    }

    @Test
    public void livestreaming_validJson_mappedCorrectly(
    ) throws Exception {
        String jsonData = FileUtils.readFileToString(new File("src/test/resources/initializationMsgs.json"));
        ObjectMapper mapper = new ObjectMapper();
        mappedJsonData = mapper.readValue(jsonData, InitializationMsgJson.class);
        Map<String, List<String>> expectedLiveStreaming = new HashMap<>();
        List<String> expectedOutputVariables = new ArrayList<>();
        expectedOutputVariables.add("output");
        expectedLiveStreaming.put("{sine}.sine", expectedOutputVariables);
        Assert.assertEquals(expectedLiveStreaming, mappedJsonData.livestream);
    }

    @Test
    public void logVariables_validJson_mappedCorrectly() throws Exception {
        String jsonData = FileUtils.readFileToString(new File("src/test/resources/initializationMsgs.json"));
        ObjectMapper mapper = new ObjectMapper();
        mappedJsonData = mapper.readValue(jsonData, InitializationMsgJson.class);
        Map<String, List<String>> expectedMap = new HashMap<>();
        List<String> expectedOutputVariables = new ArrayList<>();
        expectedOutputVariables.add("output");
        expectedMap.put("{integrate}.int1", expectedOutputVariables);
        Assert.assertEquals(expectedMap, mappedJsonData.logVariables);
    }

}
