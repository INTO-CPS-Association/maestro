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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.intocps.fmi.FmuInvocationException;
import org.intocps.fmi.IFmu;
import org.intocps.fmi.jnifmuapi.Factory;
import org.intocps.orchestration.coe.hierarchical.HierarchicalCoeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FmuFactory
{
	final static Logger logger = LoggerFactory.getLogger(FmuFactory.class);
	final static HierarchicalCoeFactory hierarchicalCoeFactory = new HierarchicalCoeFactory();

	private static class LocalFmuFactory implements IFmuFactory
	{

		@Override public boolean accept(URI uri)
		{
			return true;
		}

		@Override public IFmu instantiate(File sessionRoot, URI uri)
				throws Exception
		{

			if (uri.getScheme() != null
					&& uri.getScheme().equals(SESSION_SCHEME))
			{
				uri = new URI(
						"file://" + sessionRoot.toURI() + "/" + uri.getPath());
			}

			if(hierarchicalCoeFactory.accept(uri))
			{
				return hierarchicalCoeFactory.instantiate(sessionRoot,uri);
			}

			if (uri.getScheme() == null || uri.getScheme().equals("file"))
			{
				if (!uri.isAbsolute())
				{
					uri = new File(".").toURI().resolve(uri);
				}
				File file = new File(uri);
				if (!file.exists())
				{
					throw new FileNotFoundException(file.getAbsolutePath());
				}
				if (file.isFile())
				{
					return Factory.create(file);
				} else
				{
					return Factory.createFromDirectory(file);
				}
			}else if("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
			{
				File temp = new File(new File(uri.getPath()).getName());
				FileUtils.copyURLToFile(uri.toURL(), temp);
				return this.instantiate(sessionRoot,temp.toURI());
			}
			logger.error("Cannot create FMU from: {}", uri);
			return null;
		}

	}

	final static IFmuFactory localFactory = new LocalFmuFactory();

	public static IFmuFactory customFactory = null;
	public final static String customFmuFactoryProperty = "coe.fmu.custom.factory";

	static
	{
		String customFmuFactoryQualifiedName = System.getProperty(customFmuFactoryProperty);
		if (customFmuFactoryQualifiedName != null)
		{
			logger.trace("Obtained custum fmu factory with qualified class name '{}'", customFmuFactoryQualifiedName);
			Class<?> cls;
			try
			{
				logger.trace("Instantiating custom fmu factory with qualified class name '{}'", customFmuFactoryQualifiedName);

				cls = Class.forName(customFmuFactoryQualifiedName);
				if (IFmuFactory.class.isAssignableFrom(cls))
				{
					customFactory = (IFmuFactory) cls.newInstance();
				} else
				{
					logger.trace("Custom fmu factory with qualified class name '{}' dows not implement interface {}", customFmuFactoryQualifiedName, IFmuFactory.class.getName());
				}
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e)
			{
				logger.error("Failed to instantiate custom fmu factory", e);
			}
		}
	}

	public static IFmu create(String path)
			throws IOException, FmuInvocationException
	{
		return Factory.create(new File(path));
	}

	public static IFmu create(File sessionRoot, URI uri) throws Exception
	{
		if (customFactory != null && customFactory.accept(uri))
		{
			return customFactory.instantiate(sessionRoot, uri);
		}
		return localFactory.instantiate(sessionRoot, uri);
	}
}
