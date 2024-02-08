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

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.IOUtils;
import org.intocps.orchestration.coe.HttpUtil;
import org.intocps.orchestration.coe.httpserver.*;
import org.intocps.orchestration.coe.json.StatusMsgJson;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by ctha on 17-03-2016.
 */
public class CoeRequestHandlerTests
{

	final ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally

	@Test public void Status_SessionExistsAndSessionIdPassed_Returns200OK()
			throws IOException, NanoHTTPD.ResponseException
	{
		StatusMsgJson msg = new StatusMsgJson();
		msg.lastExecTime = 5L;
		msg.sessionId = "1";
		msg.status = "initialized";
		NanoHTTPD.Response expectedResponse = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, Response.MIME_JSON, mapper.writeValueAsString(msg));

		SessionController sessionControllerMock = mock(SessionController.class);
		RequestProcessors requestProcessorMock = mock(RequestProcessors.class);
		Mockito.when(sessionControllerMock.containsSession("1")).thenReturn(true);
		Mockito.when(sessionControllerMock.getStatus("1")).thenReturn(msg);
		NanoHTTPD.IHTTPSession ihttpSessionMock = mock(NanoHTTPD.IHTTPSession.class);
		Mockito.when(ihttpSessionMock.getUri()).thenReturn("/status/1");

		RequestHandler handler = new RequestHandler(sessionControllerMock, requestProcessorMock);
		NanoHTTPD.Response actualResponse = handler.handleRequest(ihttpSessionMock);

		verify(sessionControllerMock, times(1)).containsSession("1");
		verify(sessionControllerMock, times(1)).getStatus("1");
		Assert.assertEquals(expectedResponse.getStatus(), actualResponse.getStatus());
		Assert.assertEquals(IOUtils.toString(expectedResponse.getData()), IOUtils.toString(actualResponse.getData()));
		Assert.assertEquals(expectedResponse.getMimeType(), actualResponse.getMimeType());
	}

	@Test public void Status_SessionExistsNoSessionId_Returns200OK()
			throws IOException, NanoHTTPD.ResponseException
	{
		StatusMsgJson msg = new StatusMsgJson();
		msg.lastExecTime = 5L;
		msg.sessionId = "1";
		msg.status = "initialized";
		List<StatusMsgJson> msgs = new Vector<>();
		msgs.add(msg);
		NanoHTTPD.Response expectedResponse = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, Response.MIME_JSON, mapper.writeValueAsString(msgs));

		SessionController sessionControllerMock = mock(SessionController.class);
		RequestProcessors requestProcessorMock = mock(RequestProcessors.class);
		Mockito.when(sessionControllerMock.getStatus()).thenReturn(msgs);
		NanoHTTPD.IHTTPSession ihttpSessionMock = mock(NanoHTTPD.IHTTPSession.class);
		Mockito.when(ihttpSessionMock.getUri()).thenReturn("/status");
		RequestHandler handler = new RequestHandler(sessionControllerMock, requestProcessorMock);
		NanoHTTPD.Response actualResponse = handler.handleRequest(ihttpSessionMock);

		verify(sessionControllerMock, times(1)).getStatus();
		Assert.assertEquals(expectedResponse.getStatus(), actualResponse.getStatus());
		Assert.assertEquals(IOUtils.toString(expectedResponse.getData()), IOUtils.toString(actualResponse.getData()));
		Assert.assertEquals(expectedResponse.getMimeType(), actualResponse.getMimeType());
	}

	@Test public void CreateSession_Returns200OK()
			throws IOException, NanoHTTPD.ResponseException
	{
		SessionController sessionControllerMock = mock(SessionController.class);
		RequestProcessors requestProcessorMock = mock(RequestProcessors.class);
		NanoHTTPD.Response expectedResponse = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, Response.MIME_JSON, String.format("{\"sessionId\":\"%d\"}", 1));
		Mockito.when(requestProcessorMock.processCreateSession()).thenReturn(expectedResponse);
		RequestHandler handler = new RequestHandler(sessionControllerMock, requestProcessorMock);

		NanoHTTPD.IHTTPSession ihttpSessionMock = mock(NanoHTTPD.IHTTPSession.class);
		Mockito.when(ihttpSessionMock.getUri()).thenReturn("/createSession");

		NanoHTTPD.Response actualResponse = handler.handleRequest(ihttpSessionMock);

		verify(requestProcessorMock, times(1)).processCreateSession();
		Assert.assertEquals(expectedResponse, actualResponse);
	}

	@Test public void Upload_SessionsExists_Returns200OK()
			throws IOException, NanoHTTPD.ResponseException
	{
		NanoHTTPD.Response expectedResponse = ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.OK, "Files uploaded..");
		SessionController sessionControllerMock = mock(SessionController.class);
		RequestProcessors requestProcessorMock = mock(RequestProcessors.class);
		NanoHTTPD.IHTTPSession ihttpSessionMock = mock(NanoHTTPD.IHTTPSession.class);
		Map<String, String> returnMap = new HashMap<>();
		Mockito.when(ihttpSessionMock.getUri()).thenReturn("/upload/1");
		Mockito.when(ihttpSessionMock.getHeaders()).thenReturn(returnMap);
		InputStream inputStream = HttpUtil.toInput("");
		Mockito.when(ihttpSessionMock.getInputStream()).thenReturn(inputStream);

		Mockito.when(requestProcessorMock.processFileUpload("1", returnMap, inputStream)).thenReturn(expectedResponse);
		Mockito.when(sessionControllerMock.containsSession("1")).thenReturn(true);
		RequestHandler handler = new RequestHandler(sessionControllerMock, requestProcessorMock);

		NanoHTTPD.Response actualResponse = handler.handleRequest(ihttpSessionMock);

		verify(requestProcessorMock, times(1)).processFileUpload(eq("1"), eq(returnMap), eq(inputStream));
		verify(sessionControllerMock, times(1)).containsSession("1");
		Assert.assertEquals(expectedResponse, actualResponse);
	}

	@Test public void Initialize_SessionsExists_Returns200OK()
			throws IOException, NanoHTTPD.ResponseException
	{
		final String jsonData = "{\"dummy\":\"test\"}";
		NanoHTTPD.Response expectedResponse = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, Response.MIME_JSON, jsonData);

		NanoHTTPD.IHTTPSession ihttpSessionMock = mock(NanoHTTPD.IHTTPSession.class);
		Map<String, String> returnMap = new HashMap<>();
		Mockito.when(ihttpSessionMock.getUri()).thenReturn("/initialize/1");
		Mockito.when(ihttpSessionMock.getHeaders()).thenReturn(returnMap);
		doAnswer(new Answer<Void>()
		{
			@Override public Void answer(InvocationOnMock invocationOnMock)
					throws Throwable
			{
				Map<String, String> files = (Map<String, String>) invocationOnMock.getArguments()[0];
				files.put("postData", jsonData);
				return null;
			}
		}).when(ihttpSessionMock).parseBody(anyMap());

		RequestProcessors requestProcessorMock = mock(RequestProcessors.class);
		Mockito.when(requestProcessorMock.processInitialize("1", jsonData)).thenReturn(expectedResponse);

		SessionController sessionControllerMock = mock(SessionController.class);
		Mockito.when(sessionControllerMock.containsSession("1")).thenReturn(true);

		RequestHandler handler = new RequestHandler(sessionControllerMock, requestProcessorMock);
		NanoHTTPD.Response actualResponse = handler.handleRequest(ihttpSessionMock);

		verify(requestProcessorMock, times(1)).processInitialize(eq("1"), eq(jsonData));
		verify(sessionControllerMock, times(1)).containsSession(eq("1"));
		verify(ihttpSessionMock, times(1)).parseBody(anyMap());
		Assert.assertEquals(expectedResponse, actualResponse);
	}

	@Test public void Simulate_SessionsExists_Returns200OK()
			throws IOException, NanoHTTPD.ResponseException
	{
		final String jsonData = "{\"dummy\":\"test\"}";
		NanoHTTPD.Response expectedResponse = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, Response.MIME_JSON, jsonData);

		NanoHTTPD.IHTTPSession ihttpSessionMock = mock(NanoHTTPD.IHTTPSession.class);
		Map<String, String> returnMap = new HashMap<>();
		Mockito.when(ihttpSessionMock.getUri()).thenReturn("/simulate/1");
		Mockito.when(ihttpSessionMock.getHeaders()).thenReturn(returnMap);
		doAnswer(new Answer<Void>()
		{
			@Override public Void answer(InvocationOnMock invocationOnMock)
					throws Throwable
			{
				Map<String, String> files = (Map<String, String>) invocationOnMock.getArguments()[0];
				files.put("postData", jsonData);
				return null;
			}
		}).when(ihttpSessionMock).parseBody(anyMap());

		SessionController sessionControllerMock = mock(SessionController.class);
		Mockito.when(sessionControllerMock.containsSession("1")).thenReturn(true);

		RequestProcessors requestProcessorMock = mock(RequestProcessors.class);
		Mockito.when(requestProcessorMock.processSimulate("1", jsonData, false)).thenReturn(expectedResponse);

		RequestHandler handler = new RequestHandler(sessionControllerMock, requestProcessorMock);
		NanoHTTPD.Response actualResponse = handler.handleRequest(ihttpSessionMock);

		verify(requestProcessorMock, times(1)).processSimulate(eq("1"), eq(jsonData), eq(false));
		verify(sessionControllerMock, times(1)).containsSession("1");
		Assert.assertEquals(expectedResponse, actualResponse);
	}

	@Test public void Result_ZipTypeSessionExists_Returns200OK()
			throws IOException, NanoHTTPD.ResponseException
	{
		NanoHTTPD.Response expectedResponse = NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, Response.MIME_ZIP, new ByteArrayInputStream("Fake zip".getBytes()));

		NanoHTTPD.IHTTPSession ihttpSessionMock = mock(NanoHTTPD.IHTTPSession.class);
		Mockito.when(ihttpSessionMock.getUri()).thenReturn("/result/1/zip");

		SessionController sessionControllerMock = mock(SessionController.class);
		Mockito.when(sessionControllerMock.containsSession("1")).thenReturn(true);

		RequestProcessors requestProcessorMock = mock(RequestProcessors.class);
		Mockito.when(requestProcessorMock.processResult("1", true)).thenReturn(expectedResponse);

		RequestHandler handler = new RequestHandler(sessionControllerMock, requestProcessorMock);
		NanoHTTPD.Response actualResponse = handler.handleRequest(ihttpSessionMock);

		verify(requestProcessorMock, times(1)).processResult(eq("1"), eq(true));
		verify(sessionControllerMock, times(1)).containsSession("1");
		Assert.assertEquals(expectedResponse, actualResponse);
	}

	@Test public void Result_PlainTypeSessionExists_Returns200OK()
			throws IOException, NanoHTTPD.ResponseException
	{
		NanoHTTPD.Response expectedResponse = ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.OK, "Fake zip");

		NanoHTTPD.IHTTPSession ihttpSessionMock = mock(NanoHTTPD.IHTTPSession.class);
		Mockito.when(ihttpSessionMock.getUri()).thenReturn("/result/1/plain");

		SessionController sessionControllerMock = mock(SessionController.class);
		Mockito.when(sessionControllerMock.containsSession("1")).thenReturn(true);

		RequestProcessors requestProcessorMock = mock(RequestProcessors.class);
		Mockito.when(requestProcessorMock.processResult("1", false)).thenReturn(expectedResponse);

		RequestHandler handler = new RequestHandler(sessionControllerMock, requestProcessorMock);
		NanoHTTPD.Response actualResponse = handler.handleRequest(ihttpSessionMock);

		verify(requestProcessorMock, times(1)).processResult(eq("1"), eq(false));
		verify(sessionControllerMock, times(1)).containsSession("1");
		Assert.assertEquals(expectedResponse, actualResponse);
	}

	@Test public void Result_NoTypeSessionExists_Returns200OK()
			throws IOException, NanoHTTPD.ResponseException
	{

		NanoHTTPD.Response expectedResponse = ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.OK, "Fake zip");

		NanoHTTPD.IHTTPSession ihttpSessionMock = mock(NanoHTTPD.IHTTPSession.class);
		Mockito.when(ihttpSessionMock.getUri()).thenReturn("/result/1");

		SessionController sessionControllerMock = mock(SessionController.class);
		Mockito.when(sessionControllerMock.containsSession("1")).thenReturn(true);

		RequestProcessors requestProcessorMock = mock(RequestProcessors.class);
		Mockito.when(requestProcessorMock.processResult("1", false)).thenReturn(expectedResponse);

		RequestHandler handler = new RequestHandler(sessionControllerMock, requestProcessorMock);
		NanoHTTPD.Response actualResponse = handler.handleRequest(ihttpSessionMock);

		verify(requestProcessorMock, times(1)).processResult(eq("1"), eq(false));
		verify(sessionControllerMock, times(1)).containsSession("1");
		Assert.assertEquals(expectedResponse, actualResponse);
	}

	@Test public void Destroy_SessionExists_Returns200OK()
			throws IOException, NanoHTTPD.ResponseException
	{
		NanoHTTPD.Response expectedResponse = ProcessingUtils.newFixedLengthPlainResponse(NanoHTTPD.Response.Status.OK,
				"Session " + 1 + " destroyed");

		NanoHTTPD.IHTTPSession ihttpSessionMock = mock(NanoHTTPD.IHTTPSession.class);
		Mockito.when(ihttpSessionMock.getUri()).thenReturn("/destroy/1");

		SessionController sessionControllerMock = mock(SessionController.class);
		Mockito.when(sessionControllerMock.containsSession("1")).thenReturn(true);

		RequestProcessors requestProcessorMock = mock(RequestProcessors.class);
		Mockito.when(requestProcessorMock.processDestroy("1")).thenReturn(expectedResponse);

		RequestHandler handler = new RequestHandler(sessionControllerMock, requestProcessorMock);
		NanoHTTPD.Response actualResponse = handler.handleRequest(ihttpSessionMock);

		verify(requestProcessorMock, times(1)).processDestroy(eq("1"));
		verify(sessionControllerMock, times(1)).containsSession("1");
		Assert.assertEquals(expectedResponse, actualResponse);
	}
}
