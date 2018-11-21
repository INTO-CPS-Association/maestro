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
package org.intocps.orchestration.coe.hierarchical;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.fmi.*;
import org.intocps.fmi.jnifmuapi.TempDirectory;
import org.intocps.fmi.jnifmuapi.ZipUtility;
import org.intocps.orchestration.coe.json.InitializationMsgJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by kel on 04/10/2017.
 */
public class HierarchicalCoeFmu implements IFmu
{
	private final static Logger logger = LoggerFactory.getLogger(HierarchicalCoeFmu.class);
	private final URI uri;
	private final File tmpFolder;
	private static final String MODEL_DESCRIPTION = "modelDescription.xml";
	private boolean unpacked = false;
	private boolean loaded = false;
	private final File path;
	private final ArrayList<File> additionalResources = new ArrayList<>();

	static String uriToPath(URI uri)
	{
		if(uri.getScheme()!=null && uri.getScheme().equals("coe"))
			return new File(uri.getSchemeSpecificPart()).getAbsolutePath();

		String uriAsString = uri.toString();
		if(uri.getScheme() != null)
			uriAsString = uriAsString.replaceFirst(uri.getScheme(),"file");

		uri = URI.create(uriAsString);
		File f = new File(uri);
		return f.getAbsolutePath();
//		String p = uri.getHost() == null ?
//				uri.getPath() :
//				uri.getHost();
//		if (uri.getHost() != null)
//			p = Paths.get(p, uri.getPath()).toString();
//		return p;
	}

	public HierarchicalCoeFmu(URI uri) throws FmuInvocationException
	{
		this.uri = uri;
		this.tmpFolder = createTempDir(getFmuName(getFmuFile(uri)));
		this.path = getFmuFile(uri);
	}

	private static File getFmuFile(URI uri)
	{
		return URIUtil.schemeSpecificToLocal(uri);
	}

	private static File createTempDir(String prefix)
	{
		TempDirectory dir = new TempDirectory(prefix);
		dir.deleteOnExit();
		return dir.getPath().toFile();
	}

	private boolean isArchive()
	{
		return uri.getSchemeSpecificPart().endsWith(".fmu");
	}

	private static String getFmuName(File path) throws FmuInvocationException
	{
		if (path.getName().endsWith(".fmu"))
		{
			return path.getName().substring(0, path.getName().indexOf('.'));
		}
		return path.getName();

	}

	private void unPack() throws IOException
	{
		if (isArchive())
		{
			this.tmpFolder.mkdirs();
			logger.debug("Extracting: " + path.getAbsolutePath() + " to "
					+ tmpFolder.getAbsolutePath());
			ZipUtility.unzipApacheCompress(path, tmpFolder);
			logger.debug("Extracted '" + path.getAbsolutePath() + "' to '"
					+ tmpFolder.getAbsolutePath() + "'");
			unpacked = true;
		}
	}

	String getConfig() throws IOException
	{
		try
		{
			if (isArchive())
			{
				return correctFmuPath(FileUtils.readFileToString(Paths.get(this.tmpFolder.getPath(), "resources", "config.json").toFile()));
			} else
			{
				return correctFmuPath(FileUtils.readFileToString(Paths.get(URIUtil.schemeSpecificToLocal(uri).getPath(), "resources", "config.json").toFile()));
			}
		} catch (URISyntaxException e)
		{
			logger.error("failed to modify config", e);
			return null;
		}
	}

	private String correctFmuPath(String config)
			throws URISyntaxException, IOException
	{
		final ObjectMapper mapper = new ObjectMapper();
		URI base = uri;
		if (isArchive())
			base = this.tmpFolder.toURI();
		InitializationMsgJson st = mapper.readValue(config, InitializationMsgJson.class);
		for (Map.Entry<String, String> entry : st.fmus.entrySet())
		{
			URI u = new URI(entry.getValue());
			//if (u.getScheme() == null)
			{

				String p = u.getHost();
				if (!u.getPath().isEmpty())
				{
					if (p != null)
					{
						p = Paths.get(p, u.getPath()).toString();
					} else
					{
						p = u.getPath();
					}
				}

				String correctedPath = uriToPath(base);
				correctedPath = Paths.get(correctedPath, "resources", p).toString();
				if(System.getProperty("os.name").startsWith("Windows") && correctedPath.length() > 2 && correctedPath.charAt(1) == ':')
					correctedPath = "/" + correctedPath;
				if (u.getScheme() != null)
					correctedPath = u.getScheme() + ":" + correctedPath;
				entry.setValue(correctedPath.replace("\\","/"));
			}
		}

		return mapper.writeValueAsString(st);

	}

	/*
	 * (non-Javadoc)
	 * @see intocps.fmuapi.IFmu#load()
	 */
	@Override public void load() throws FmuInvocationException
	{
		logger.debug("Load FMU {}", path);
		if (loaded)
		{
			return;
		}

		if (!unpacked && isArchive())
		{
			try
			{
				unPack();
			} catch (IOException e)
			{
				throw new FmuInvocationException(e.getMessage());
			}
		}
	}

	@Override public void unLoad() throws FmiInvalidNativeStateException
	{
		if (isArchive())
		{
			logger.debug("UnLoad FMU {}, temp folder", path, tmpFolder);

			if (tmpFolder != null && tmpFolder.exists())
			{
				logger.debug("Deleting temp folder {}", tmpFolder);
				TempDirectory.delete(tmpFolder.toPath());
			}
		}
	}

	@Override public IFmiComponent instantiate(String s, String s1, boolean b,
			boolean b1, IFmuCallback iFmuCallback)
			throws XPathExpressionException, FmiInvalidNativeStateException
	{
		try
		{
			return new HierarchicalCoeComponent(this);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	@Override public String getVersion() throws FmiInvalidNativeStateException
	{
		return "";
	}

	@Override public String getTypesPlatform()
			throws FmiInvalidNativeStateException
	{
		return "";
	}

	@Override public InputStream getModelDescription() throws IOException
	{
		if (!isArchive())
		{
			byte[] bytes = IOUtils.toByteArray(new FileInputStream(Paths.get(URIUtil.schemeSpecificToLocal(uri).getAbsolutePath(), "modelDescription.xml").toFile()));
			return new ByteArrayInputStream(bytes);
		} else
		{

			if (!path.exists() || !path.canRead())
			{
				return null;
			}

			ZipFile zipFile = null;
			try
			{
				zipFile = new ZipFile(path);

				Enumeration<? extends ZipEntry> entries = zipFile.entries();

				while (entries.hasMoreElements())
				{
					ZipEntry entry = entries.nextElement();
					if (!entry.isDirectory()
							&& entry.getName().equals(MODEL_DESCRIPTION))
					{
						byte[] bytes = IOUtils.toByteArray(zipFile.getInputStream(entry));
						return new ByteArrayInputStream(bytes);
					}
				}
			} finally
			{
				if (zipFile != null)
				{
					zipFile.close();
				}
			}
			return null;
		}
	}

	@Override public boolean isValid()
	{
		return true;
	}
}
