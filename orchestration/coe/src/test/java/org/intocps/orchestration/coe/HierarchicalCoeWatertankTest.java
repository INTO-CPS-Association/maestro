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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.orchestration.coe.hierarchical.HierarchicalCoeFactory;
import org.intocps.orchestration.coe.util.FolderCompressor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class HierarchicalCoeWatertankTest extends OnlineModelsCoSimTest
{

	@BeforeClass public static void enableHierarchicalCoeFactory()
	{
		FmuFactory.customFactory = new HierarchicalCoeFactory();
	}

	@AfterClass public static void disableHierarchicalCoeFactory()
	{
		FmuFactory.customFactory = null;
	}

	@Test public void testWatertankC()
			throws IOException, NanoHTTPD.ResponseException
	{

		File fmuFolder = new File("target/online-cache/hierarchicalCoeWatertank");
		fmuFolder.mkdirs();

		File resourcesFolder = new File(fmuFolder, "resources");
		resourcesFolder.mkdirs();

		File inputModelFolder = new File("src/test/resources/online-models/hierarchicalCoeTests/watertank");
		FileUtils.copyFile(new File(inputModelFolder,"modelDescriptionCoe.xml"), new File(fmuFolder, "modelDescription.xml"));
		FileUtils.copyFile(new File(inputModelFolder,"modelDescriptionInnerStub.xml"), new File(new File(resourcesFolder, "external"), "modelDescription.xml"));
		FileUtils.copyFile(new File(inputModelFolder,"configInner.json"), new File(resourcesFolder, "config.json"));
		FileUtils.copyFile(new File(fmuFolder.getParent(), "watertank-c.fmu"), new File(resourcesFolder, "watertank-c.fmu"));

		String configPath = new File(inputModelFolder,"config.json").getPath().replace('\\','/');
		configPath = configPath.substring(configPath.indexOf(prefix)+prefix.length());
		test(configPath, 0, 30);
	}

	@Test public void testWatertankCArchive()
			throws IOException, NanoHTTPD.ResponseException
	{

		File fmuFolder = new File("target/online-cache/hierarchicalCoeWatertankArchive");
		fmuFolder.mkdirs();

		File resourcesFolder = new File(fmuFolder, "resources");
		resourcesFolder.mkdirs();

		File inputModelFolder = new File("src/test/resources/online-models/hierarchicalCoeTests/watertankArchive");
		FileUtils.copyFile(new File(inputModelFolder,"modelDescriptionCoe.xml"), new File(fmuFolder, "modelDescription.xml"));
		FileUtils.copyFile(new File(inputModelFolder,"modelDescriptionInnerStub.xml"), new File(new File(resourcesFolder, "external"), "modelDescription.xml"));
		FileUtils.copyFile(new File(inputModelFolder,"configInner.json"), new File(resourcesFolder, "config.json"));
		FileUtils.copyFile(new File(fmuFolder.getParent(), "watertank-c.fmu"), new File(resourcesFolder, "watertank-c.fmu"));

		FolderCompressor.compress(fmuFolder,new File(fmuFolder.getParentFile(),"hierarchicalCoeWatertank.fmu"));

		String configPath = new File(inputModelFolder,"config._son").getPath().replace('\\','/');
		configPath = configPath.substring(configPath.indexOf(prefix)+prefix.length());
		test(configPath, 0, 30);
	}

	@Test public void testWatertankCImploded()
			throws IOException, NanoHTTPD.ResponseException
	{

		File fmuFolder = new File("target/online-cache/hierarchicalCoeWatertankImploded");
		fmuFolder.mkdirs();

		File resourcesFolder = new File(fmuFolder, "resources");
		resourcesFolder.mkdirs();
		File inputModelFolder = new File("src/test/resources/online-models/hierarchicalCoeTests/implodedWaterank");


		FileUtils.copyFile(new File(inputModelFolder,"modelDescriptionCoe.xml"), new File(fmuFolder, "modelDescription.xml"));
		FileUtils.copyFile(new File(inputModelFolder,"modelDescriptionInnerStub.xml"), new File(new File(resourcesFolder, "external"), "modelDescription.xml"));
		FileUtils.copyFile(new File(inputModelFolder,"configInner.json"), new File(resourcesFolder, "config.json"));
		FileUtils.copyFile(new File(fmuFolder.getParent(), "watertankcontroller-c.fmu"), new File(resourcesFolder, "watertankcontroller-c.fmu"));
		FileUtils.copyFile(new File(fmuFolder.getParent(), "singlewatertank-20sim.fmu"), new File(resourcesFolder, "singlewatertank-20sim.fmu"));

		String configPath = new File(inputModelFolder,"config.json").getPath().replace('\\','/');
		configPath = configPath.substring(configPath.indexOf(prefix)+prefix.length());
		test(configPath, 0, 30);
	}

}
