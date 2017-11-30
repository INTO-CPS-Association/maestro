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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ctha on 17-03-2016.
 */
public class ProcessingUtils {
    final static Logger logger = LoggerFactory.getLogger(ProcessingUtils.class);
    // Reads until clrf is met.
    public static String readUntilLineEnd(InputStream reader)
            throws IOException
    {
        String data = "";
        int charsRead;
        char[] crlf = new char[] { '\r', '\n' };
        char[] dataChar = new char[2];
        for (int i = 0; i < dataChar.length; i++)
        {
            dataChar[i] = (char) reader.read();
        }

        while (!Arrays.equals(dataChar, crlf)
                && (charsRead = reader.read()) != -1)
        {
            data += dataChar[0];
            dataChar[0] = dataChar[1];
            dataChar[1] = (char) charsRead;
        }
        return data;
    }

    // Reads until an empty line is read
    public static String readUntilBlank(BufferedInputStream bufferedInputStream)
            throws IOException
    {
        List<String> returnLines = new ArrayList<>();
        String line = readUntilLineEnd(bufferedInputStream);
        // System.out.println(line);
        while (!line.isEmpty())
        {
            returnLines.add(line);
            line = readUntilLineEnd(bufferedInputStream);
            // System.out.println(line);
        }
        return StringUtils.join(returnLines, " ");
    }

    // Reads from the stream until boundaryArr is reached and stores the data in "filename".
    public static void storePartContent(File filePath, BufferedInputStream bufferedInputStream, byte[] boundaryArr)
            throws IOException
    {
        logger.debug("Storing uploaded file: {}", filePath);
        FileOutputStream fos = new FileOutputStream(filePath);
        byte[] cbuf = new byte[boundaryArr.length];
        int charsRead;
        for (int i = 0; i < cbuf.length; i++)
        {
            cbuf[i] = (byte) bufferedInputStream.read();
        }

        while ((charsRead = bufferedInputStream.read()) != -1)
       {
            fos.write(cbuf[0]);
            for (int i = 1; i < cbuf.length; i++)
            {
                cbuf[i - 1] = cbuf[i];
            }
            cbuf[cbuf.length - 1] = (byte) charsRead;
            if (Arrays.equals(boundaryArr, cbuf))
            {
                break;
            }
        }
        fos.close();
    }

    public static Boolean canObjectBeCastToDouble(final Object obj)
    {
        return !(obj == null || !(Integer.class.isAssignableFrom(obj.getClass()) || Double.class.isAssignableFrom(obj.getClass())));
    }

    public static Double castObjectToDouble(Object obj)
    {
        Double dbl;
        if (obj instanceof Integer)
        {
            dbl = new Double((int) obj);
        } else
        {
            dbl = (double) obj;
        }
        return dbl;
    }

    public static String getSessionFromUrl(String url, String apiRequest) {
        if (url.length() > apiRequest.length() + 1)// slash and at least one number char
        {
            String sessionId = url.substring((apiRequest.length() + 1));
            if (sessionId != null) {
                return sessionId;
            }
        }
        return null;
    }



    public static NanoHTTPD.Response newFixedLengthPlainResponse(NanoHTTPD.Response.Status status, String msg) {
        return NanoHTTPD.newFixedLengthResponse(status, Response.MIME_PLAINTEXT, msg);
    }
}
