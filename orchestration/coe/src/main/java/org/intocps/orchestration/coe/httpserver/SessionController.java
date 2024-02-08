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
package org.intocps.orchestration.coe.httpserver;

import org.intocps.orchestration.coe.json.SessionLogicFactory;
import org.intocps.orchestration.coe.json.StatusMsgJson;
import org.intocps.orchestration.coe.scala.Coe;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by ctha on 17-03-2016.
 */
public class SessionController
{
	private final Map<String, SessionLogic> coeInstanceMap = new HashMap<>();
	private final SessionLogicFactory sessionLogicFactory;

	public SessionController(SessionLogicFactory sessionLogicFactory)
	{
		this.sessionLogicFactory = sessionLogicFactory;
	}

	public String createNewSession()
	{
		String session = UUID.randomUUID().toString();
		coeInstanceMap.put(session, sessionLogicFactory.CreateSessionLogic(this.getSessionRootDir(session)));
		return session;
	}

	public Coe getCoe(String sessionId)
	{
		return coeInstanceMap.get(sessionId).coe();
	}

	public boolean containsSession(String sessionId)
	{
		return coeInstanceMap.containsKey(sessionId);
	}

	public SessionLogic removeSession(String sessionId)
	{
		return coeInstanceMap.remove(sessionId);
	}

	public List<StatusMsgJson> getStatus()
	{
		List<StatusMsgJson> data = coeInstanceMap.entrySet().stream().map(entry -> new StatusMsgJson(entry.getValue().coe().getState(), entry.getKey(), entry.getValue().coe().getLastExecTime())).collect(Collectors.toCollection(Vector::new));

		return data;
	}

	public StatusMsgJson getStatus(String sessionId)
	{
		Coe coe = coeInstanceMap.get(sessionId).coe();
		return new StatusMsgJson(coe.getState(), sessionId, coe.getLastExecTime());
	}

	public static boolean test = false;

	public File getSessionRootDir(String session)
	{
		if (test)
		{
			return new File(new File("target"), session);
		} else
		{
			return new File(session);
		}
	}

	public void removeSocket(String session)
	{
		coeInstanceMap.get(session).removeSocket();
	}

	public boolean containsSocket(String sessionId)
	{
		return coeInstanceMap.get(sessionId) != null
				&& coeInstanceMap.get(sessionId).containsSocket();
	}

	public void addSocket(String sessionId, NanoWebSocketImpl socket)
	{
		coeInstanceMap.get(sessionId).setWebSocket(socket);
	}
}
