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
package org.intocps.maestro.interpreter.values.variablestep.valuetracker;

import java.util.Observable;
import java.util.Observer;


import org.intocps.maestro.framework.fmi2.ModelConnection;
import org.intocps.maestro.interpreter.values.variablestep.CurrentSolutionPoint;
import org.intocps.maestro.interpreter.values.variablestep.CurrentSolutionPoint.Operation;
import org.intocps.maestro.interpreter.values.derivativeestimator.ScalarDerivativeEstimator;
import org.intocps.maestro.interpreter.values.variablestep.extrapolationerror.ExtrapolationErrorEstimator;

public class DoubleValueTracker implements Observer, ValueTracker
{

	private DoubleValueTracker previousState = null;
	private ModelConnection.Variable trackedVariable;
	private Double xNext = null;
	private Double x = null;
	private Double xPrev = null;
	private Double xPrevPrev = null;
	private ScalarDerivativeEstimator derivativeEstimator;
	private ExtrapolationErrorEstimator extrapolationErrorEstimator;
	private Integer order;

	public DoubleValueTracker(final DoubleValueTracker dvt)
	{
		trackedVariable = dvt.trackedVariable;
		xNext = dvt.xNext;
		x = dvt.x;
		xPrev = dvt.xPrev;
		xPrevPrev = dvt.xPrevPrev;
		derivativeEstimator = dvt.derivativeEstimator;
		extrapolationErrorEstimator = dvt.extrapolationErrorEstimator;
		order = dvt.order;
	}

	public DoubleValueTracker(final Observable observable,
			final ModelConnection.Variable variable, final Integer predictionOrder)
	{
		observable.addObserver(this);
		trackedVariable = variable;
		order = predictionOrder;
		derivativeEstimator = new ScalarDerivativeEstimator(predictionOrder);
		extrapolationErrorEstimator = new ExtrapolationErrorEstimator(predictionOrder);
	}

	@Override
	public void update(final Observable obs, final Object arg)
	{
		if (obs instanceof CurrentSolutionPoint)
		{
			CurrentSolutionPoint cs = (CurrentSolutionPoint) obs;
			final Operation op = cs.getOperation();

			if (Operation.ADVANCE.equals(op))
			{
				previousState = new DoubleValueTracker(this);
				xPrevPrev = xPrev;
				xPrev = x;
				x = cs.getDoubleValue(trackedVariable);
				xNext = null;
				final Double firstDerivative = cs.getDerivative(trackedVariable, 1);
				final Double secondDerivative = cs.getDerivative(trackedVariable, 2);
				final Double[] xVec = { x, firstDerivative, secondDerivative };
				final Double prevStepsize = cs.getPrevStepsize();
				derivativeEstimator.advance(xVec, prevStepsize);
				extrapolationErrorEstimator.update(xVec, prevStepsize);
			}

			if (Operation.PEEK.equals(op))
			{
				xNext = cs.getNextDoubleValue(trackedVariable);
			}

			if (Operation.ROLLBACK.equals(op))
			{
				xNext = previousState.xNext;
				x = previousState.x;
				xPrev = previousState.xPrev;
				xPrevPrev = previousState.xPrevPrev;
				previousState = null;
				derivativeEstimator.rollback();
				extrapolationErrorEstimator.rollback();
			}

		}
	}

	@Override
	public Integer getPredictionOrder()
	{
		return order;
	}

	@Override
	public Double getNextValue()
	{
		return xNext;
	}

	@Override
	public Double getCurrentValue()
	{
		return x;
	}

	@Override
	public Double getPreviousValue()
	{
		return xPrev;
	}

	@Override
	public Double getPrevPrevValue()
	{
		return xPrevPrev;
	}

	@Override
	public Double getFirstDerivative()
	{
		return derivativeEstimator.getFirstDerivative();
	}

	@Override
	public Double getSecondDerivative()
	{
		return derivativeEstimator.getSecondDerivative();
	}

	@Override
	public Double getExtrapolationErrorEstimate()
	{
		return extrapolationErrorEstimator.getEstimate();
	}

}
