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
package org.intocps.orchestration.coe.cosim.varstep.extrapolationerror;

public class ExtrapolationErrorEstimator
{

	private final Double FIRSTORDER_IIR_PAST_WEIGHT = 0.7;
	private final Double INITIAL_PREDICTION_ERROR = 0.0;

	private ExtrapolationErrorEstimator previousState = null;
	private Double filteredExtrapolationError = INITIAL_PREDICTION_ERROR;
	private Integer predictionOrder;
	private Double[] x = null;

	public ExtrapolationErrorEstimator(final Integer predictionOrder)
	{
		assert (FIRSTORDER_IIR_PAST_WEIGHT >= 0 && FIRSTORDER_IIR_PAST_WEIGHT <= 1);
		this.predictionOrder = predictionOrder;
	}

	public ExtrapolationErrorEstimator(final ExtrapolationErrorEstimator eee)
	{
		filteredExtrapolationError = eee.filteredExtrapolationError;
		predictionOrder = eee.predictionOrder;
		x = eee.x;
	}

	public Double getEstimate()
	{
		return filteredExtrapolationError;
	}

	public void update(final Double[] xNew, final Double prevStepsize)
	{
		previousState = new ExtrapolationErrorEstimator(this);
		final Double currentError = calcCurrentExtrapolationError(xNew[0], prevStepsize);
		if (currentError >= filteredExtrapolationError)
		{
			filteredExtrapolationError = currentError;
		} else
		{
			filteredExtrapolationError = FIRSTORDER_IIR_PAST_WEIGHT
					* filteredExtrapolationError
					+ (1 - FIRSTORDER_IIR_PAST_WEIGHT) * currentError;
		}
		x = xNew;
	}

	public void rollback()
	{
		filteredExtrapolationError = previousState.filteredExtrapolationError;
		x = previousState.x;
		previousState = null;
	}

	private Double calcCurrentExtrapolationError(final Double currentValue,
			final Double prevStepsize)
	{
		if (prevStepsize == null || x == null || x[1] == null)
		{
			return filteredExtrapolationError;
		}
		Double prediction = x[0] + x[1] * prevStepsize;
		if (predictionOrder == 2)
		{
			prediction += 0.5 * x[2] * Math.pow(prevStepsize, 2);
		}
		return Math.abs((currentValue - prediction)
				/ Math.max(Math.abs(currentValue), Double.MIN_VALUE));
	}

}
