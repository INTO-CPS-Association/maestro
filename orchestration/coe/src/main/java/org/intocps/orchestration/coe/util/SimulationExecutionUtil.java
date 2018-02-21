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
package org.intocps.orchestration.coe.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.orchestration.coe.httpserver.RequestHandler;
import org.intocps.orchestration.coe.httpserver.RequestProcessors;
import org.intocps.orchestration.coe.httpserver.Response;
import org.intocps.orchestration.coe.httpserver.SessionController;
import org.intocps.orchestration.coe.json.InitializationStatusJson;
import org.intocps.orchestration.coe.json.ProdSessionLogicFactory;
import org.intocps.orchestration.coe.json.StartMsgJson;
import org.intocps.orchestration.coe.json.StatusMsgJson;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by kel on 19/12/16.
 */
public class SimulationExecutionUtil
{
	public static String extractSessionId(String data)
	{
		ObjectMapper mapper = new ObjectMapper();

		try
		{
			Map d = mapper.readValue(data, Map.class);
			return (String)d.get("sessionId");
		} catch (IOException e)
		{
			return null;
		}

	}

	SessionController sessionController = new SessionController(new ProdSessionLogicFactory());
	RequestProcessors requestProcessors2 = new RequestProcessors(sessionController);
	org.intocps.orchestration.coe.httpserver.RequestHandler RequestHandler = new RequestHandler(sessionController, requestProcessors2);
	final ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
	final boolean verbose;

	public SimulationExecutionUtil(boolean verbose)
	{
		this.verbose = verbose;
	}

	private static class S implements NanoHTTPD.IHTTPSession
	{

		private final String data;

		private final String uri;

		public S(String uri, String data)
		{
			this.uri = uri;
			this.data = data;
		}

		@Override public void execute() throws IOException
		{

		}

		@Override public NanoHTTPD.CookieHandler getCookies()
		{
			return null;
		}

		@Override public Map<String, String> getHeaders()
		{
			return null;
		}

		@Override public InputStream getInputStream()
		{
			return null;
		}

		@Override public NanoHTTPD.Method getMethod()
		{
			return null;
		}

		@Override public Map<String, String> getParms()
		{
			return null;
		}

		@Override public String getQueryParameterString()
		{
			return null;
		}

		@Override public String getUri()
		{
			return this.uri;
		}

		@Override public void parseBody(Map<String, String> files)
				throws IOException, NanoHTTPD.ResponseException
		{
			if (data != null)
			{
				files.put("postData", data);
			}
		}

		@Override public String getRemoteIpAddress()
		{
			return null;
		}

		@Override public String getRemoteHostName()
		{
			return null;
		}
	}

	public static NanoHTTPD.Response callHttpAny(RequestHandler RequestHandler,
			String uri, final String data)
			throws IOException, NanoHTTPD.ResponseException
	{

		return RequestHandler.handleRequest(new S(uri, data));
	}

	protected void handleInitializationError(String initializeResponseData)
	{
		System.err.println(initializeResponseData);
	}

	@SuppressWarnings("rawtypes") public void run(String configPath,
			double startTime, double endTime, File outputFile)
			throws IOException, NanoHTTPD.ResponseException
	{
		InputStream resourceAsStream = this.getClass().getResourceAsStream(configPath);

		if (resourceAsStream == null)
		{
			handleConfigNotFound(configPath);
			return;
		}
		String configurationData = IOUtils.toString(resourceAsStream);
		this.run(configPath, configurationData, startTime, endTime, outputFile);
	}

	@SuppressWarnings("rawtypes") public void run(String configPath,
			String configurationData, double startTime, double endTime,
			File outputFile) throws IOException, NanoHTTPD.ResponseException
	{

		//Create session
		NanoHTTPD.Response response = callHttpAny(RequestHandler, "/createSession", null);
		String sessionId = extractSessionId(IOUtils.toString(response.getData()));
		try
		{
			//Initialize
			response = callHttpAny(RequestHandler,
					"/initialize/" + sessionId, configurationData);

			String initializeResponseData = IOUtils.toString(response.getData());
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
			response = callHttpAny(RequestHandler,
					"/simulate/" + sessionId, simulateMsgJson);
			final long endTime01 = System.currentTimeMillis();

			System.out.println(
					"Total execution time: " + (endTime01 - startTime01)
							+ " ms");

			String simulateResponseData = IOUtils.toString(response.getData());

			if (NanoHTTPD.Response.Status.OK != response.getStatus())
			{
				handleSimulateError(simulateResponseData, response);
				return;
			}

			StatusMsgJson simulateResponseMsg = mapper.readValue(simulateResponseData, new TypeReference<StatusMsgJson>()
			{
			});
			System.out.println(String.format("Status: '%s' Session: '%s'", simulateResponseMsg.status, simulateResponseMsg.sessionId));

			if (verbose)
			{
				NanoHTTPD.Response resultResponse = callHttpAny(RequestHandler,
						"/result/" + sessionId, null);

				IOUtils.copy(resultResponse.getData(), System.out);
			}
			if (outputFile != null)
			{
				NanoHTTPD.Response resultResponse = callHttpAny(RequestHandler,
						"/result/" + sessionId, null);
				System.out.println(outputFile.getAbsolutePath());
				FileOutputStream output = new FileOutputStream(outputFile);
				try
				{
					IOUtils.copy(resultResponse.getData(), output);
				} finally
				{
					IOUtils.closeQuietly(resultResponse.getData());
					IOUtils.closeQuietly(output);
				}
			}

			response = callHttpAny(RequestHandler,
					"/destroy/" + sessionId, null);

			if (response.getStatus() != NanoHTTPD.Response.Status.OK)
			{
				handleDestroyError(response);
				return;
			}

			//			String parent =
			//					configPath.substring(0, configPath.lastIndexOf('/') + 1)
			//							+ "result.csv";
			//
			//			InputStream resultInputStream0 = this.getClass().getResourceAsStream(parent);
			//
			//			tryPlot(configPath, resultFile0, endTime);
			//
			//			if (resultInputStream0 != null)
			//			{
			//				List<String> original = fileToLines(new ByteArrayInputStream(resultData.getBytes(StandardCharsets.UTF_8)));
			//				List<String> revised = fileToLines(resultInputStream0);
			//
			//				// Compute diff. Get the Patch object. Patch is the container for computed deltas.
			//				compareResults(original, revised);
			//
			//			}
		} finally
		{
			try
			{
				callHttpAny(RequestHandler, "/destroy/" + sessionId, null);
			} catch (Exception e)
			{
			}
		}

	}

	protected void handleDestroyError(NanoHTTPD.Response response)
	{
	}

	private void compareResults(List<String> original, List<String> revised)
	{
	}

	private void handleConfigNotFound(String configPath)
	{
	}

	protected void handleSimulateError(String simulateResponseData,
			NanoHTTPD.Response response)
	{
		System.err.println(
				"Error: " + simulateResponseData + response.getStatus());
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

	private void tryPlot(String configPath, File resultFile, double endTime)
	{
		try
		{
			String plotPy = configPath.substring(0, configPath.lastIndexOf('/'))
					+ "/plot.py";
			InputStream plotStream = this.getClass().getResourceAsStream(plotPy);
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
