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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.orchestration.coe.Utilities;
import org.intocps.orchestration.coe.config.CoeConfiguration;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.config.ModelParameter;
import org.intocps.orchestration.coe.cosim.CoSimStepSizeCalculator;
import org.intocps.orchestration.coe.httpserver.RequestHandler;
import org.intocps.orchestration.coe.httpserver.RequestProcessors;
import org.intocps.orchestration.coe.httpserver.SessionLogic;
import org.intocps.orchestration.coe.json.SessionLogicFactory;
import org.intocps.orchestration.coe.json.StartMsgJson;
import org.intocps.orchestration.coe.json.StatusMsgJson;
import org.intocps.orchestration.coe.httpserver.SessionController;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.scala.Coe;
import org.intocps.orchestration.coe.scala.CoeStatus;
import org.intocps.orchestration.coe.scala.LogVariablesContainer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by ctha on 23-02-2016.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(FileUtils.class)
public class ApiTest
{
	final static String DEFAULT_CONFIG_PATH = "/online-models/watertank-c/config.json";
	final static String WATERTANK_WITH_VARSTEP = "/watertank-c_varstep/config.json";
public final static	String guidRegex="[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

	private Coe coeMock;
	RequestHandler RequestHandler;
	final static ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
	private NanoHTTPD.IHTTPSession ihttpSessionMock;

	enum MimeType
	{
		json("application/json"),

		plain("text/plain"),

		html("text/html"),

		pdf("application/pdf"),

		zip("application/zip");

		MimeType(String value)
		{
			this.value = value;
		}

		public final String value;
	}

	@Before
	public void testSetup() throws IOException
	{
		// Mock the COE
		coeMock = mock(Coe.class);

		// Mock the Coe Factory
		SessionLogicFactory sessionLogicFactoryMock = mock(SessionLogicFactory.class);
		SessionLogic sessionLogicMock = mock(SessionLogic.class);
		when(sessionLogicFactoryMock.CreateSessionLogic(any(File.class))).thenReturn(sessionLogicMock);
		//when(sessionLogicFactoryMock.CreateCoe()).thenReturn(coeMock);
		when(sessionLogicMock.coe()).thenReturn(coeMock);
		// Mock "initialize" method
		List<ModelDescription.LogCategory> logCategories = new ArrayList<>();
		logCategories.add(new ModelDescription.LogCategory("LowError", "Lowest possible error"));
		Map<String, List<ModelDescription.LogCategory>> logCatMap = new HashMap<>();
		logCatMap.put("Error", logCategories);
		when(coeMock.initialize(anyMapOf(String.class,URI.class), anyListOf(ModelConnection.class), anyListOf(ModelParameter.class), any(CoSimStepSizeCalculator.class), anyLiveStreamMap2())).thenReturn(logCatMap);
		when(coeMock.getState()).thenReturn(CoeStatus.Initialized + "", CoeStatus.Finished
				+ "");

		when(coeMock.getConfiguration()).thenReturn(new CoeConfiguration());

		when(coeMock.getLastExecTime()).thenReturn(3L);
		// Create the request handler
		SessionController sessionController = new SessionController(sessionLogicFactoryMock);
		RequestProcessors requestProcessors = new RequestProcessors(sessionController);
		RequestHandler = new RequestHandler(sessionController, requestProcessors);

		ihttpSessionMock = mock(NanoHTTPD.IHTTPSession.class);

	}
	private static Map<ModelConnection.ModelInstance, Set<ModelDescription.ScalarVariable>> anyLiveStreamMap() {
		return any();
	}

	private static LogVariablesContainer anyLiveStreamMap2(){
		return any();
	}

	private void assertResponseOk(NanoHTTPD.Response response) {
		Assert.assertEquals(NanoHTTPD.Response.Status.OK, response.getStatus());
	}

	public void assertResponseBadRequest(NanoHTTPD.Response r) throws IOException
	{
		Assert.assertEquals("Failed to report error", NanoHTTPD.Response.Status.BAD_REQUEST, r.getStatus());
	}

	public void assertResponseMimeType(NanoHTTPD.Response r, MimeType m)
			throws IOException
	{
		Assert.assertEquals("Wrong content type returned", m.value, r.getMimeType());
	}

	static String getStatusSessionId(NanoHTTPD.Response response) throws IOException {
		String string = IOUtils.toString(response.getData());
		Assert.assertTrue("Expected data but nothing is returned", !string.trim().isEmpty());

		StatusMsgJson status = mapper.readValue(string, new TypeReference<StatusMsgJson>(){});
		return status.sessionId;
	}

	static List<String> getStatusSessionIds(NanoHTTPD.Response response)
			throws IOException
	{
		String string = IOUtils.toString(response.getData());
		Assert.assertTrue("Expected data but nothing is returned", !string.trim().isEmpty());

		List<StatusMsgJson> statuses = mapper.readValue(string, new TypeReference<List<StatusMsgJson>>()
		{
		});

		List<String> ids = new Vector<>();

		for (StatusMsgJson st : statuses)
		{
			ids.add(st.sessionId);
		}

		return ids;
	}

	@Test
	public void createSession_ReturnsSession() throws IOException, NanoHTTPD.ResponseException {
		NanoHTTPD.Response response = Utilities.runAny(RequestHandler,ihttpSessionMock,"/createSession");

		String re1="\\{\\\"sessionId\\\":\\\""+guidRegex+"\\\"\\}";	// Curly Braces 1

		Pattern p = Pattern.compile(re1,Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		String data = IOUtils.toString(response.getData());
		Matcher m = p.matcher(data);
		Assert.assertTrue("returned response did not match expected", m.find());
		Assert.assertEquals(NanoHTTPD.Response.Status.OK, response.getStatus());
		Assert.assertEquals("application/json", response.getMimeType());
	}
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


	@Test
	public void testInitialize_fmuDoesNotSupportVarstep() throws IOException, NanoHTTPD.ResponseException {
		NanoHTTPD.Response response = Utilities.runAny(RequestHandler,ihttpSessionMock,"/createSession");
		String session = extractSessionId(IOUtils.toString(response.getData()));

		when(coeMock.canDoVariableStep()).thenReturn(false);

		NanoHTTPD.Response r = Utilities.runAny(RequestHandler,ihttpSessionMock,IOUtils.toString(this.getClass().getResourceAsStream(WATERTANK_WITH_VARSTEP)),"/initialize/"+session);
		String data = IOUtils.toString(r.getData());

		// System.out.println(String.format("Status: '%s' Session: '%s'", st.status, st.sessionId));
		Assert.assertEquals(NanoHTTPD.Response.Status.BAD_REQUEST, r.getStatus());
		Assert.assertEquals("text/plain", r.getMimeType());
		Assert.assertEquals("One or more FMUs does not support variable step size.",data);
	}

	@Test
	public void testInitialize2() throws IOException, NanoHTTPD.ResponseException {
		NanoHTTPD.Response response = Utilities.runAny(RequestHandler,ihttpSessionMock,"/createSession");
		String session = extractSessionId(IOUtils.toString(response.getData()));

		NanoHTTPD.Response r = Utilities.runAny(RequestHandler,ihttpSessionMock,IOUtils.toString(this.getClass().getResourceAsStream(DEFAULT_CONFIG_PATH)),"/initialize/"+session);
		String data = IOUtils.toString(r.getData());

		// System.out.println(String.format("Status: '%s' Session: '%s'", st.status, st.sessionId));
		Assert.assertEquals(NanoHTTPD.Response.Status.OK, r.getStatus());
		Assert.assertEquals("application/json", r.getMimeType());

//		// pattern generated with:
//		// http://www.txt2re.com/index-java.php3?s=[{%22status%22:%22Initialized%22,%22sessionId%22:73885,%22lastExecTime%22:3,%22avaliableLogLevels%22:{%22Error%22:[{%22name%22:%22LowError%22,%22description%22:%22Lowest%20possible%20error%22}]}}]&-1
		String re1 = "(\\[\\{\"status\":\"Initialized\",\"sessionId\":\""+guidRegex+"\",\"lastExecTime\":3,\"avaliableLogLevels\":\\{\"Error\":\\[\\{\"name\":\"LowError\",\"description\":\"Lowest possible error\"\\}\\])"; // Square
//
		Pattern p = Pattern.compile(re1, Pattern.CASE_INSENSITIVE
				| Pattern.DOTALL);
		Matcher m = p.matcher(data);

		Assert.assertTrue("returned response did not match expected", m.find());
	}

	@Test
	public void testInitConfigError2() throws IOException, NanoHTTPD.ResponseException {
		NanoHTTPD.Response response = Utilities.runAny(RequestHandler,ihttpSessionMock,"/createSession");
		String session = extractSessionId(IOUtils.toString(response.getData()));
		NanoHTTPD.Response r = Utilities.runAny(RequestHandler,ihttpSessionMock,IOUtils.toString(this.getClass().getResourceAsStream("/configs/config_error1.json")),"/initialize/"+session);

		assertResponseBadRequest(r);
		assertResponseMimeType(r, MimeType.plain);
	}

	@Test
	public void apiCommandStatusWithSession() throws IOException, NanoHTTPD.ResponseException {
		Mockito.when(ihttpSessionMock.getUri()).thenReturn("/status");
		NanoHTTPD.Response response = RequestHandler.handleRequest(ihttpSessionMock);
		Assert.assertTrue("Did not expect any sessions", getStatusSessionIds(response).isEmpty());

		response = Utilities.runAny(RequestHandler,ihttpSessionMock,"/createSession");
		String sessionId2 = extractSessionId(IOUtils.toString(response.getData()));
		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/status");
		List<String> statuses = getStatusSessionIds(response);
		Assert.assertEquals("Expected 1 session", 1, statuses.size());
		Assert.assertTrue("Expected status to contain session: " + sessionId2, statuses.contains(sessionId2));

		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/status/"+sessionId2);

		Assert.assertEquals("Expected a status for sessionId: " + sessionId2, sessionId2, getStatusSessionId(response));
	}

	@Test
	public void apiCommandPlainTest() throws IOException, NanoHTTPD.ResponseException {
		NanoHTTPD.Response response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/api");
		assertResponseOk(response);
		assertResponseMimeType(response, MimeType.html);
	}



	@Test
	public void apiCommandPdfTest() throws IOException, NanoHTTPD.ResponseException {
		NanoHTTPD.Response response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/api/pdf");
		assertResponseOk(response);
		assertResponseMimeType(response, MimeType.pdf);
	}

	@Test
	public void apiCommandSimulateTest() throws
			IOException, NanoHTTPD.ResponseException {
		NanoHTTPD.Response response = Utilities.runAny(RequestHandler,ihttpSessionMock,"/simulate/1234");
		assertResponseBadRequest(response);
		assertResponseMimeType(response, MimeType.plain);
		response = Utilities.runAny(RequestHandler,ihttpSessionMock,"/createSession");
		String sessionId = extractSessionId(IOUtils.toString(response.getData()));
		response = Utilities.runAny(RequestHandler,ihttpSessionMock,IOUtils.toString(this.getClass().getResourceAsStream(DEFAULT_CONFIG_PATH)),"/initialize/"+sessionId);
		assertResponseOk(response);
		assertResponseMimeType(response, MimeType.json);

		StartMsgJson startMsg = new StartMsgJson();

		startMsg.startTime = 0;
		startMsg.endTime = 9;
		startMsg.logLevels = new HashMap<String, List<String>>();

		String data = mapper.writeValueAsString(startMsg);

		// simulate that we have simulated
		File tmpFolder = new File("target/api-test".replace('/', File.separatorChar));
		tmpFolder.mkdirs();
		when(coeMock.getResultRoot()).thenReturn(tmpFolder);
		response = Utilities.runAny(RequestHandler, ihttpSessionMock, data, "/simulate/"+sessionId);
		assertResponseOk(response);
		assertResponseMimeType(response, MimeType.json);

		// check status
		String string = IOUtils.toString(response.getData());
		StatusMsgJson status = mapper.readValue(string, new TypeReference<StatusMsgJson>(){});
		Assert.assertEquals("Expected a status for sessionId: " + sessionId, sessionId, status.sessionId);
		Assert.assertEquals("Expected status to be finished", "Finished", status.status);
	}

	@Test
	public void apiCommandResultPlainTest() throws IOException, NanoHTTPD.ResponseException {
		NanoHTTPD.Response response = null;
		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/createSession");
		String sessionId = extractSessionId(IOUtils.toString(response.getData()));
		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/result/1234");
		assertResponseBadRequest(response);
		assertResponseMimeType(response, MimeType.plain);
		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/result/" + sessionId);
		assertResponseBadRequest(response);
		assertResponseMimeType(response, MimeType.plain);
		// simulate that we have simulated
		File tmpFolder = Files.createTempFile("coe", "coe").toFile();
		when(coeMock.getResult()).thenReturn(tmpFolder);
		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/result/" + sessionId);
		assertResponseOk(response);
		assertResponseMimeType(response, MimeType.plain);
		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/result/" + sessionId + "/plain");
		assertResponseOk(response);
		assertResponseMimeType(response, MimeType.plain);
	}

	@Test
	public void apiCommandResultTest() throws IOException, NanoHTTPD.ResponseException {
		NanoHTTPD.Response response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/result");
		assertResponseBadRequest(response);
		assertResponseMimeType(response, MimeType.plain);

		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/result/0");
		assertResponseBadRequest(response);
		assertResponseMimeType(response, MimeType.plain);

		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/result/");
		assertResponseBadRequest(response);
		assertResponseMimeType(response, MimeType.plain);

		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/result/something");
		assertResponseBadRequest(response);
		assertResponseMimeType(response, MimeType.plain);
	}

	@Test
	public void apiCommandResultZipTest() throws IOException, NanoHTTPD.ResponseException {
		NanoHTTPD.Response response = null;
		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/createSession");
		String sessionId = extractSessionId(IOUtils.toString(response.getData()));

		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/result/1234/zip");
		assertResponseBadRequest(response);
		assertResponseMimeType(response, MimeType.plain);

		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/result/" + sessionId + "/zip");
		assertResponseBadRequest(response);
		assertResponseMimeType(response, MimeType.plain);

		// simulate that we have simulated
		File tmpFolder = Files.createTempDirectory("coe").toFile();
		when(coeMock.getResultRoot()).thenReturn(tmpFolder);

		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/result/" + sessionId + "/zip");
		assertResponseOk(response);
		assertResponseMimeType(response, MimeType.zip);
	}

	@Test
	public void Destroy_NoSessionInUrl_BadRequest() throws Exception
	{
		NanoHTTPD.Response response = null;
		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/destroy");
		assertResponseBadRequest(response);
		assertResponseMimeType(response, MimeType.plain);
	}

	@Test
	public void Destroy_WithSessionInUrl_SessionDeleted() throws Exception
	{
		// Arrange
		File resultRootFile = new File("test");
		when(coeMock.getResultRoot()).thenReturn(resultRootFile);
		PowerMockito.spy(FileUtils.class);
		PowerMockito.doNothing().when(FileUtils.class, "deleteDirectory", any(java.io.File.class));
		PowerMockito.doNothing().when(FileUtils.class, "write", any(java.io.File.class), anyString()); // To prevent
																										// creation of
																										// initialize.json
																										// when invoking
																										// "initialize"
		// Create session
		NanoHTTPD.Response response = null;
		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/createSession");
		String sessionId2 = extractSessionId(IOUtils.toString(response.getData()));
		// init
		response = Utilities.runAny(RequestHandler,ihttpSessionMock,IOUtils.toString(this.getClass().getResourceAsStream(DEFAULT_CONFIG_PATH)),"/initialize/"+sessionId2);
		assertResponseOk(response);
		assertResponseMimeType(response, MimeType.json);
		// Act
		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/destroy/"+sessionId2);
		// Verify the response from destroy
		assertResponseOk(response);
		assertResponseMimeType(response, MimeType.plain);
		response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/status");

		// Verify "destroy" results in passing the correct file argument to deleteDirectory
		PowerMockito.verifyStatic();
		ArgumentCaptor<File> fileUtilsArgumentCaptor2 = ArgumentCaptor.forClass(java.io.File.class);
		FileUtils.deleteDirectory(fileUtilsArgumentCaptor2.capture());
		Assert.assertEquals(new File(sessionId2), fileUtilsArgumentCaptor2.getValue());

		// Verify the created session has been deleted
		Assert.assertTrue("session was not deleted", !getStatusSessionIds(response).contains(sessionId2));
	}

	@Test
	public void apiCommandResetTest() throws IOException, NanoHTTPD.ResponseException {
		NanoHTTPD.Response response = Utilities.runAny(RequestHandler, ihttpSessionMock, "/reset");
		Assert.assertEquals(NanoHTTPD.Response.Status.NOT_IMPLEMENTED, response.getStatus());
		assertResponseMimeType(response, MimeType.plain);
	}
}
