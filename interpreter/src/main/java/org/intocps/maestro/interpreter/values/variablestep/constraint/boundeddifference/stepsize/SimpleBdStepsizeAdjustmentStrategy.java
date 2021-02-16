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
package org.intocps.maestro.interpreter.values.variablestep.constraint.boundeddifference.stepsize;

import java.util.Observable;
import java.util.Observer;

import org.intocps.maestro.interpreter.values.variablestep.CurrentSolutionPoint;
import org.intocps.maestro.interpreter.values.variablestep.CurrentSolutionPoint.Operation;
import org.intocps.maestro.interpreter.values.variablestep.InitializationMsgJson;
import org.intocps.maestro.interpreter.values.variablestep.StepsizeInterval;
import org.intocps.maestro.interpreter.values.variablestep.constraint.boundeddifference.BoundedDifferenceCalculator;

public class SimpleBdStepsizeAdjustmentStrategy implements Observer,
		BdStepsizeAdjustmentStrategy
{
	private enum LimitingTolerance
	{
		ABSOLUTE, RELATIVE, BOTH;

		private String getLogText()
		{
			switch (this)
			{
				case ABSOLUTE:
					return "absolute difference";
				case RELATIVE:
					return "relative difference";
				case BOTH:
					return "absolute and relative differences";
				default:
					return "";
			}
		}
	}

	private enum DistanceBin
	{
		VIOLATION, RISKY, TARGET, SAFE, SAFEST;
	};

	private enum Decision
	{
		RELAX_STRONGLY, RELAX, CONSTANT, TIGHTEN, TIGHTEN_STRONGLY;

		private String getLogTextBeginning()
		{
			switch (this)
			{
				case RELAX_STRONGLY:
					return "strongly relax the stepsize (very safe ";
				case RELAX:
					return "relax the stepsize (safe ";
				case CONSTANT:
					return "hold the stepsize constant (";
				case TIGHTEN:
					return "tighten the stepsize (";
				case TIGHTEN_STRONGLY:
					return "strongly tighten the stepsize (tolerance violation of ";
				default:
					return "";
			}
		}

		private String getLogTextEnding()
		{
			switch (this)
			{
				case RELAX_STRONGLY:
					return ")";
				case RELAX:
					return ")";
				case CONSTANT:
					return " within target range)";
				case TIGHTEN:
					return " close to tolerance violation)";
				case TIGHTEN_STRONGLY:
					return ")";
				default:
					return "";
			}
		}
	}

	private static final String HAS_SKIPPED_LOGTEXT = "choose the last stepsize that was limited by a continuous constraint and ";

	private static final Double STEPSIZE_TIGHTENING_FACTOR = 0.5;
	private static final Double STEPSIZE_STRONG_TIGHTENING_FACTOR = 0.01;
	private static final Double STEPSIZE_RELAXATION_FACTOR = 1.2;

	private static final Double TOLERANCE_RISKYBIN_FACTOR = 0.6;
	private static final Double TOLERANCE_TARGETBIN_FACTOR = 0.4;
	private static final Double TOLERANCE_SAFEBIN_FACTOR = 0.2;

	private SimpleBdStepsizeAdjustmentStrategy previousState = null;
	private StepsizeInterval interval;
	private BoundedDifferenceCalculator calculator;
	private Double absTol;
	private Double relTol;
	private Double strongRelaxationFactor;
	private Decision decision;
	private LimitingTolerance limitingTolerance;
	private Double safety;
	private Boolean skipDiscrete;
	private Boolean hasSkippedDiscrete;
	private Boolean wasLastStepsizeLimitedByDiscreteConstraint;
	private Double lastStepsizeLimitedByContinuousConstraint;

	public SimpleBdStepsizeAdjustmentStrategy(
			final SimpleBdStepsizeAdjustmentStrategy sas)
	{
		interval = sas.interval;
		calculator = sas.calculator;
		absTol = sas.absTol;
		relTol = sas.relTol;
		strongRelaxationFactor = sas.strongRelaxationFactor;
		decision = sas.decision;
		limitingTolerance = sas.limitingTolerance;
		safety = sas.safety;
		skipDiscrete = sas.skipDiscrete;
		hasSkippedDiscrete = sas.hasSkippedDiscrete;
		wasLastStepsizeLimitedByDiscreteConstraint = sas.wasLastStepsizeLimitedByDiscreteConstraint;
		lastStepsizeLimitedByContinuousConstraint = sas.lastStepsizeLimitedByContinuousConstraint;
	}

	public SimpleBdStepsizeAdjustmentStrategy(
			final BoundedDifferenceCalculator calculator, final InitializationMsgJson.Constraint jc,
			final StepsizeInterval interval,
			final Double strongRelaxationFactor, final Observable observable)
	{
		this.calculator = calculator;
		absTol = jc.getAbsoluteTolerance();
		relTol = jc.getRelativeTolerance();
		safety = jc.getSafety();
		skipDiscrete = jc.getSkipDiscrete();
		this.interval = interval;
		this.strongRelaxationFactor = strongRelaxationFactor;
		if (skipDiscrete)
		{
			observable.addObserver(this);
		}
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
				previousState = new SimpleBdStepsizeAdjustmentStrategy(this);
				wasLastStepsizeLimitedByDiscreteConstraint = cs.wasLastStepsizeLimitedByDiscreteConstraint();
				lastStepsizeLimitedByContinuousConstraint = cs.getLastStepsizeLimitedByContinuousConstraint();
				hasSkippedDiscrete = false;
			}

			if (Operation.PEEK.equals(op))
			{
				// do nothing
			}

			if (Operation.ROLLBACK.equals(op))
			{
				wasLastStepsizeLimitedByDiscreteConstraint = previousState.wasLastStepsizeLimitedByDiscreteConstraint;
				lastStepsizeLimitedByContinuousConstraint = previousState.lastStepsizeLimitedByContinuousConstraint;
				hasSkippedDiscrete = previousState.hasSkippedDiscrete;
				decision = previousState.decision;
				limitingTolerance = previousState.limitingTolerance;
				previousState = null;
			}
		}
	}

	@Override
	public Double getStepsize(final Double prevStepsize)
	{
		if (skipDiscrete && wasLastStepsizeLimitedByDiscreteConstraint)
		{
			return getStepsizeAfterDiscreteEvent(prevStepsize);
		}

		decision = getDecisionBasedOnCurrentValues(calcDistanceBin());
		return mapDecisionOnStepsize(decision, prevStepsize);
	}

	@Override
	public String getDecision()
	{
		final String logIntro = hasSkippedDiscrete ? HAS_SKIPPED_LOGTEXT : "";
		return logIntro + decision.getLogTextBeginning()
				+ limitingTolerance.getLogText() + decision.getLogTextEnding();
	}

	@Override
	public Boolean isRelaxingStrongly()
	{
		return Decision.RELAX_STRONGLY.equals(decision);
	}

	private Double getStepsizeAfterDiscreteEvent(final Double prevStepsize)
	{
		final Decision repeatedDecision = removeRelaxations(decision);
		decision = getDecisionBasedOnCurrentValues(calcDistanceBin());

		final Double dtFromCurrentValues = mapDecisionOnStepsize(decision, prevStepsize);
		final Double dtFromRepeatedDecision = mapDecisionOnStepsize(repeatedDecision, lastStepsizeLimitedByContinuousConstraint);

		if (dtFromRepeatedDecision > dtFromCurrentValues)
		{
			decision = repeatedDecision;
			hasSkippedDiscrete = true;
			return dtFromRepeatedDecision;
		}

		return dtFromCurrentValues;
	}

	private Decision removeRelaxations(final Decision aDecision)
	{
		if (Decision.RELAX_STRONGLY.equals(aDecision)
				|| Decision.RELAX.equals(aDecision))
		{
			return Decision.CONSTANT;
		}
		return aDecision;
	}

	private Double mapDecisionOnStepsize(final Decision queryDecision,
			final Double prevStepsize)
	{

		if (Decision.TIGHTEN_STRONGLY.equals(queryDecision))
		{
			return getStronglyTightenedStepsize(prevStepsize);
		}

		if (Decision.TIGHTEN.equals(queryDecision))
		{
			return getTightenedStepsize(prevStepsize);
		}

		if (Decision.CONSTANT.equals(queryDecision))
		{
			return prevStepsize;
		}

		if (Decision.RELAX.equals(queryDecision))
		{
			return getRelaxedStepsize(prevStepsize);
		}

		if (Decision.RELAX_STRONGLY.equals(queryDecision))
		{
			return getStronglyRelaxedStepsize(prevStepsize);
		}

		throw new IllegalStateException("Unreachable code");

	}

	private Decision getDecisionBasedOnCurrentValues(final DistanceBin bin)
	{
		if (DistanceBin.VIOLATION.equals(bin))
		{
			return Decision.TIGHTEN_STRONGLY;
		}

		if (DistanceBin.RISKY.equals(bin))
		{
			return Decision.TIGHTEN;
		}

		if (DistanceBin.TARGET.equals(bin))
		{
			return Decision.CONSTANT;
		}

		if (DistanceBin.SAFE.equals(bin))
		{
			return Decision.RELAX;
		}

		if (DistanceBin.SAFEST.equals(bin))
		{
			return Decision.RELAX_STRONGLY;
		}

		throw new IllegalStateException("Unreachable code");
	}

	private DistanceBin calcDistanceBin()
	{
		final DistanceBin absBin = getBinByDistanceAndTolerance(calculator.getAbsoluteDifference(), absTol);
		final DistanceBin relBin = getBinByDistanceAndTolerance(calculator.getRelativeDifference(), relTol);
		if (absBin.ordinal() < relBin.ordinal())
		{
			limitingTolerance = LimitingTolerance.ABSOLUTE;
			return absBin;
		}
		if (relBin.ordinal() < absBin.ordinal())
		{
			limitingTolerance = LimitingTolerance.RELATIVE;
			return relBin;
		}
		limitingTolerance = LimitingTolerance.BOTH;
		return absBin;
	}

	private DistanceBin getBinByDistanceAndTolerance(final Double dist,
			final Double tol)
	{
		final Double safetyFactor = 1 / (1 + safety);
		if (Math.abs(dist) <= tol * TOLERANCE_SAFEBIN_FACTOR * safetyFactor)
		{
			return DistanceBin.SAFEST;
		}
		if (Math.abs(dist) <= tol * TOLERANCE_TARGETBIN_FACTOR * safetyFactor)
		{
			return DistanceBin.SAFE;
		}
		if (Math.abs(dist) <= tol * TOLERANCE_RISKYBIN_FACTOR * safetyFactor)
		{
			return DistanceBin.TARGET;
		}
		if (Math.abs(dist) <= tol)
		{
			return DistanceBin.RISKY;
		}
		return DistanceBin.VIOLATION;
	}

	private Double getTightenedStepsize(final Double prevStepsize)
	{
		return interval.saturateStepsize(prevStepsize
				* STEPSIZE_TIGHTENING_FACTOR);
	}

	private Double getStronglyTightenedStepsize(final Double prevStepsize)
	{
		return interval.saturateStepsize(prevStepsize
				* STEPSIZE_STRONG_TIGHTENING_FACTOR);
	}

	private Double getRelaxedStepsize(final Double prevStepsize)
	{
		return interval.saturateStepsize(prevStepsize
				* STEPSIZE_RELAXATION_FACTOR);
	}

	private Double getStronglyRelaxedStepsize(final Double prevStepsize)
	{
		return interval.saturateStepsize(prevStepsize * strongRelaxationFactor);
	}

}
