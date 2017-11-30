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

/**
 * Created by kel on 17/12/16.
 */
public class StubComponent implements IFmiComponent
{

	private IFmu fmu;

	public StubComponent(IFmu fmu)
	{
		this.fmu = fmu;
	}

	@Override public IFmu getFmu()
	{
		return this.fmu;
	}

	@Override public Fmi2Status setDebugLogging(boolean b, String[] strings)
			throws FmuInvocationException
	{
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status setupExperiment(boolean b, double v, double v1,
			boolean b1, double v2) throws FmuInvocationException
	{
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status enterInitializationMode()
			throws FmuInvocationException
	{
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status exitInitializationMode()
			throws FmuInvocationException
	{
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status reset() throws FmuInvocationException
	{
		return null;
	}

	@Override public Fmi2Status setRealInputDerivatives(long[] longs,
			int[] ints, double[] doubles) throws FmuInvocationException
	{
		return Fmi2Status.OK;
	}

	@Override public FmuResult<double[]> getRealOutputDerivatives(long[] longs,
			int[] ints) throws FmuInvocationException
	{
		 return new FmuResult<>( Fmi2Status.Error,null);
	}

	@Override public FmuResult<double[]> getDirectionalDerivative(long[] longs,
			long[] longs1, double[] doubles) throws FmuInvocationException
	{
		return new FmuResult<>( Fmi2Status.Error,null);
	}

	@Override public Fmi2Status doStep(double v, double v1, boolean b)
			throws FmuInvocationException
	{
		return Fmi2Status.OK;
	}

	@Override public FmuResult<double[]> getReal(long[] longs)
			throws FmuInvocationException
	{
		return new FmuResult<>( Fmi2Status.Error,null);
	}

	@Override public FmuResult<int[]> getInteger(long[] longs)
			throws FmuInvocationException
	{
		return new FmuResult<>( Fmi2Status.Error,null);
	}

	@Override public FmuResult<boolean[]> getBooleans(long[] longs)
			throws FmuInvocationException
	{
		return new FmuResult<>( Fmi2Status.Error,null);
	}

	@Override public FmuResult<String[]> getStrings(long[] longs)
			throws FmuInvocationException
	{
		return new FmuResult<>( Fmi2Status.Error,null);
	}

	@Override public Fmi2Status setBooleans(long[] longs, boolean[] booleen)
			throws InvalidParameterException, FmiInvalidNativeStateException
	{
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status setReals(long[] longs, double[] doubles)
			throws InvalidParameterException, FmiInvalidNativeStateException
	{
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status setIntegers(long[] longs, int[] ints)
			throws InvalidParameterException, FmiInvalidNativeStateException
	{
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status setStrings(long[] longs, String[] strings)
			throws InvalidParameterException, FmiInvalidNativeStateException
	{
		return Fmi2Status.OK;
	}

	@Override public FmuResult<Boolean> getBooleanStatus(
			Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException
	{
		return new FmuResult<>( Fmi2Status.Error,null);
	}

	@Override public FmuResult<Fmi2Status> getStatus(
			Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException
	{
		return new FmuResult<>( Fmi2Status.Error,null);
	}

	@Override public FmuResult<Integer> getIntegerStatus(
			Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException
	{
		return new FmuResult<>( Fmi2Status.Error,null);
	}

	@Override public FmuResult<Double> getRealStatus(
			Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException
	{
		return new FmuResult<>( Fmi2Status.Error,null);
	}

	@Override public FmuResult<String> getStringStatus(
			Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException
	{
		return new FmuResult<>( Fmi2Status.Error,null);
	}

	@Override public Fmi2Status terminate() throws FmuInvocationException
	{
		return Fmi2Status.OK;
	}

	@Override public void freeInstance() throws FmuInvocationException
	{

	}

	@Override public FmuResult<IFmiComponentState> getState()
			throws FmuInvocationException
	{
		return new FmuResult<>( Fmi2Status.Error,null);
	}

	@Override public Fmi2Status setState(IFmiComponentState iFmiComponentState)
			throws FmuInvocationException
	{
		return Fmi2Status.Discard;
	}

	@Override public Fmi2Status freeState(IFmiComponentState iFmiComponentState)
			throws FmuInvocationException
	{
		return Fmi2Status.Discard;
	}

	@Override public boolean isValid()
	{
		return true;
	}

	@Override public FmuResult<Double> getMaxStepSize()
			throws FmiInvalidNativeStateException
	{
		return new FmuResult<>( Fmi2Status.Discard,null);
	}
}
