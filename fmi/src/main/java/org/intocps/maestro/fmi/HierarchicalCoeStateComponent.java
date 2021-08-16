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
package org.intocps.maestro.fmi;

import org.intocps.fmi.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kel on 09/10/2017.
 */
public abstract class HierarchicalCoeStateComponent implements IFmiComponent
{
	protected final Map<Fmi2ModelDescription.ScalarVariable, Object> outputsSvToValue = new HashMap<>();
	protected final Map<Fmi2ModelDescription.ScalarVariable, Object> inputsSvToValue = new HashMap<>();

	protected Map<Long, Fmi2ModelDescription.ScalarVariable> refToSv = new HashMap<>();

	@Override public FmuResult<double[]> getReal(long[] longs)
			throws FmuInvocationException
	{
		double[] values = new double[longs.length];
		for (int i = 0; i < longs.length; i++)
		{
			values[i] = (double) outputsSvToValue.get(refToSv.get(longs[i]));
		}
		return new FmuResult<>(Fmi2Status.OK, values);
	}

	@Override public FmuResult<int[]> getInteger(long[] longs)
			throws FmuInvocationException
	{
		int[] values = new int[longs.length];
		for (int i = 0; i < longs.length; i++)
		{
			values[i] = (int) outputsSvToValue.get(refToSv.get(longs[i]));
		}
		return new FmuResult<>(Fmi2Status.OK, values);
	}

	@Override public FmuResult<boolean[]> getBooleans(long[] longs)
			throws FmuInvocationException
	{
		boolean[] values = new boolean[longs.length];
		for (int i = 0; i < longs.length; i++)
		{
			values[i] = (boolean) outputsSvToValue.get(refToSv.get(longs[i]));
		}
		return new FmuResult<>(Fmi2Status.OK, values);
	}

	@Override public FmuResult<String[]> getStrings(long[] longs)
			throws FmuInvocationException
	{
		String[] values = new String[longs.length];
		for (int i = 0; i < longs.length; i++)
		{
			values[i] = (String) outputsSvToValue.get(refToSv.get(longs[i]));
		}
		return new FmuResult<>(Fmi2Status.OK, values);
	}

	@Override public Fmi2Status setBooleans(long[] longs, boolean[] booleen)
			throws InvalidParameterException, FmiInvalidNativeStateException
	{
		for (int i = 0; i < longs.length; i++)
		{
			inputsSvToValue.put(refToSv.get(longs[i]), booleen[i]);
		}
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status setReals(long[] longs, double[] doubles)
			throws InvalidParameterException, FmiInvalidNativeStateException
	{
		for (int i = 0; i < longs.length; i++)
		{
			inputsSvToValue.put(refToSv.get(longs[i]), doubles[i]);
		}
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status setIntegers(long[] longs, int[] ints)
			throws InvalidParameterException, FmiInvalidNativeStateException
	{
		for (int i = 0; i < longs.length; i++)
		{
			inputsSvToValue.put(refToSv.get(longs[i]), ints[i]);
		}
		return Fmi2Status.OK;
	}

	@Override public Fmi2Status setStrings(long[] longs, String[] strings)
			throws InvalidParameterException, FmiInvalidNativeStateException
	{
		for (int i = 0; i < longs.length; i++)
		{
			inputsSvToValue.put(refToSv.get(longs[i]), strings[i]);
		}
		return Fmi2Status.OK;
	}


	@Override public Fmi2Status setRealInputDerivatives(long[] longs,
			int[] ints, double[] doubles) throws FmuInvocationException
	{
		return Fmi2Status.OK;
	}

	@Override public FmuResult<double[]> getRealOutputDerivatives(long[] longs,
			int[] ints) throws FmuInvocationException
	{
		return new FmuResult<>(Fmi2Status.Discard, null);
	}

	@Override public FmuResult<double[]> getDirectionalDerivative(long[] longs,
			long[] longs1, double[] doubles) throws FmuInvocationException
	{
		return new FmuResult<>(Fmi2Status.Discard, null);
	}


	@Override public FmuResult<IFmiComponentState> getState()
			throws FmuInvocationException
	{
		return new FmuResult<>(Fmi2Status.Error, null);
	}

	@Override public Fmi2Status setState(IFmiComponentState iFmiComponentState)
			throws FmuInvocationException
	{
		return Fmi2Status.Error;
	}

	@Override public Fmi2Status freeState(IFmiComponentState iFmiComponentState)
			throws FmuInvocationException
	{
		return Fmi2Status.Error;
	}
}
