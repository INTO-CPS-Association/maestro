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

import fi.iki.elonen.NanoHTTPD;
import org.intocps.orchestration.coe.httpserver.RequestHandler;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.Map;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doAnswer;

/**
 * Created by ctha on 30-03-2016.
 */
public class Utilities {

    public static NanoHTTPD.Response runAny(RequestHandler RequestHandler, NanoHTTPD.IHTTPSession sessionMock, final String data, String uri) throws IOException, NanoHTTPD.ResponseException {
        Mockito.when(sessionMock.getUri()).thenReturn(uri);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (data != null) {
                    Map<String, String> files = (Map<String, String>) invocationOnMock.getArguments()[0];
                    files.put("postData", data);
                }
                return null;
            }
        }).when(sessionMock).parseBody(anyMap());

        return RequestHandler.handleRequest(sessionMock);
    }

    public static NanoHTTPD.Response runAny(RequestHandler RequestHandler, NanoHTTPD.IHTTPSession sessionMock, String uri) throws IOException, NanoHTTPD.ResponseException {
        Mockito.when(sessionMock.getUri()).thenReturn(uri);
        return RequestHandler.handleRequest(sessionMock);
    }
}
