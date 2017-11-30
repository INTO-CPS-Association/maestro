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
package org.intocps.orchestration.coe.cosim.varstep.constraint.zerocrossing.stepsize;

import org.intocps.orchestration.coe.cosim.varstep.valuetracker.OptionalDifferenceTracker;

public class ZerocrossingPredictor
{

	private OptionalDifferenceTracker tracker;
	private Double safety;

	public ZerocrossingPredictor(final OptionalDifferenceTracker tracker,
			final Double safety)
	{
		this.tracker = tracker;
		this.safety = safety;
	}

	public Double estimateNStepsToZerocrossing(final Double prevStepsize)
	{
		final Double nStepsExtrapolated = extrapolateNStepsToZerocrossing(prevStepsize);
		final Double estimatedError = tracker.getExtrapolationErrorEstimate();
		return nStepsExtrapolated / (1 + estimatedError + safety);
	}

	private Double extrapolateNStepsToZerocrossing(final Double prevStepsize)
	{

		final Double currDistance = tracker.getCurrentValue();
		if (currDistance == null || currDistance == Double.MAX_VALUE)
		{
			return Double.MAX_VALUE;
		}

		final Integer predictionOrder = tracker.getPredictionOrder();

		if (predictionOrder == 1)
		{
			return calcFirstOrderExtrapolation();
		}

		if (predictionOrder == 2)
		{
			return calcSecondOrderExtrapolation(prevStepsize);
		}

		throw new IllegalStateException("Unreachable code");
	}

	private Double calcFirstOrderExtrapolation()
	{
		final Double currDistance = tracker.getCurrentValue();
		final Double prevDistance = tracker.getPreviousValue();
		if (prevDistance == null || prevDistance == Double.MAX_VALUE)
		{
			return Double.MAX_VALUE;
		}
		final Double firstOrderEstimate = Math.abs(currDistance)
				/ Math.max(Double.MIN_VALUE, Math.abs(currDistance
						- prevDistance));
		return firstOrderEstimate;
	}

	private Double calcSecondOrderExtrapolation(final Double prevStepsize)
	{
		final Double x = tracker.getCurrentValue();
		// hidden in the next two lines is the sum rule in differentiation
		final Double xdot = tracker.getFirstDerivative();
		 Double xdotdot = tracker.getSecondDerivative();

		//See doc sec 2.3.1 Extrapolation f(t+deltaT) = f(t) + f'(t) deltaT + 0.5 f''(t)  deltaT^2
		//Solve using second order function if xdotdot is different from 0

		if(xdotdot==0)
		{
			return calcFirstOrderExtrapolation();
		}

		final Double p = 2 * xdot / xdotdot;
		final Double q = 2 * x / xdotdot;

		final Double d = p * p / 4 - q;
		if (d < 0)
		{
			return Double.MAX_VALUE;
		}
		if (d == 0)
		{
			final Double t = -p / 2;
			if (t == 0)
			{
				return Double.MIN_VALUE;
			}
			if (t < 0)
			{
				return Double.MAX_VALUE;
			}
			return t / prevStepsize;
		}
		final Double sqrtD = Math.sqrt(d);
		final Double t1 = -p / 2 - sqrtD;
		final Double t2 = -p / 2 + sqrtD;

		//select solution and convert to number of steps
		if (t1 > 0)
		{
			return t1 / prevStepsize;
		}
		if (t2 > 0)
		{
			return t2 / prevStepsize;
		}
		return Double.MAX_VALUE;
	}

}
