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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.intocps.orchestration.coe.json.InitializationMsgJson;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Vector;

import fi.iki.elonen.NanoHTTPD;
public abstract class OnlineModelsCoSimTest extends BasicTest
{
	final static String prefix = "/online-models";
	public final static String baseDownloadUrl = "https://overture.au.dk/into-cps/examples/public-coe-test-fmus/latest/";


	public static void scanConfigsAndDownloadFmus(File rootPath) throws IOException
	{
		List<java.net.URL> urls = new Vector<>();

		final ObjectMapper mapper = new ObjectMapper();
		File dir = rootPath;
		String[] extensions = new String[] { "json" };
		List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);
		for (File file : files)
		{
			System.out.println("file: " + file.getCanonicalPath());

			InitializationMsgJson st = mapper.readValue(FileUtils.readFileToString(file), InitializationMsgJson.class);

			for (String fmuPath : st.fmus.values())
			{
				String fmuName = fmuPath.substring(fmuPath.lastIndexOf('/') + 1);
				if (fmuName.endsWith(".fmu"))
				{
					urls.add(new URL(baseDownloadUrl + fmuName));
				} else
				{
					System.err.println("Invalid fmu name: " + fmuName);
				}
			}
		}

		System.out.println("Downloading FMUs");
		for (URL url : urls)
		{
			String file = url.getFile();
			file = file.substring(file.lastIndexOf('/') + 1);
			File destination = new File("target/online-cache/" + file);
			if (!destination.exists())
			{
				System.out.println("Downloading: " + url + " as: "
						+ destination);
				org.apache.commons.io.FileUtils.copyURLToFile(url, destination);
			} else
			{
				System.out.println("Skipped - Downloading: " + url + " as: "
						+ destination);
			}
		}

	}

	@BeforeClass
	public static void downloadFmus() throws IOException
	{
		scanConfigsAndDownloadFmus( new File("src/test/resources" + prefix));
	}

	@Override
	protected void test(String configPath, double startTime, double endTime)
			throws IOException, JsonParseException, JsonMappingException,
			JsonProcessingException, NanoHTTPD.ResponseException {
		super.test(prefix + configPath, startTime, endTime);
	}

}
