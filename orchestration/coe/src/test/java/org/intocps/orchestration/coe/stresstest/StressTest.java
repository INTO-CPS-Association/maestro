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
package org.intocps.orchestration.coe.stresstest;

import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.RootLogger;
import org.intocps.orchestration.coe.BasicTest;
import org.intocps.orchestration.coe.OnlineModelsCoSimTest;
import org.intocps.orchestration.coe.scala.CoeObject;
import org.intocps.orchestration.coe.scala.CoeSimulator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by kel on 28/04/2017.
 */
public class StressTest extends BasicTest
{
	static public void unzip(File file, File outputDir) throws IOException
	{
		ZipFile zipFile = new ZipFile(file);
		try
		{
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements())
			{
				ZipEntry entry = entries.nextElement();
				File entryDestination = new File(outputDir, entry.getName());
				if (entry.isDirectory())
				{
					entryDestination.mkdirs();
				} else
				{
					entryDestination.getParentFile().mkdirs();
					InputStream in = zipFile.getInputStream(entry);
					OutputStream out = new FileOutputStream(entryDestination);
					IOUtils.copy(in, out);
					IOUtils.closeQuietly(in);
					out.close();
				}
			}
		} finally
		{
			zipFile.close();
		}
	}

	@BeforeClass public static void prepareFmu() throws IOException
	{
		//String file = "";
		String fmuName = "add-parameter.fmu";
		URL url = new URL(OnlineModelsCoSimTest.baseDownloadUrl + fmuName);
		File destination = new File("target/online-cache/" + fmuName);
		destination.getParentFile().mkdirs();
		if (!destination.exists())
		{
			System.out.println("Downloading: " + url + " as: " + destination);
			org.apache.commons.io.FileUtils.copyURLToFile(url, destination);
		} else
		{
			System.out.println(
					"Skipped - Downloading: " + url + " as: " + destination);
		}

		baseFmu = new File("target/online-cache/unpacked/"
				+ fmuName.substring(0, fmuName.lastIndexOf(".")));
		baseFmu.getParentFile().mkdirs();

		unzip(destination, baseFmu);

	}

	static File baseFmu = null;

	public static String getMethodName()
	{
		return Thread.currentThread().getStackTrace()[2].getMethodName();
	}

	void validateResult(File result)
	{
		try
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(result), "UTF-8"));

			String data = "";
			String line;
			while ((line = br.readLine()) != null)
			{
				// process the line.
				data = line;
			}

			boolean found = false;
			int skip = 2;
			for (String col : data.split(","))
			{
				if (skip > 0)
				{
					skip--;
					continue;
				}
				if (col.equals("0.0"))
				{
					found = true;
					break;
				}
			}
			Assert.assertFalse(
					"Incomplete simulation not enough steps was taken to replicate result through all instances. Expected no 0.0 values: "
							+ data, found);

			found = false;
			skip = 2;

			for (String col : data.split(","))
			{
				if (skip > 0)
				{
					skip--;
					continue;
				}

				if (col.equals("1.0"))
				{
					found = true;
					break;
				}
			}

			br.close();

			Assert.assertTrue(
					"The expected result 1.0 not found with a complete run: "
							+ data, found);

		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Test public void fmuMaxLoadTest()
			throws IOException, NanoHTTPD.ResponseException
	{
		//File baseFmu = new File("/Users/kel/data/into-cps/public-coe-test-fmus/add-parameter");
		File output = Paths.get("target", "stress-test", getMethodName()).toFile();

		final int replications = 100;

		Logger.getRootLogger().setLevel(Level.ERROR);

		Map<Integer, Map<String, Object>> parametersArg = new HashedMap();
		Map<String, Object> parameters = new HashedMap();
		parametersArg.put(0, parameters);
		parameters.put("activated", true);
		parameters.put("feedback", true);

		String config = replicateAndChain(baseFmu, "in", "out", output, parametersArg, replications, true);
		System.out.println(config);

		File configFile = new File(output, "stressTest.config");
		FileUtils.write(configFile, config);

		//Total simulation time 00:01:36,117, initialization time 00:00:00,136, Simulation time 00:01:35,981

		System.setProperty("simulation.parallelise.resolveinputs", "true");
		System.setProperty("simulation.parallelise.setinputs", "true");
		System.setProperty("simulation.parallelise.dostep", "true");
		System.setProperty("simulation.parallelise.obtainstate", "true");

		System.setProperty("simulation.profile.executiontime", "false");

		//Total simulation time 00:00:57,521, initialization time 00:00:00,136, Simulation time 00:00:57,385

		testExternalConfig(configFile.getAbsolutePath(), 0,
				((replications) * 0.1) + 0.01);

		File result = new File(output, "result.csv");
		validateResult(result);

	}

	@Test public void showExecutionTime()
			throws IOException, NanoHTTPD.ResponseException
	{
		//	File baseFmu = new File("/Users/kel/data/into-cps/public-coe-test-fmus/add-parameter");
		File output = Paths.get("target", "stress-test", getMethodName()).toFile();

		final int replications = 100;

		Logger.getRootLogger().setLevel(Level.DEBUG);

		Map<Integer, Map<String, Object>> parametersArg = new HashedMap();
		Map<String, Object> parameters = new HashedMap();
		parametersArg.put(0, parameters);
		parameters.put("activated", true);
		parameters.put("feedback", true);

		String config = replicateAndChain(baseFmu, "in", "out", output, parametersArg, replications, true);
		System.out.println(config);

		File configFile = new File(output, "stressTest.config");
		FileUtils.write(configFile, config);

		//Total simulation time 00:01:36,117, initialization time 00:00:00,136, Simulation time 00:01:35,981

		System.setProperty("simulation.parallelise.resolveinputs", "true");
		System.setProperty("simulation.parallelise.setinputs", "true");
		System.setProperty("simulation.parallelise.dostep", "true");
		System.setProperty("simulation.parallelise.obtainstate", "true");

		System.setProperty("simulation.profile.executiontime", "true");

		//Total simulation time 00:00:57,521, initialization time 00:00:00,136, Simulation time 00:00:57,385

		double end =((replications) * 0.1) + 0.01;

		System.out.println("Simulating with end time: "+end);

		testExternalConfig(configFile.getAbsolutePath(), 0,
				end);
	}

	private static String OS = System.getProperty("os.name").toLowerCase();

	public static boolean isWindows() {
		return (OS.indexOf("win") >= 0);
	}

	public static boolean isMac() {
		return (OS.indexOf("mac") >= 0);
	}

	public static boolean isUnix() {
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
	}

	private String replicateAndChain(File baseFmu, String inName,
			String outName, File output,
			Map<Integer, Map<String, Object>> parametersArg, int max,
			boolean useMultipleInstances) throws IOException
	{
		final String md = "modelDescription.xml";
		final String bin = "binaries";
		final String[] PLATFORMS = calculateRequiredPlatforms();

		final String namePattern = "%s-%d";
		final String connection = useMultipleInstances ?
				"\"{f0}.%s.%s\":[\"{f0}.%s.%s\"]" :
				"\"{%s}.i.%s\":[\"{%s}.i.%s\"]";
		final String fmuPattern = useMultipleInstances ?
				"\"{f%s}\":\"%s\"" :
				"\"{%s}\":\"%s\"";
		final String instanceParameter = useMultipleInstances ?
				"\"{f0}.%s.%s\":%s" :
				"\"{%s}.i.%s\":%s";
		String baseName = baseFmu.getName();

		List<String> connections = new Vector<>();
		List<String> fmus = new Vector<>();
		List<String> parameters = new Vector<>();
		output.mkdirs();
		for (int i = 0; i < max + 1; i++)
		{
			File fmu = null;
			String name = String.format(namePattern, baseName, i);
			if (!useMultipleInstances || i == 0)
			{
				fmu = new File(output, name).getCanonicalFile();
				fmus.add(String.format(fmuPattern, i, fmu.toURI().toString()));
			}
			//parameter first fmu
			if (parametersArg != null && parametersArg.containsKey(i))
			{
				for (Map.Entry<String, Object> parameter : parametersArg.get(i).entrySet())
				{
					parameters.add(String.format(instanceParameter, i, parameter.getKey(), parameter.getValue()));
				}

			}

			if (!useMultipleInstances || i == 0)
			{
				FileUtils.copyFile(new File(baseFmu, md), new File(fmu, md));

				for (String p : PLATFORMS)
				{
					File libraryDir = Paths.get(baseFmu.getAbsolutePath(), bin, p).toFile();
					File libraryDirTarget = Paths.get(fmu.getAbsolutePath(), bin, p).toFile();
					if (libraryDir.exists())
					{
						for (File file : libraryDir.listFiles())
						{
							String targetName = file.getName();
							if (file.getName().startsWith(baseName))
							{
								targetName = name
										+ targetName.substring(targetName.lastIndexOf('.'));
							}
							FileUtils.copyFile(file, new File(libraryDirTarget, targetName));
						}
					}
				}
			}

			if (i + 1 < max + 1)
			{
				connections.add(String.format(connection, i, outName,
						i + 1, inName));
			}

		}

		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("\"fmus\":{\n");

		Iterator<String> itr = fmus.iterator();
		while (itr.hasNext())
		{
			sb.append(itr.next());

			if (itr.hasNext())
				sb.append(",");

			sb.append("\n");
		}
		sb.append("},\n");
		sb.append("\"connections\":{\n");

		itr = connections.iterator();
		while (itr.hasNext())
		{
			sb.append(itr.next());

			if (itr.hasNext())
				sb.append(",");

			sb.append("\n");
		}
		sb.append("},\n");

		sb.append("\"parameters\":{\n");

		itr = parameters.iterator();
		while (itr.hasNext())
		{
			sb.append(itr.next());

			if (itr.hasNext())
				sb.append(",");

			sb.append("\n");
		}
		sb.append("}\n");

		sb.append("}");
		return sb.toString();
	}

	private String[] calculateRequiredPlatforms()
	{
		List<String> platforms = new Vector<>();

		if(isMac())
		platforms.add("darwin64");

		if(isWindows())
		{
			platforms.add("win32");
			platforms.add("win64");
		}

		if(isUnix())
		{
			platforms.add("linux32");
			platforms.add("linux64");
		}

		return platforms.toArray(new String[0]);
	}
}
