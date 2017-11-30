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
package org.intocps.orchestration.coe;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import fi.iki.elonen.NanoHTTPD;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.orchestration.coe.Api.ApiTest;
import org.intocps.orchestration.coe.httpserver.RequestHandler;
import org.intocps.orchestration.coe.httpserver.RequestProcessors;
import org.intocps.orchestration.coe.httpserver.Response;
import org.intocps.orchestration.coe.json.InitializationStatusJson;
import org.intocps.orchestration.coe.json.ProdSessionLogicFactory;
import org.intocps.orchestration.coe.json.StartMsgJson;
import org.intocps.orchestration.coe.json.StatusMsgJson;
import org.intocps.orchestration.coe.httpserver.SessionController;
import org.junit.Assert;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class BasicTest extends CoeBaseTest
{

	SessionController sessionController = new SessionController(new ProdSessionLogicFactory());
	RequestProcessors requestProcessors2 = new RequestProcessors(sessionController);
	RequestHandler RequestHandler = new RequestHandler(sessionController, requestProcessors2);
	NanoHTTPD.IHTTPSession ihttpSessionMock = mock(NanoHTTPD.IHTTPSession.class);
	final ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally

	protected void handleInitializationError(String initializeResponseData)
	{
		System.err.println(initializeResponseData);
		Assert.fail(initializeResponseData);
	}

	@SuppressWarnings("rawtypes") protected void test(String configPath,
			double startTime, double endTime)
			throws IOException, NanoHTTPD.ResponseException
	{
		InputStream resourceAsStream = this.getClass().getResourceAsStream(configPath);
		Assert.assertNotNull(
				"config must not be null: " + configPath, resourceAsStream);
		String configurationData = IOUtils.toString(resourceAsStream);

//output location
		File outputDir = new File(("target"
				+ configPath).replace('/', File.separatorChar)).getParentFile();
		outputDir.mkdirs();

		//plot script
		String plotPy = configPath.substring(0, configPath.lastIndexOf('/'))
				+ "/plot.py";
		InputStream plotStream = this.getClass().getResourceAsStream(plotPy);

		//compare result
		String parent =
				configPath.substring(0, configPath.lastIndexOf('/') + 1)
						+ "result.csv";

		InputStream expectedResult = this.getClass().getResourceAsStream(parent);

		testInternal(configurationData,startTime,endTime,outputDir,expectedResult,plotStream);
	}

	@SuppressWarnings("rawtypes") protected void testExternalConfig(String configPath,
			double startTime, double endTime)
			throws IOException, NanoHTTPD.ResponseException
	{
		InputStream resourceAsStream = new FileInputStream(configPath);
		Assert.assertNotNull(
				"config must not be null: " + configPath, resourceAsStream);
		String configurationData = IOUtils.toString(resourceAsStream);

		File output = new File(configPath).getParentFile();

		testInternal(configurationData,startTime,endTime,output,null,null);
	}

	@SuppressWarnings("rawtypes") protected void testInternal(
			String configurationData, double startTime, double endTime,
			File outputDir, InputStream expectedResult, InputStream plotStream)
			throws IOException, NanoHTTPD.ResponseException
	{


		//Create session
		NanoHTTPD.Response response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/createSession");
		String sessionId = ApiTest.extractSessionId(IOUtils.toString(response.getData()));
		try
		{
			//Initialize
			response = Utilities.runAny(RequestHandler, ihttpSessionMock, configurationData,
					"/initialize/" + sessionId);
			String initializeResponseData = IOUtils.toString(response.getData());
			IOUtils.closeQuietly(response);
			if (Response.MIME_PLAINTEXT.equals(response.getMimeType()))
			{
				handleInitializationError(initializeResponseData);
				return;
			}

			InitializationStatusJson initializeResponseJson = (InitializationStatusJson) ((List) mapper.readValue(initializeResponseData, new TypeReference<List<InitializationStatusJson>>()
			{
			})).get(0);

			System.out.println(String.format("Status: '%s' Session: '%s'", initializeResponseJson.status, initializeResponseJson.sessionId));

			StartMsgJson simulateMsg = new StartMsgJson();

			simulateMsg.startTime = startTime;
			simulateMsg.endTime = endTime;
			simulateMsg.logLevels = new HashMap<>();
			String simulateMsgJson = mapper.writeValueAsString(simulateMsg);

			final long startTime01 = System.currentTimeMillis();
			response = Utilities.runAny(RequestHandler, ihttpSessionMock, simulateMsgJson,
					"/simulate/" + sessionId);
			final long endTime01 = System.currentTimeMillis();

			System.out.println(
					"Total execution time: " + (endTime01 - startTime01)
							+ " ms");

			String simulateResponseData = IOUtils.toString(response.getData());
			IOUtils.closeQuietly(response);
			if (NanoHTTPD.Response.Status.OK != response.getStatus())
			{
				handleSimulateError(simulateResponseData, response);
				return;
			}

			StatusMsgJson simulateResponseMsg = mapper.readValue(simulateResponseData, new TypeReference<StatusMsgJson>()
			{
			});
			System.out.println(String.format("Status: '%s' Session: '%s'", simulateResponseMsg.status, simulateResponseMsg.sessionId));
			NanoHTTPD.Response resultResponse = Utilities.runAny(RequestHandler, ihttpSessionMock,
					"/result/" + sessionId);

//			File resultFolder0 = new File(("target"
//					+ configLocation).replace('/', File.separatorChar));
//			resultFolder0.getParentFile().mkdirs();

			String resultData = IOUtils.toString(resultResponse.getData());
			IOUtils.closeQuietly(resultResponse);

			File resultFile0 = new File(outputDir, "result.csv");
			FileUtils.writeStringToFile(resultFile0, resultData);

			response = Utilities.runAny(RequestHandler, ihttpSessionMock, null,
					"/destroy/" + sessionId);
			IOUtils.closeQuietly(response);
			Assert.assertEquals("Error: in destroy", NanoHTTPD.Response.Status.OK, response.getStatus());



			tryPlot(plotStream, resultFile0, endTime);

			assertResultEqualsDiff(resultData, expectedResult);
		} finally
		{
			try
			{
				Utilities.runAny(RequestHandler, ihttpSessionMock, null,
						"/destroy/" + sessionId);
			} catch (Exception e)
			{
			}
		}

	}

	public static void assertResultEqualsDiff(String resultData,
			InputStream resultInputStream0)
	{
		if (resultInputStream0 != null)
		{
			List<String> original = fileToLines(new ByteArrayInputStream(resultData.getBytes(StandardCharsets.UTF_8)));
			List<String> revised = fileToLines(resultInputStream0);

			// Compute diff. Get the Patch object. Patch is the container for computed deltas.
			Patch patch = DiffUtils.diff(original, revised);

			for (Delta delta : patch.getDeltas())
			{
				System.err.println(delta);
				Assert.fail("Expected result and actual differ: " + delta);
			}

		}
	}

	protected void handleSimulateError(String simulateResponseData,
			NanoHTTPD.Response response)
	{
		Assert.assertEquals("Error: "
				+ simulateResponseData, NanoHTTPD.Response.Status.OK, response.getStatus());
	}

	private static List<String> fileToLines(InputStream filename)
	{
		List<String> lines = new LinkedList<String>();
		String line = "";
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(filename));
			while ((line = in.readLine()) != null)
			{
				lines.add(line);
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return lines;
	}

	private void tryPlot(InputStream plotStream, File resultFile, double endTime)
	{
		try
		{
			if (plotStream == null)
			{
				System.out.println("No plot script found");
				return;
			}
			String data = IOUtils.toString(plotStream);

			File plotPyFile = new File(resultFile.getParentFile(), "plot.py");
			FileUtils.writeStringToFile(plotPyFile, data);

			Runtime.getRuntime().exec("python plot.py result.csv "
					+ endTime, null, resultFile.getParentFile());

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
