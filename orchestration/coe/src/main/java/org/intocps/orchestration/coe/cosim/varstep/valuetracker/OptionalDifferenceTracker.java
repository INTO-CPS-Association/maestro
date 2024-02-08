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
package org.intocps.orchestration.coe.cosim.varstep.valuetracker;

import org.intocps.orchestration.coe.config.ModelConnection.Variable;

import java.util.List;
import java.util.Observable;

public class OptionalDifferenceTracker implements ValueTracker
{
	private ValueTracker value1 = null;
	private ValueTracker value2 = null;
	private Boolean isDefinedByDifference;

	public OptionalDifferenceTracker(final Observable observable,
			final List<Variable> variables, final Integer predictionOrder)
	{
		isDefinedByDifference = variables.size() == 2;
		value1 = new DoubleValueTracker(observable, variables.get(0), predictionOrder);
		if (isDefinedByDifference)
		{
			value2 = new DoubleValueTracker(observable, variables.get(1), predictionOrder);
		}
	}

	public OptionalDifferenceTracker(final Observable observable,
			final Variable variable, final Integer predictionOrder)
	{
		value1 = new DoubleValueTracker(observable, variable, predictionOrder);
		isDefinedByDifference = false;
	}

	@Override
	public Double getNextValue()
	{
		return isDefinedByDifference ? value1.getNextValue()
				- value2.getNextValue() : value1.getNextValue();
	}

	@Override
	public Double getCurrentValue()
	{
		return isDefinedByDifference ? value1.getCurrentValue()
				- value2.getCurrentValue() : value1.getCurrentValue();
	}

	@Override
	public Double getPreviousValue()
	{
		if (value1.getPreviousValue() == null)
		{
			return null;
		}
		return isDefinedByDifference ? value1.getPreviousValue()
				- value2.getPreviousValue() : value1.getPreviousValue();
	}

	@Override
	public Double getPrevPrevValue()
	{
		if (value1.getPrevPrevValue() == null)
		{
			return null;
		}
		return isDefinedByDifference ? value1.getPrevPrevValue()
				- value2.getPrevPrevValue() : value1.getPrevPrevValue();
	}

	@Override
	public Double getFirstDerivative()
	{
		return isDefinedByDifference ? value1.getFirstDerivative()
				- value2.getFirstDerivative() : value1.getFirstDerivative();
	}

	@Override
	public Double getSecondDerivative()
	{
		return isDefinedByDifference ? value1.getSecondDerivative()
				- value2.getSecondDerivative() : value1.getSecondDerivative();
	}

	@Override
	public Double getExtrapolationErrorEstimate()
	{
		return isDefinedByDifference ? value1.getExtrapolationErrorEstimate()
				- value2.getExtrapolationErrorEstimate()
				: value1.getExtrapolationErrorEstimate();
	}

	@Override
	public Integer getPredictionOrder()
	{
		return value1.getPredictionOrder();
	}

}
