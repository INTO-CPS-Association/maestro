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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.intocps.orchestration.coe.httpserver.Response;

public class HttpUtil
{
	public static BufferedInputStream toInput(String data)

	{
		return new BufferedInputStream(new ByteArrayInputStream(data.getBytes()));
	}

	public static Properties createHeader(String data)
	{
		Properties props = new Properties();
		props.put(Response.HEADER_CONTENT_LENGTH, "" + data.getBytes().length);
		props.put(Response.HEADER_CONTENT_TYPE, "application/json");
		return props;
	}

	public static Properties createMultiPartFormDataHeader(File data, String boundary) throws IOException {
		Properties props = new Properties();
		props.put(Response.HEADER_CONTENT_LENGTH, "" + Files.readAllBytes(Paths.get(data.getAbsolutePath())).length);
		props.put(Response.HEADER_CONTENT_TYPE, "multipart/form-data, boundary="+boundary);
		return props;
	}
}
