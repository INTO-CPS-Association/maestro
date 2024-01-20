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

import org.apache.commons.io.IOUtils;
import org.intocps.fmi.*;

import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;

/**
 * Created by kel on 05/10/2017.
 */
public class HierarchicalExternalFmuStub implements IFmu
{
	private final URI uri;

	public HierarchicalExternalFmuStub(URI uri)
	{
		this.uri = uri;
	}

	@Override public void load() throws FmuInvocationException
	{

	}

	@Override public IFmiComponent instantiate(String s, String s1, boolean b,
			boolean b1, IFmuCallback iFmuCallback)
			throws XPathExpressionException, FmiInvalidNativeStateException
	{
		return new HierarchicalExternalFmuStubComponent(this);
	}

	@Override public void unLoad() throws FmiInvalidNativeStateException
	{

	}

	@Override public String getVersion() throws FmiInvalidNativeStateException
	{
		return null;
	}

	@Override public String getTypesPlatform()
			throws FmiInvalidNativeStateException
	{
		return null;
	}

	@Override public InputStream getModelDescription()
			throws IOException
	{

		byte[] bytes = IOUtils.toByteArray(new FileInputStream(Paths.get(HierarchicalCoeFmu.uriToPath(uri),  "modelDescription.xml").toFile()));
		return new ByteArrayInputStream(bytes);
	}

	@Override public boolean isValid()
	{
		return false;
	}
}
