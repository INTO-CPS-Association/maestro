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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.intocps.fmi.*;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created by kel on 08/08/2017.
 */
public class CsvInputStubComponent extends StubComponent
{
	private String inputFile = "";
	private Map<String, Long> nameToId = new HashMap<>();
	private Map<Long, PolynomialSplineFunction> refIdToFun = new HashMap<>();
	private double time = 0;

	public CsvInputStubComponent(IFmu fmu)
	{
		super(fmu);
		try
		{
			ModelDescription md = new ModelDescription(fmu.getModelDescription());

			for (ModelDescription.ScalarVariable scalarVariable : md.getOutputs())
			{
				nameToId.put(scalarVariable.getName(), scalarVariable.getValueReference());
			}

		} catch (ParserConfigurationException e)
		{
			e.printStackTrace();
		} catch (SAXException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (IllegalAccessException e)
		{
			e.printStackTrace();
		} catch (XPathExpressionException e)
		{
			e.printStackTrace();
		} catch (InvocationTargetException e)
		{
			e.printStackTrace();
		}
	}

	@Override public Fmi2Status setStrings(long[] longs, String[] strings)
			throws InvalidParameterException, FmiInvalidNativeStateException
	{
		for (String string : strings)
		{
			inputFile = string;
		}
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status exitInitializationMode()
			throws FmuInvocationException
	{
		Reader in = null;
		try
		{
			in = new FileReader(inputFile);
			CSVParser records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);

			Set<String> columns = records.getHeaderMap().keySet();

			Map<String, List<Double>> rows = new HashMap<>();

			for (Iterator<CSVRecord> iterator = records.iterator(); iterator.hasNext(); )
			{
				CSVRecord next = iterator.next();

				for (String c : columns)
				{
					List<Double> crow = rows.get(c);
					if (crow == null)
					{
						crow = new Vector<>();
						rows.put(c, crow);
					}
					crow.add(Double.parseDouble(next.get(c)));
				}
			}

			for (Map.Entry<String, Long> entry : nameToId.entrySet())
			{
				List<Double> r = rows.get(entry.getKey());
				List<Double> rt = new ArrayList<>(rows.get("time"));

				correctForDenseTime(rt, r);

				LinearInterpolator li = new LinearInterpolator(); // or other interpolator

				PolynomialSplineFunction psf = li.interpolate(ArrayUtils.toPrimitive(rt.toArray(new Double[rt.size()])), ArrayUtils.toPrimitive(r.toArray(new Double[r.size()])));

				refIdToFun.put(entry.getValue(), psf);

			}

			return Fmi2Status.OK;
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		} finally
		{
			if (in != null)
				IOUtils.closeQuietly(in);
		}
		return Fmi2Status.Fatal;
	}

	private void correctForDenseTime(List<Double> rt, List<Double> r)
	{
		for (int i = 0; i < rt.size(); i++)
		{
			 if(i+1 < rt.size() && rt.get(i).equals(rt.get(i+1)))
			 {
				 rt.remove(i);
				 r.remove(i);
				 i--;//re-check
			 }
			
		}
	}

	@Override public FmuResult<double[]> getReal(long[] longs)
			throws FmuInvocationException
	{

		double[] values = new double[longs.length];

		for (int i = 0; i < longs.length; i++)
		{
			values[i] = refIdToFun.get(longs[i]).value(time);
		}

		return new FmuResult<>(Fmi2Status.OK, values);
	}

	@Override public Fmi2Status doStep(double communicationPoint,
			double stepSize, boolean b) throws FmuInvocationException
	{
		double now = communicationPoint + stepSize;

		this.time = now;
		return Fmi2Status.OK;
	}

}
