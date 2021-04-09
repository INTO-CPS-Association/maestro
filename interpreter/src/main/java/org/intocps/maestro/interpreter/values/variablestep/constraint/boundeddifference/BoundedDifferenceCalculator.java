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
*		Oliver Kotte
*		Alexander Kluber
*		Kenneth Lausdahl
*		Casper Thule
*/

/*
* Author:
*		Kenneth Lausdahl
*		Casper Thule
*/
package org.intocps.maestro.interpreter.values.variablestep.constraint.boundeddifference;

import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Vector;


import org.intocps.maestro.framework.fmi2.ModelConnection;
import org.intocps.maestro.interpreter.values.variablestep.valuetracker.DoubleValueTracker;

public class BoundedDifferenceCalculator
{
	private List<DoubleValueTracker> tracker = new Vector<DoubleValueTracker>();
	private Boolean isDefinedBySinglePort;

	public BoundedDifferenceCalculator(final Observable observable,
			final List<ModelConnection.Variable> variables)
	{
		isDefinedBySinglePort = variables.size() == 1;
		for (ModelConnection.Variable variable : variables)
		{
			tracker.add(new DoubleValueTracker(observable, variable, 1));
		}
	}

	public Double getAbsoluteDifference()
	{
		if (isDefinedBySinglePort)
		{
			final Double currentValue = tracker.get(0).getCurrentValue();
			Double previousValue = tracker.get(0).getPreviousValue();
			if (!currentValue.isNaN() && previousValue==null)
			{
				previousValue = currentValue;
			}
			return currentValue - previousValue;
		} else
		{
			final List<Double> currentValues = getCurrentPortValues();
			return Collections.max(currentValues)
					- Collections.min(currentValues);
		}
	}

	public Double getNextAbsoluteDifference()
	{
		if (isDefinedBySinglePort)
		{
			final Double currentValue = tracker.get(0).getCurrentValue();
			final Double nextValue = tracker.get(0).getNextValue();
			return nextValue - currentValue;
		} else
		{
			final List<Double> nextValues = getNextPortValues();
			return Collections.max(nextValues) - Collections.min(nextValues);
		}
	}

	public Double getRelativeDifference()
	{
		if (isDefinedBySinglePort)
		{
			final Double currentValue = tracker.get(0).getCurrentValue();
			Double previousValue = tracker.get(0).getPreviousValue();
			if (!currentValue.isNaN() && previousValue==null)
			{
				previousValue = currentValue;
			}
			return getAbsoluteDifference()
					/ Math.max(Math.max(Math.abs(currentValue), Math.abs(previousValue)), Double.MIN_VALUE);
		} else
		{
			final List<Double> currentValues = getCurrentPortValues();
			return getAbsoluteDifference()
					/ Math.max(Math.max(Collections.max(currentValues), Math.abs(Collections.min(currentValues))), Double.MIN_VALUE);
		}
	}

	public Double getNextRelativeDifference()
	{
		if (isDefinedBySinglePort)
		{
			final Double currentValue = tracker.get(0).getCurrentValue();
			final Double nextValue = tracker.get(0).getNextValue();
			return getNextAbsoluteDifference()
					/ Math.max(Math.max(Math.abs(currentValue), Math.abs(nextValue)), Double.MIN_VALUE);
		} else
		{
			final List<Double> nextValues = getNextPortValues();
			return getNextAbsoluteDifference()
					/ Math.max(Math.max(Collections.max(nextValues), Math.abs(Collections.min(nextValues))), Double.MIN_VALUE);
		}
	}

	private List<Double> getCurrentPortValues()
	{
		final List<Double> currentValues = new Vector<Double>();
		for (DoubleValueTracker t : tracker)
		{
			currentValues.add(t.getCurrentValue());
		}
		return currentValues;
	}

	private List<Double> getNextPortValues()
	{
		final List<Double> nextValues = new Vector<Double>();
		for (DoubleValueTracker t : tracker)
		{
			nextValues.add(t.getNextValue());
		}
		return nextValues;
	}

}
