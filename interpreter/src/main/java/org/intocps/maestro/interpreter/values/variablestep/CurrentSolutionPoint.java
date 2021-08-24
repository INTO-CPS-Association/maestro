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
package org.intocps.maestro.interpreter.values.variablestep;

import org.intocps.maestro.framework.fmi2.ModelConnection;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import static org.intocps.maestro.fmi.Fmi2ModelDescription.*;

public class CurrentSolutionPoint extends Observable
{

	public enum Operation
	{
		ADVANCE, PEEK, ROLLBACK
	}

	private CurrentSolutionPoint previousState = null;
	private Double currentTime;
	private Double nextTime;
	private Double prevStepsize;
	private Double lastStepsizeLimitedByContinuousConstraint;
	private Boolean wasLastStepsizeLimitedByDiscreteConstraint;
	private Map<ModelConnection.ModelInstance, Map<ScalarVariable, Object>> currentValues;
	private Map<ModelConnection.ModelInstance, Map<ScalarVariable, Object>> nextValues;
	private Map<ModelConnection.ModelInstance, Map<ScalarVariable, Map<Integer, Double>>> currentDerivatives;
	private Operation operation;

	public CurrentSolutionPoint()
	{

	}

	public CurrentSolutionPoint(final CurrentSolutionPoint cs)
	{
		currentTime = cs.currentTime;
		nextTime = cs.nextTime;
		prevStepsize = cs.prevStepsize;
		lastStepsizeLimitedByContinuousConstraint = cs.lastStepsizeLimitedByContinuousConstraint;
		wasLastStepsizeLimitedByDiscreteConstraint = cs.wasLastStepsizeLimitedByDiscreteConstraint;
		currentValues = cs.currentValues;
		currentDerivatives = cs.currentDerivatives;
		nextValues = cs.nextValues;
	}

	public void advance(
			final Double currentTime,
			final Map<ModelConnection.ModelInstance, Map<ScalarVariable, Object>> currentValues,
			final Map<ModelConnection.ModelInstance, Map<ScalarVariable, Map<Integer, Double>>> currentDerivatives,
			final Double prevStepsize,
			final Boolean wasStepsizeLimitedByDiscreteConstraint)
	{
		previousState = new CurrentSolutionPoint(this);
		operation = Operation.ADVANCE;
		this.currentTime = currentTime;
		this.currentValues = currentValues;
		this.currentDerivatives = currentDerivatives;
		this.prevStepsize = prevStepsize;
		this.wasLastStepsizeLimitedByDiscreteConstraint = wasStepsizeLimitedByDiscreteConstraint;
		if (!wasStepsizeLimitedByDiscreteConstraint)
		{
			lastStepsizeLimitedByContinuousConstraint = prevStepsize;
		}
		setChanged();
		notifyObservers();
	}

	public void peek(final Double nextTime,
			final Map<ModelConnection.ModelInstance, Map<ScalarVariable, Object>> nextValues)
	{
		operation = Operation.PEEK;
		this.nextValues = nextValues;
		this.nextTime = nextTime;
		setChanged();
		notifyObservers();
	}

	public void rollback()
	{
		operation = Operation.ROLLBACK;
		currentTime = previousState.currentTime;
		prevStepsize = previousState.prevStepsize;
		lastStepsizeLimitedByContinuousConstraint = previousState.lastStepsizeLimitedByContinuousConstraint;
		wasLastStepsizeLimitedByDiscreteConstraint = previousState.wasLastStepsizeLimitedByDiscreteConstraint;
		currentValues = previousState.currentValues;
		currentDerivatives = previousState.currentDerivatives;
		nextValues = previousState.nextValues;
		nextTime = previousState.nextTime;
		previousState = null;
		setChanged();
		notifyObservers();
	}

	public Operation getOperation()
	{
		return operation;
	}

	public Double getCurrentTime()
	{
		return currentTime;
	}

	public Double getNextTime()
	{
		return nextTime;
	}

	public Double getPrevStepsize()
	{
		return prevStepsize;
	}

	public Double getDoubleValue(final ModelConnection.Variable variable)
	{
		return (Double) retrieveValue(currentValues.get(variable.instance), variable);
	}

	public Double getNextDoubleValue(final ModelConnection.Variable variable)
	{
		return (Double) retrieveValue(nextValues.get(variable.instance), variable);
	}

	public Integer getIntegerValue(final ModelConnection.Variable variable)
	{
		return (Integer) retrieveValue(currentValues.get(variable.instance), variable);
	}

	public Boolean getBooleanValue(final ModelConnection.Variable variable)
	{
		return (Boolean) retrieveValue(currentValues.get(variable.instance), variable);
	}

	public String getStringValue(final ModelConnection.Variable variable)
	{
		return (String) retrieveValue(currentValues.get(variable.instance), variable);
	}

	public Double getDerivative(final ModelConnection.Variable variable, final Integer order)
	{
		if (order < 1)
		{
			return null;
		}

		final Map<ScalarVariable, Map<Integer, Double>> dersInstance = currentDerivatives.get(variable.instance);
		if (dersInstance == null)
		{
			return null;
		}
		Map<ScalarVariable, Double> dersInstanceOrder = new HashMap<>();
		for (Map.Entry<ScalarVariable, Map<Integer, Double>> entry : dersInstance.entrySet()){
			dersInstanceOrder.put(entry.getKey(),entry.getValue().get(order));
		}
			

		if (dersInstanceOrder.isEmpty())
		{
			return null;
		}

		return retrieveValue(dersInstanceOrder, variable);
	}

	public Double getLastStepsizeLimitedByContinuousConstraint()
	{
		return lastStepsizeLimitedByContinuousConstraint;
	}

	public Boolean wasLastStepsizeLimitedByDiscreteConstraint()
	{
		return wasLastStepsizeLimitedByDiscreteConstraint != null && wasLastStepsizeLimitedByDiscreteConstraint;
	}

	private <T> T retrieveValue(final Map<ScalarVariable, T> values,
			final ModelConnection.Variable variable)
	{
		if (values == null)
		{
			return null;
		}

		for (ScalarVariable sv : values.keySet())
		{
			if (sv.getName().equals(variable.variable) && (Types.Real.equals(sv.getType().type) || Types.Integer.equals(sv.getType().type)))
			{
				return values.get(sv);
			}
		}
		return null;
	}

}
