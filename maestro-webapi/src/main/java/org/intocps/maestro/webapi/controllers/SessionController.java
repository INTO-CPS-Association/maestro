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
package org.intocps.maestro.webapi.controllers;


import org.apache.commons.io.FileUtils;
import org.intocps.maestro.webapi.maestro2.dto.StatusModel;
import org.intocps.maestro.webapi.util.Files;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by ctha on 17-03-2016.
 */
public class SessionController {
    public static boolean test = false;
    private final Map<String, SessionLogic> maestroInstanceMap = new HashMap<>();
    private final SessionLogicFactory sessionLogicFactory;


    public SessionController(SessionLogicFactory sessionLogicFactory) {
        this.sessionLogicFactory = sessionLogicFactory;
    }

    public String createNewSession() {
        String session = UUID.randomUUID().toString();
        synchronized (maestroInstanceMap) {
            maestroInstanceMap.put(session, sessionLogicFactory.createSessionLogic(this.getSessionRootDir(session)));
        }
        return session;
    }

    public SessionLogic getSessionLogic(String sessionID) {
        synchronized (maestroInstanceMap) {
            return maestroInstanceMap.get(sessionID);
        }
    }

    public boolean containsSession(String sessionId) {
        synchronized (maestroInstanceMap) {
            return maestroInstanceMap.containsKey(sessionId);
        }
    }

    public SessionLogic removeSession(String sessionId) {
        synchronized (maestroInstanceMap) {
            return maestroInstanceMap.remove(sessionId);
        }
    }

    public List<StatusModel> getStatus() {
        return maestroInstanceMap.entrySet().stream()
                .map(entry -> new StatusModel(entry.getValue().getStatus() + "", entry.getKey(), entry.getValue().getLastExecTime()))
                .collect(Collectors.toCollection(Vector::new));
    }

    public StatusModel getStatus(String sessionId) throws Exception {
        return maestroInstanceMap.entrySet().stream().filter(entry -> entry.getKey().equals(sessionId)).findFirst()
                .map(kv -> new StatusModel(kv.getValue().getStatus() + "", kv.getKey(), kv.getValue().getLastExecTime()))
                .orElseThrow(() -> new Exception("No such session id."));
    }

    public void deleteSession(String sessionId) throws IOException {
        synchronized (maestroInstanceMap) {
            FileUtils.deleteDirectory(maestroInstanceMap.get(sessionId).rootDirectory);
        }
        this.removeSession(sessionId);

    }

    public File getSessionRootDir(String session) {
        if (test) {
            return new File(new File("target"), session);
        } else {
            return new File(Files.createTempDir(), session);
        }
    }

    public void removeSocket(String sessionId) {
        this.getSessionLogic(sessionId).removeSocket();
    }


    public boolean containsSocket(String sessionId) {
        return this.getSessionLogic(sessionId).containsSocket();
    }

    public void addSocket(String sessionId, WebSocketSession socket) {
        this.getSessionLogic(sessionId).setWebsocketSession(socket);
    }
}
