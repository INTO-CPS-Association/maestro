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
package org.intocps.orchestration.coe.Api;

import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.IOUtils;
import org.intocps.orchestration.coe.HttpUtil;
import org.intocps.orchestration.coe.httpserver.Response;
import org.intocps.orchestration.coe.httpserver.SessionController;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by ctha on 17-03-2016.
 */
public class RequestProcessorsTests {

    @Test
    public void processCreateSession_ReturnsReponseWithSession() throws IOException {
        SessionController sessionControllerMock = mock(SessionController.class);
        String session = UUID.randomUUID().toString();
        when(sessionControllerMock.createNewSession()).thenReturn(session);

        org.intocps.orchestration.coe.httpserver.RequestProcessors rps = new org.intocps.orchestration.coe.httpserver.RequestProcessors(sessionControllerMock);
        NanoHTTPD.Response actualResponse = rps.processCreateSession();

        Assert.assertEquals(NanoHTTPD.Response.Status.OK, actualResponse.getStatus());
        Assert.assertEquals(Response.MIME_JSON,actualResponse.getMimeType());
        Assert.assertEquals(String.format("{\"sessionId\":\""+session+"\"}", session), IOUtils.toString(actualResponse.getData()));
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    /**
     * The post request has been creating by adding the following to NanoHTTPDJson.java:
     *  				OutputStream out = new FileOutputStream("PostRequest_multiPartFormData_Filesf1Andf2");
     *					IOUtils.copy(bufferedInputStream,out);
     *					bufferedInputStream.close();
     *					out.close();
     *	And running the following CURL from resource\fileUploadTest
     *                  curl -v -i -F file1=@WaterTanks1.fmu -F file2=@WaterTanks2.fmu localhost:8082/upload
     */
    public void processFileUpload_InvokesCorrectStoreFileContent_Returns200OK() throws IOException, URISyntaxException {
        FileInputStream expectedFile = null;
        FileInputStream actualFile = null;
        InputStream postRequestData = null;

        String session = "1";

        File fileUploadTestFolder = new File(getClass().getClassLoader().getResource("fileUploadTest").toURI());
        String fileName1 = "WaterTanks1.fmu";
        String fileName2 = "WaterTanks2.fmu";
        List<File> expectedFiles = new Vector<>();
        expectedFiles.add(new File(fileUploadTestFolder, fileName1));
        expectedFiles.add(new File(fileUploadTestFolder, fileName2));

        //Mock setup
        SessionController sessionControllerMock = mock(SessionController.class);
        when(sessionControllerMock.getSessionRootDir(session)).thenReturn(tempFolder.getRoot());

        //Create request
        File multiPartRequest = new File(fileUploadTestFolder, "PostRequest_multiPartFormData_Filesf1Andf2");
        String boundary = "------------------------d37ea78d012b81b9";
        Properties header = HttpUtil.createMultiPartFormDataHeader(multiPartRequest, boundary);
        try {
            postRequestData = new BufferedInputStream(new FileInputStream(multiPartRequest));

            //Act
            org.intocps.orchestration.coe.httpserver.RequestProcessors rps = new org.intocps.orchestration.coe.httpserver.RequestProcessors(sessionControllerMock);
            rps.processFileUpload(session, new HashMap(header), postRequestData);
        }
        finally {
            if (postRequestData != null)
                postRequestData.close();
        }

        //Verify the folder contains the expected files
        List<File> actualFiles = new Vector<>();
        actualFiles.add(new File(tempFolder.getRoot() + File.separator + fileName1));
        actualFiles.add(new File(tempFolder.getRoot() + File.separator + fileName2));

        for (int i = 0; i < expectedFiles.size(); i++) {
            try {
                expectedFile = new FileInputStream(expectedFiles.get(i));
                actualFile = new FileInputStream(actualFiles.get(i));
                Assert.assertArrayEquals(IOUtils.toByteArray(expectedFile), IOUtils.toByteArray(actualFile));
            } finally {
                expectedFile.close();
                actualFile.close();
            }
        }

    }




}
