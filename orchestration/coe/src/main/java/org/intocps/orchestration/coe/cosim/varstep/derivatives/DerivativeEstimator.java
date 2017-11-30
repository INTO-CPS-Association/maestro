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
package org.intocps.orchestration.coe.cosim.varstep.derivatives;

import java.util.HashMap;
import java.util.Map;

import org.intocps.orchestration.coe.config.ModelConnection.ModelInstance;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.ScalarVariable;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.Types;

public class DerivativeEstimator
{

	private static final Integer ESTIMATIONORDER = 2;

	private Map<ModelInstance, Map<ScalarVariable, ScalarDerivativeEstimator>> estimators = null;

	public DerivativeEstimator()
	{

	}



	public Map<ModelInstance, Map<ScalarVariable, Map<Integer, Double>>> advance(
			final Map<ModelInstance, Map<ScalarVariable, Object>> readValues,
			final Map<ModelInstance, Map<ScalarVariable, Map<Integer, Double>>> readDerivatives,
			final Double stepsize)
	{
		final Map<ModelInstance, Map<ScalarVariable, Map<Integer, Double>>> derivatives = new HashMap<>();

		if (estimators == null)
		{
			initialize(readValues);
		}
		HashMap<ScalarVariable, Map<Integer, Double>> scalarVariablesState;

		for (ModelInstance mi : readValues.keySet())
		{
			scalarVariablesState = null;

			final Map<ScalarVariable, Object> svo = readValues.get(mi);
			for (ScalarVariable sv : svo.keySet())
			{

				if (Types.Real.equals(sv.getType().type))
				{
					if (scalarVariablesState == null)
					{
						scalarVariablesState = new HashMap<>();
						derivatives.put(mi, scalarVariablesState);
					}
					HashMap<Integer, Double> calculatedDerivatives = new HashMap<>();
					scalarVariablesState.put(sv, calculatedDerivatives);

					Double[] x = getReadValueAndDerivatives(readValues, readDerivatives, mi, sv);
					final ScalarDerivativeEstimator estimator = estimators.get(mi).get(sv);
					estimator.advance(x, stepsize);
					for (Integer order = 1; order <= ESTIMATIONORDER; order++)
					{
						calculatedDerivatives.put(order, estimator.getDerivative(order));
					}
				}
			}
		}

		return derivatives;
	}

	public Map<ModelInstance, Map<Integer, Map<ScalarVariable, Double>>> rollback()
	{
		final Map<ModelInstance, Map<Integer, Map<ScalarVariable, Double>>> derivatives = new HashMap<ModelInstance, Map<Integer, Map<ScalarVariable, Double>>>();
		for (ModelInstance mi : estimators.keySet())
		{
			final Map<Integer, Map<ScalarVariable, Double>> isvd = new HashMap<Integer, Map<ScalarVariable, Double>>();
			for (Integer order = 1; order <= ESTIMATIONORDER; order++)
			{
				final Map<ScalarVariable, Double> svd = new HashMap<ScalarVariable, Double>();
				for (ScalarVariable sv : estimators.get(mi).keySet())
				{
					final ScalarDerivativeEstimator estimator = estimators.get(mi).get(sv);
					if (Types.Real.equals(sv.getType().type))
					{
						estimator.rollback();
						svd.put(sv, estimator.getDerivative(order));
					} else
					{
						svd.put(sv, null);
					}
				}
				isvd.put(order, svd);
			}
			derivatives.put(mi, isvd);
		}
		return derivatives;
	}

	private Double[] getReadValueAndDerivatives(
			final Map<ModelInstance, Map<ScalarVariable, Object>> readValues,
			final Map<ModelInstance, Map<ScalarVariable, Map<Integer, Double>>> readDerivatives,
			ModelInstance mi, ScalarVariable sv)
	{
		Double[] x = new Double[] { null, null, null };
		x[0] = (Double) readValues.get(mi).get(sv);
		for (Integer order = 1; order <= ESTIMATIONORDER; order++)
		{
			final Map<ScalarVariable, Map<Integer, Double>> derMi = readDerivatives.get(mi);
			if (derMi == null)
				break;
			final Map<Integer, Double> orderDer = derMi.get(sv);
			if (orderDer == null)
				break;
			final Double derVal = orderDer.get(order);
			if (derVal == null)
				break;
			x[order] = derVal;
		}
		return x;
	}

	private void initialize(
			final Map<ModelInstance, Map<ScalarVariable, Object>> currentValues)
	{
		estimators = new HashMap<ModelInstance, Map<ScalarVariable, ScalarDerivativeEstimator>>();
		for (ModelInstance mi : currentValues.keySet())
		{
			final Map<ScalarVariable, Object> svo = currentValues.get(mi);
			final Map<ScalarVariable, ScalarDerivativeEstimator> svde = new HashMap<ScalarVariable, ScalarDerivativeEstimator>();
			for (ScalarVariable sv : svo.keySet())
			{
				ScalarDerivativeEstimator de = null;
				if (Types.Real.equals(sv.getType().type))
				{
					de = new ScalarDerivativeEstimator(ESTIMATIONORDER);
				}
				svde.put(sv, de);
			}
			estimators.put(mi, svde);
		}
	}

}
