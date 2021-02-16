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

import java.util.Observable;
import java.util.Observer;

import org.intocps.maestro.interpreter.values.variablestep.CurrentSolutionPoint;
import org.intocps.maestro.interpreter.values.variablestep.InitializationMsgJson;
import org.intocps.maestro.interpreter.values.variablestep.StepsizeInterval;
import org.intocps.maestro.interpreter.values.variablestep.CurrentSolutionPoint.Operation;
import org.intocps.maestro.interpreter.values.variablestep.constraint.ConstraintHandler;
import org.intocps.maestro.interpreter.values.variablestep.constraint.boundeddifference.stepsize.BdStepsizeAdjustmentStrategy;
import org.intocps.maestro.interpreter.values.variablestep.constraint.boundeddifference.stepsize.SimpleBdStepsizeAdjustmentStrategy;
import org.intocps.orchestration.coe.httpserver.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoundedDifferenceHandler implements Observer, ConstraintHandler
{

	final static Logger logger = LoggerFactory.getLogger(RequestHandler.class);

	private BoundedDifferenceHandler previousState = null;
	private Double prevStepsize;
	private BdStepsizeAdjustmentStrategy stepsizeAdjustmentStrategy;
	private BoundedDifferenceCalculator calculator;
	private StepsizeInterval interval;
	private String id;
	private Double currentTime;
	private Double absTol;
	private Double relTol;
	private Double nextTime;

	public BoundedDifferenceHandler(final Observable observable,
			final InitializationMsgJson.Constraint jc, final StepsizeInterval interval,
			final Double strongRelaxationFactor)
	{
		calculator = new BoundedDifferenceCalculator(observable, jc.getPorts());
		absTol = jc.getAbsoluteTolerance();
		relTol = jc.getRelativeTolerance();
		id = jc.getId();
		this.interval = interval;
		stepsizeAdjustmentStrategy = new SimpleBdStepsizeAdjustmentStrategy(calculator, jc, interval, strongRelaxationFactor, observable);
		observable.addObserver(this);
	}

	public BoundedDifferenceHandler(final BoundedDifferenceHandler bdh)
	{
		prevStepsize = bdh.prevStepsize;
		stepsizeAdjustmentStrategy = bdh.stepsizeAdjustmentStrategy;
		calculator = bdh.calculator;
		interval = bdh.interval;
		id = bdh.id;
		currentTime = bdh.currentTime;
		absTol = bdh.absTol;
		relTol = bdh.relTol;
		nextTime = bdh.nextTime;
		previousState = null;
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
				previousState = new BoundedDifferenceHandler(this);
				prevStepsize = cs.getPrevStepsize();
				currentTime = cs.getCurrentTime();
				nextTime = null;
			}

			if (Operation.PEEK.equals(op))
			{
				nextTime = cs.getNextTime();
			}

			if (Operation.ROLLBACK.equals(op))
			{
				prevStepsize = previousState.prevStepsize;
				currentTime = previousState.currentTime;
				nextTime = previousState.nextTime;
				previousState = null;
			}
		}
	}

	@Override
	public Double getMaxStepSize()
	{
		return stepsizeAdjustmentStrategy.getStepsize(prevStepsize);
	}

	@Override
	public String getDecision()
	{
		return stepsizeAdjustmentStrategy.getDecision();
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public Boolean isRelaxingStrongly()
	{
		return stepsizeAdjustmentStrategy.isRelaxingStrongly();
	}

	@Override
	public Boolean wasStepValid()
	{
		Boolean valid = true;
		final Double absDistance = calculator.getNextAbsoluteDifference();
		if (Math.abs(absDistance) > absTol)
		{
			logAbsoluteToleranceViolation(absDistance);
			valid = false;
		}
		final Double relDistance = calculator.getNextRelativeDifference();
		if (Math.abs(relDistance) > relTol)
		{
			logRelativeToleranceViolation(relDistance);
			valid = false;
		}
		return valid;
	}

	private void logAbsoluteToleranceViolation(final Double distance)
	{
		final Double peekStepsize = nextTime - currentTime;
		final Boolean isStepsizeMinimal = peekStepsize.equals(interval.getMinimalStepsize());
		String msg = "Absolute tolerance violated!\n";
		msg += "\t| The bound difference defined by the constraint \"";
		msg += id;
		msg += "\"\n\t| could not be met in the time interval [ ";
		msg += currentTime;
		msg += " ; ";
		msg += nextTime;
		msg += " ]\n\t| as the absolute difference of ";
		msg += distance;
		msg += "\n\t| exceeds the absolute tolerance of ";
		msg += absTol;
		if (isStepsizeMinimal)
		{
			msg += "\n\t| The stepsize equals the minimal stepsize of ";
			msg += interval.getMinimalStepsize();
			msg += " !";
		}
		logger.warn(msg);
	}

	private void logRelativeToleranceViolation(final Double distance)
	{
		final Double peekStepsize = nextTime - currentTime;
		final Boolean isStepsizeMinimal = peekStepsize.equals(interval.getMinimalStepsize());
		String msg = "Relative tolerance violated!\n";
		msg += "\t| The bound difference defined by the constraint \"";
		msg += id;
		msg += "\"\n\t| could not be met in the time interval [ ";
		msg += currentTime;
		msg += " ; ";
		msg += nextTime;
		msg += " ]\n\t| as the relative difference of ";
		msg += distance;
		msg += "\n\t| exceeds the relative tolerance of ";
		msg += relTol;
		if (isStepsizeMinimal)
		{
			msg += "\n\t| The stepsize equals the minimal stepsize of ";
			msg += interval.getMinimalStepsize();
			msg += " !";
		}
		logger.warn(msg);
	}
}
