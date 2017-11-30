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

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by ctha on 11-04-2016.
 */
public class NanoWebSocketImpl extends NanoWSD.WebSocket {

    final static Logger logger = LoggerFactory.getLogger(NanoWebSocketImpl.class);
    private final RequestHandler requestHandler;
    public final String session;
    public NanoWebSocketImpl(NanoHTTPD.IHTTPSession handshakeRequest, RequestHandler requestHandler, String session) {
        super(handshakeRequest);
        this.requestHandler = requestHandler;
        this.session = session;
    }

    @Override
    protected void onOpen() {
        logger.debug("Opening web socket for session: " + session);
        this.requestHandler.handleSocketOpen(session);
    }

    @Override
    protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        this.requestHandler.handleSocketClose(session);
        logger.debug("C [" + (initiatedByRemote ? "Remote" : "Self") + "] " + (code != null ? code : "UnknownCloseCode[" + code + "]")
                + (reason != null && !reason.isEmpty() ? ": " + reason : ""));
    }

    @Override
    protected void onMessage(NanoWSD.WebSocketFrame message) {
        logger.debug("Received message: " + message.getTextPayload());
    }

    @Override
    protected void onPong(NanoWSD.WebSocketFrame message) {
        logger.debug("Received Pong: " + message.getTextPayload());
        System.out.println("P " + message);
    }

    @Override
    protected void onException(IOException e) {
        logger.warn("Exception occured: " + e);
    }

}
