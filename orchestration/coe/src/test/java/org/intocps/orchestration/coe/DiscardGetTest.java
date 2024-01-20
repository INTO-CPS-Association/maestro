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
import org.apache.commons.io.IOUtils;
import org.intocps.fmi.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by kel on 14/12/16.
 */
public class DiscardGetTest extends BasicTest
{

	@After
	public void cleanup()
	{
		FmuFactory.customFactory=null;

	}

	@Test public void testGetNull()
			throws IOException, NanoHTTPD.ResponseException
	{

		configureInstances(0, 0);
		try
		{
			test("/derivativeInOutTest/config.json", 0, 1);
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
	}

	protected void handleSimulateError(String simulateResponseData,
			NanoHTTPD.Response response)
	{

		Assert.assertEquals("Expected bad request", NanoHTTPD.Response.Status.BAD_REQUEST, response.getStatus());
		final String errorMsg = "Unable to obtain output value for tank.[level]";
		Assert.assertEquals("Expected",errorMsg,simulateResponseData);
	}

	private void configureInstances(double realValue, int integerValue)
	{
		FmuFactory.customFactory = new IFmuFactory()
		{
			@Override public boolean accept(URI uri)
			{
				return true;
			}

			@Override public IFmu instantiate(File sessionRoot, URI uri)
					throws Exception
			{
				IFmu fmu = mock(IFmu.class);
				when(fmu.isValid()).thenReturn(true);

				IFmiComponent comp = mock(IFmiComponent.class);
				when(fmu.instantiate(anyString(), anyString(), anyBoolean(), anyBoolean(), any())).thenReturn(comp);

				compMock(fmu, comp);

				String modelDescriptionPath;
				if (uri.toASCIIString().contains("watertank-c"))
				{
					modelDescriptionPath = "src/test/resources/minMaxTest/watertank-c/modelDescription.xml";

					when(comp.getReal(any())).thenReturn(new FmuResult<>(Fmi2Status.Discard, new double[] {
							0 }));

					when(comp.getInteger(new long[] {
							6 })).thenReturn(new FmuResult<>(Fmi2Status.OK, new int[] {
							integerValue }));

				} else
				{
					modelDescriptionPath = "src/test/resources/minMaxTest/watertankcontroller-c/modelDescription.xml";
					when(comp.getBooleans(new long[] {
							4 })).thenReturn(new FmuResult<>(Fmi2Status.OK, new boolean[] {
							true }));

				}

				final InputStream md = new ByteArrayInputStream(IOUtils.toByteArray(new File(modelDescriptionPath.replace('/', File.separatorChar)).toURI()));
				when(fmu.getModelDescription()).thenReturn(md);
				return fmu;
			}

		};
	}

	private void compMock(IFmu fmu, IFmiComponent comp)
			throws FmuInvocationException, InvalidParameterException
	{

		when(comp.getFmu()).thenReturn(fmu);

		//		Fmi2Status setDebugLogging(boolean var1, String[] var2) throws FmuInvocationException;
		when(comp.setDebugLogging(anyBoolean(), any())).thenReturn(Fmi2Status.OK);

		//		Fmi2Status setupExperiment(boolean var1, double var2, double var4, boolean var6, double var7) throws FmuInvocationException;
		when(comp.setupExperiment(anyBoolean(), anyDouble(), anyDouble(), anyBoolean(), anyDouble())).thenReturn(Fmi2Status.OK);
		//		Fmi2Status enterInitializationMode() throws FmuInvocationException;
		when(comp.enterInitializationMode()).thenReturn(Fmi2Status.OK);
		//		Fmi2Status exitInitializationMode() throws FmuInvocationException;
		when(comp.exitInitializationMode()).thenReturn(Fmi2Status.OK);
		//		Fmi2Status reset() throws FmuInvocationException;
		//		when(comp.reset());
		//		Fmi2Status setRealInputDerivatives(long[] var1, int[] var2, double[] var3) throws FmuInvocationException;
		//when(comp.setRealInputDerivatives(any(), any(), any())).thenReturn(Fmi2Status.OK);
		//		FmuResult<double[]> getRealOutputDerivatives(long[] var1, int[] var2) throws FmuInvocationException;
		//
		//		FmuResult<double[]> getDirectionalDerivative(long[] var1, long[] var2, double[] var3) throws FmuInvocationException;
		//
		//		Fmi2Status doStep(double var1, double var3, boolean var5) throws FmuInvocationException;
		when(comp.doStep(anyDouble(), anyDouble(), anyBoolean())).thenReturn(Fmi2Status.OK);
		//		FmuResult<double[]> getReal(long[] var1) throws FmuInvocationException;
		//
		//		FmuResult<int[]> getInteger(long[] var1) throws FmuInvocationException;
		//
		//		FmuResult<boolean[]> getBooleans(long[] var1) throws FmuInvocationException;
		//
		//		FmuResult<String[]> getStrings(long[] var1) throws FmuInvocationException;
		//
		//		Fmi2Status setBooleans(long[] var1, boolean[] var2) throws InvalidParameterException, FmiInvalidNativeStateException;
		when(comp.setBooleans(any(), any())).thenReturn(Fmi2Status.OK);
		//		Fmi2Status setReals(long[] var1, double[] var2) throws InvalidParameterException, FmiInvalidNativeStateException;
		when(comp.setReals(any(), any())).thenReturn(Fmi2Status.OK);
		//		Fmi2Status setIntegers(long[] var1, int[] var2) throws InvalidParameterException, FmiInvalidNativeStateException;
		when(comp.setIntegers(any(), any())).thenReturn(Fmi2Status.OK);
		//		Fmi2Status setStrings(long[] var1, String[] var2) throws InvalidParameterException, FmiInvalidNativeStateException;
		when(comp.setStrings(any(), any())).thenReturn(Fmi2Status.OK);
		//		FmuResult<Boolean> getBooleanStatus(Fmi2StatusKind var1) throws FmuInvocationException;
		//
		//		FmuResult<Fmi2Status> getStatus(Fmi2StatusKind var1) throws FmuInvocationException;
		//
		//		FmuResult<Integer> getIntegerStatus(Fmi2StatusKind var1) throws FmuInvocationException;
		//
		//		FmuResult<Double> getRealStatus(Fmi2StatusKind var1) throws FmuInvocationException;
		//
		//		FmuResult<String> getStringStatus(Fmi2StatusKind var1) throws FmuInvocationException;
		//
		//		Fmi2Status terminate() throws FmuInvocationException;
		when(comp.terminate()).thenReturn(Fmi2Status.OK);
		//		void freeInstance() throws FmuInvocationException;
		//		when(comp.freeInstance());
		//		FmuResult<IFmiComponentState> getState() throws FmuInvocationException;
		//
		//		Fmi2Status setState(IFmiComponentState var1) throws FmuInvocationException;
		//
		//		Fmi2Status freeState(IFmiComponentState var1) throws FmuInvocationException;
		//
		//		boolean isValid();
		when(comp.isValid()).thenReturn(true);
		//		FmuResult<Double> getMaxStepSize() throws FmiInvalidNativeStateException;
		when(comp.getMaxStepSize()).thenReturn(new FmuResult<Double>(Fmi2Status.Discard, 0.0));
	}
}
