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
package org.intocps.orchestration.coe.single;

import org.intocps.fmi.*;
import org.intocps.orchestration.coe.FmuFactory;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

/**
 * Created by kel on 17/12/16.
 */
public class StubFmu implements IFmu
{

	final String modelDescription;

	public StubFmu(URI otherFmu) throws Exception
	{
		URI fmuUri = new URI(otherFmu.toASCIIString().replace("stub", "file"));
		IFmu fmu = FmuFactory.create(null, fmuUri);

		ModelDescription md = new ModelDescription(fmu.getModelDescription());
		this.modelDescription = createModelDescriptionFrom(md);
	}

	private String createModelDescriptionFrom(ModelDescription md)
			throws IllegalAccessException, XPathExpressionException,
			InvocationTargetException
	{
		String name = "inverted-" + md.getModelId();
		final StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
		sb.append(String.format("<fmiModelDescription fmiVersion=\"2.0\" modelName=\"%s\" guid=\"{00000000-0000-0000-0000-000000000000}\" numberOfEventIndicators=\"0\">\n", name));
		sb.append(String.format("<CoSimulation modelIdentifier=\"%s\" canHandleVariableCommunicationStepSize=\"true\" canGetAndSetFMUstate=\"false\" maxOutputDerivativeOrder=\"0\"   />\n", name));

		sb.append("<ModelVariables>\n");

		int index = 0;

		final String SV_TEMPLATE = "<ScalarVariable name=\"%s\" valueReference=\"%d\" causality=\"input\" variability=\"%s\"><%s start=\"%s\" />	</ScalarVariable>\n";

		for (ModelDescription.ScalarVariable scalarVariable : md.getOutputs())
		{

			String type = "Real";
			String startValue = "0";
			String variability = ModelDescription.Variability.Discrete.name().toLowerCase();

			switch (scalarVariable.type.type)
			{
				case Boolean:
					type = "Boolean";
					startValue = "false";
					break;
				case Real:
					type = "Real";
					startValue = "0";
					variability = ModelDescription.Variability.Continuous.name().toLowerCase();
					break;
				case Integer:
				case Enumeration:
					type = "Integer";
					startValue = "0";
					break;
				case String:
					type = "String";
					startValue = "";
					break;

			}

				sb.append(String.format(SV_TEMPLATE, scalarVariable.getName(), index++, variability, type, startValue));
		}
		if(md.getOutputs().isEmpty())
			sb.append(String.format(SV_TEMPLATE, "internal-not-used", index++, ModelDescription.Variability.Continuous.name().toLowerCase(), "Real", "0"));

		sb.append("</ModelVariables>\n");

		sb.append("<ModelStructure></ModelStructure>\n");
		sb.append("</fmiModelDescription>");
		return sb.toString();
	}

	@Override public void load() throws FmuInvocationException
	{

	}

	@Override public IFmiComponent instantiate(String s, String s1, boolean b,
			boolean b1, IFmuCallback iFmuCallback)
			throws XPathExpressionException, FmiInvalidNativeStateException
	{
		return new StubComponent(this);
	}

	@Override public void unLoad() throws FmiInvalidNativeStateException
	{

	}

	@Override public String getVersion() throws FmiInvalidNativeStateException
	{
		return "2.0";
	}

	@Override public String getTypesPlatform()
			throws FmiInvalidNativeStateException
	{
		return "";
	}

	@Override public InputStream getModelDescription() throws IOException
	{
		return new ByteArrayInputStream(this.modelDescription.getBytes("UTF-8"));
	}

	@Override public boolean isValid()
	{
		return true;
	}
}
