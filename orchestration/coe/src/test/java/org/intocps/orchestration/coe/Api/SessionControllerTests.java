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

import org.intocps.orchestration.coe.httpserver.NanoWebSocketImpl;
import org.intocps.orchestration.coe.httpserver.SessionController;
import org.intocps.orchestration.coe.httpserver.SessionLogic;
import org.intocps.orchestration.coe.json.SessionLogicFactory;
import org.intocps.orchestration.coe.json.StatusMsgJson;
import org.intocps.orchestration.coe.scala.Coe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by ctha on 17-03-2016.
 */
public class SessionControllerTests {
	
	@Before
	public void setup(){
		SessionController.test=true;
	}
	
    @Test
    public void createNewSession_ReturnsSession()
    {
        SessionLogicFactory sessionLogicFactoryMock = mock(SessionLogicFactory.class);

        SessionController sessionController = new SessionController(sessionLogicFactoryMock);
        String sessionId = sessionController.createNewSession();

        Assert.assertTrue(sessionId !=null);
        verify(sessionLogicFactoryMock, times(1)).CreateSessionLogic(any(File.class));
    }

    @Test
    public void getCoe_SessionIsCreated_ReturnsCOE()
    {
        SessionLogicFactory sessionLogicFactoryMock = mock(SessionLogicFactory.class);
        SessionLogic sessionLogicMock = mock(SessionLogic.class);
        when(sessionLogicFactoryMock.CreateSessionLogic(any(File.class))).thenReturn(sessionLogicMock);
        Coe coeMock = mock(Coe.class);
        when(sessionLogicMock.coe()).thenReturn(coeMock);

        SessionController sessionController = new SessionController(sessionLogicFactoryMock);
        String sessionId = sessionController.createNewSession();
        Assert.assertEquals(coeMock, sessionController.getCoe(sessionId));
    }

    @Test
    public void containsSession_SessionIsCreated_ReturnsTrue()
    {
        SessionLogicFactory sessionLogicFactoryMock = mock(SessionLogicFactory.class);
        SessionLogic sessionLogicMock = mock(SessionLogic.class);
        when(sessionLogicFactoryMock.CreateSessionLogic(any(File.class))).thenReturn(sessionLogicMock);

        SessionController sessionController = new SessionController(sessionLogicFactoryMock);
        String sessionId = sessionController.createNewSession();
        Assert.assertEquals(true, sessionController.containsSession(sessionId));
    }

    @Test
    public void removeSession_SessionIsCreated_RemovesCOE()
    {
        SessionLogicFactory sessionLogicFactoryMock = mock(SessionLogicFactory.class);
        SessionLogic sessionLogicMock = mock(SessionLogic.class);
        when(sessionLogicFactoryMock.CreateSessionLogic(any(File.class))).thenReturn(sessionLogicMock);

        SessionController sessionController = new SessionController(sessionLogicFactoryMock);
        String sessionId = sessionController.createNewSession();
        sessionController.removeSession(sessionId);
        Assert.assertEquals(false, sessionController.containsSession(sessionId));
    }

    @Test
    public void getStatus_NoSessionArgTwoSessionsExists_ReturnsSessions()
    {
        SessionLogicFactory sessionLogicFactoryMock = mock(SessionLogicFactory.class);
        SessionLogic sessionLogicMock1 = mock(SessionLogic.class);
        SessionLogic sessionLogicMock2 = mock(SessionLogic.class);
        when(sessionLogicFactoryMock.CreateSessionLogic(any(File.class))).thenReturn(sessionLogicMock1).thenReturn(sessionLogicMock2);

        Coe coeMock0 = mock(Coe.class);
        when(coeMock0.getLastExecTime()).thenReturn(5L);
        when(coeMock0.getState()).thenReturn("initialized");
        when(sessionLogicMock1.coe()).thenReturn(coeMock0);
        Coe coeMock1 = mock(Coe.class);
        when(coeMock1.getLastExecTime()).thenReturn(6L);
        when(coeMock1.getState()).thenReturn("simulating");
        when(sessionLogicMock2.coe()).thenReturn(coeMock1);

        SessionController sessionController = new SessionController(sessionLogicFactoryMock);
        String sessionId0 = sessionController.createNewSession();
        String sessionId1 = sessionController.createNewSession();

        Assert.assertNotEquals(sessionId0,sessionId1);

        List<StatusMsgJson> actualMsgs = sessionController.getStatus();

        Assert.assertTrue(actualMsgs.stream().anyMatch(id->id.sessionId.equals(sessionId0)));
        Assert.assertTrue(actualMsgs.stream().anyMatch(id->id.sessionId.equals(sessionId1)));
    }

    @Test
    public void getStatus_SessionArgSessionExists_ReturnsSessions()
    {
        SessionLogicFactory sessionLogicFactoryMock = mock(SessionLogicFactory.class);
        SessionLogic sessionLogicMock = mock(SessionLogic.class);
        when(sessionLogicFactoryMock.CreateSessionLogic(any(File.class))).thenReturn(sessionLogicMock);
        Coe coeMock0 = mock(Coe.class);
        when(coeMock0.getLastExecTime()).thenReturn(5L);
        when(coeMock0.getState()).thenReturn("initialized");
        when(sessionLogicMock.coe()).thenReturn(coeMock0);

        SessionController sessionController = new SessionController(sessionLogicFactoryMock);
        String sessionId = sessionController.createNewSession();
        StatusMsgJson expectedMsg = new StatusMsgJson("initialized", sessionId, 5L);

        StatusMsgJson actualMsg = sessionController.getStatus(sessionId);

        Assert.assertEquals(expectedMsg.status, actualMsg.status);
        Assert.assertEquals(expectedMsg.sessionId, actualMsg.sessionId);
        Assert.assertEquals(expectedMsg.lastExecTime, actualMsg.lastExecTime);
    }

    @Test
    public void getSessionRootDir_ReturnsDir(){
        SessionLogicFactory sessionLogicFactoryMock = mock(SessionLogicFactory.class);
        SessionController sessionController = new SessionController(sessionLogicFactoryMock);

        File file = sessionController.getSessionRootDir("1");

        Assert.assertEquals(new File("target",Integer.toString(1)),file);


    }

    @Test
    public void addSocket_InvokesSetWebSocket(){
        SessionLogicFactory sessionLogicFactoryMock = mock(SessionLogicFactory.class);
        SessionLogic sessionLogicMock = mock(SessionLogic.class);
        when(sessionLogicFactoryMock.CreateSessionLogic(any(File.class))).thenReturn(sessionLogicMock);
        NanoWebSocketImpl nanoWebSocketMock = mock(NanoWebSocketImpl.class);

        SessionController sessionController = new SessionController(sessionLogicFactoryMock);
        String sessionId = sessionController.createNewSession();
        sessionController.addSocket(sessionId, nanoWebSocketMock);

        verify(sessionLogicMock,times(1)).setWebSocket(any(NanoWebSocketImpl.class));
    }

    @Test
    public void removeSocket_invokesRemoveSocket()
    {
        SessionLogicFactory sessionLogicFactoryMock = mock(SessionLogicFactory.class);
        SessionLogic sessionLogicMock = mock(SessionLogic.class);
        when(sessionLogicFactoryMock.CreateSessionLogic(any(File.class))).thenReturn(sessionLogicMock);

        SessionController sessionController = new SessionController(sessionLogicFactoryMock);
        String sessionId = sessionController.createNewSession();
        sessionController.removeSocket(sessionId);

        verify(sessionLogicMock,times(1)).removeSocket();
    }

    @Test
    public void containsSocket_invokesContainsSocket()
    {
        SessionLogicFactory sessionLogicFactoryMock = mock(SessionLogicFactory.class);
        SessionLogic sessionLogicMock = mock(SessionLogic.class);
        when(sessionLogicFactoryMock.CreateSessionLogic(any(File.class))).thenReturn(sessionLogicMock);

        SessionController sessionController = new SessionController(sessionLogicFactoryMock);
        String sessionId = sessionController.createNewSession();
        sessionController.containsSocket(sessionId);

        verify(sessionLogicMock,times(1)).containsSocket();
    }
}
