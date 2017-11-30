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

import org.intocps.orchestration.coe.cosim.varstep.StepsizeInterval;
import org.intocps.orchestration.coe.cosim.varstep.constraint.zerocrossing.detection.ZerocrossingConstraintState;
import org.intocps.orchestration.coe.cosim.varstep.oscillation.OscillationDetector;
import org.intocps.orchestration.coe.cosim.varstep.valuetracker.OptionalDifferenceTracker;

public class DefaultZcStepsizeAdjustmentStrategy implements
		ZcStepsizeAdjustmentStrategy
{

	private enum DistanceBin
	{
		OUTSIDE_TOLERANCE, WITHIN_TOLERANCE, WELL_WITHIN_TOLERANCE
	};

	private enum Decision
	{
		OSCILLATIONS_MINIMAL, OSCILLATIONS_TIGHTEN, OSCILLATIONS_CONSTANT, ZEROCROSSING_TIGHTEN, ZEROCROSSING_CONSTANT, ZEROCROSSING_RELAX, APPROACHING_HIT, APPROACHING_STRONGLYRELAX, APPROACHING_RELAX, APPROACHING_CONSTANT, APPROACHING_TIGHTEN, APPROACHING_AT0X_CONSTANT, APPROACHING_AT0X_RELAX, DISTANCING_RELAXSTRONGLY;

		public String getLogText()
		{
			switch (this)
			{
				case OSCILLATIONS_MINIMAL:
					return "set stepsize to minimum (oscillations are building up)";
				case OSCILLATIONS_TIGHTEN:
					return "tighten stepsize (oscillations are building up)";
				case OSCILLATIONS_CONSTANT:
					return "hold stepsize constant (oscillations are building up)";
				case ZEROCROSSING_TIGHTEN:
					return "tighten stepsize (zerocrossing just occurred)";
				case ZEROCROSSING_CONSTANT:
					return "hold stepsize constant (zerocrossing just occurred)";
				case ZEROCROSSING_RELAX:
					return "relax stepsize (zerocrossing just occurred)";
				case APPROACHING_HIT:
					return "adjust stepsize to hit (possible zerocrossing imminent)";
				case APPROACHING_STRONGLYRELAX:
					return "strongly relax stepsize (approaching zerocrossing)";
				case APPROACHING_RELAX:
					return "relax stepsize (approaching zerocrossing)";
				case APPROACHING_CONSTANT:
					return "hold stepsize constant (approaching zerocrossing)";
				case APPROACHING_TIGHTEN:
					return "tighten stepsize (approaching zerocrossing)";
				case APPROACHING_AT0X_CONSTANT:
					return "hold stepsize constant (within zerocrossing tolerance)";
				case APPROACHING_AT0X_RELAX:
					return "relax stepsize (well within zerocrossing tolerance)";
				case DISTANCING_RELAXSTRONGLY:
					return "strongly relax stepsize (distancing zerocrossing)";
				default:
					return "";
			}
		}
	};

	private static final Double TIGHTENING_FACTOR = 0.5;
	private static final Double RELAXATION_FACTOR = 1.2;
	private static final Double TOLERANCE_SAFETY_FACTOR = 0.5;
	private static final Double TIGHTENING_BOUNDARY_WHILE_APPROACHING = 1.5 * RELAXATION_FACTOR;
	private static final Double STRONG_RELAXATION_BOUNDARY_WHILE_APPROACHING_FACTOR = 10.0;

	private ZerocrossingPredictor predictor;
	private OptionalDifferenceTracker tracker;
	private OscillationDetector oscillationDetector;
	private StepsizeInterval interval;
	private Double tol;
	private Decision decision = null;
	private Double strongRelaxationFactor;
	private Double relaxationBoundaryWhileApproaching;
	private Double strongRelaxationBoundaryWhileApproaching;

	public DefaultZcStepsizeAdjustmentStrategy(
			final OptionalDifferenceTracker tracker, final Double tol,
			final StepsizeInterval interval,
			final Double strongRelaxationFactor, final Double safety)
	{
		oscillationDetector = new OscillationDetector(tracker);
		predictor = new ZerocrossingPredictor(tracker, safety);
		this.tracker = tracker;
		this.tol = tol;
		this.interval = interval;
		this.strongRelaxationFactor = strongRelaxationFactor;
		relaxationBoundaryWhileApproaching = strongRelaxationFactor;
		strongRelaxationBoundaryWhileApproaching = STRONG_RELAXATION_BOUNDARY_WHILE_APPROACHING_FACTOR
				* relaxationBoundaryWhileApproaching;
	}

	@Override
	public Double getStepsize(ZerocrossingConstraintState state,
			final Double lastContinuousStepsize)
	{
		if (ZerocrossingConstraintState.CROSSED.equals(state))
		{
			return calcStepsizeAfterZerocrossing(lastContinuousStepsize);
		}
		if (ZerocrossingConstraintState.APPROACHING.equals(state))
		{
			return calcStepsizeWhenApproachingZerocrossing(lastContinuousStepsize);
		}
		if (ZerocrossingConstraintState.DISTANCING.equals(state))
		{
			return calcStepsizeWhenDistancingZerocrossing(lastContinuousStepsize);
		}
		throw new IllegalStateException("Illegal ZerocrossingState");
	}

	@Override
	public String getDecision()
	{
		return decision.getLogText();
	}

	@Override
	public Boolean isRelaxingStrongly()
	{
		return Decision.DISTANCING_RELAXSTRONGLY.equals(decision);
	}

	private Double calcStepsizeAfterZerocrossing(
			final Double lastContinuousStepsize)
	{
		final Double distance = tracker.getCurrentValue();
		final DistanceBin bin = getBinByDistanceAndTolerance(distance, tol);

		if (oscillationDetector.areOscillationsBuildingUp())
		{
			if (DistanceBin.OUTSIDE_TOLERANCE.equals(bin))
			{
				updateDecision(Decision.OSCILLATIONS_MINIMAL);
				return interval.getMinimalStepsize();
			}
			if (DistanceBin.WITHIN_TOLERANCE.equals(bin))
			{
				updateDecision(Decision.OSCILLATIONS_TIGHTEN);
				return getTightenedStepsize(lastContinuousStepsize);
			}
			updateDecision(Decision.OSCILLATIONS_CONSTANT);
			return lastContinuousStepsize;
		}

		if (DistanceBin.OUTSIDE_TOLERANCE.equals(bin))
		{
			updateDecision(Decision.ZEROCROSSING_TIGHTEN);
			return getTightenedStepsize(lastContinuousStepsize);
		}

		if (DistanceBin.WITHIN_TOLERANCE.equals(bin))
		{
			updateDecision(Decision.ZEROCROSSING_CONSTANT);
			return lastContinuousStepsize;
		}

		if (DistanceBin.WELL_WITHIN_TOLERANCE.equals(bin))
		{
			updateDecision(Decision.ZEROCROSSING_RELAX);
			return getRelaxedStepsize(lastContinuousStepsize);
		}

		throw new IllegalStateException("Unreachable code");
	}

	private Double calcStepsizeWhenApproachingZerocrossing(
			final Double lastContinuousStepsize)
	{
		final Double distance = tracker.getCurrentValue();
		final DistanceBin bin = getBinByDistanceAndTolerance(distance, tol);

		if (DistanceBin.WELL_WITHIN_TOLERANCE.equals(bin))
		{
			updateDecision(Decision.APPROACHING_AT0X_RELAX);
			return getRelaxedStepsize(lastContinuousStepsize);
		}

		if (DistanceBin.WITHIN_TOLERANCE.equals(bin))
		{
			updateDecision(Decision.APPROACHING_AT0X_CONSTANT);
			return lastContinuousStepsize;
		}

		final Double nStepsTo0X = predictor.estimateNStepsToZerocrossing(lastContinuousStepsize);

		if (nStepsTo0X <= 1)
		{
			updateDecision(Decision.APPROACHING_HIT);
			return getStepsizeToHitZerocrossing(lastContinuousStepsize, nStepsTo0X);
		}

		if (nStepsTo0X > strongRelaxationBoundaryWhileApproaching)
		{
			updateDecision(Decision.APPROACHING_STRONGLYRELAX);
			return getStronglyRelaxedStepsize(lastContinuousStepsize);
		}

		if (nStepsTo0X > relaxationBoundaryWhileApproaching)
		{
			updateDecision(Decision.APPROACHING_RELAX);
			return getRelaxedStepsize(lastContinuousStepsize);
		}

		if (nStepsTo0X > TIGHTENING_BOUNDARY_WHILE_APPROACHING)
		{
			updateDecision(Decision.APPROACHING_CONSTANT);
			return lastContinuousStepsize;
		}

		if (nStepsTo0X <= TIGHTENING_BOUNDARY_WHILE_APPROACHING)
		{
			updateDecision(Decision.APPROACHING_TIGHTEN);
			return getTightenedStepsize(lastContinuousStepsize);
		}

		throw new IllegalStateException("Unreachable code");
	}

	private void updateDecision(final Decision newDecision)
	{
		decision = newDecision;
	}

	private Double calcStepsizeWhenDistancingZerocrossing(
			final Double lastContinuousStepsize)
	{
		updateDecision(Decision.DISTANCING_RELAXSTRONGLY);
		return getStronglyRelaxedStepsize(lastContinuousStepsize);
	}

	private DistanceBin getBinByDistanceAndTolerance(final Double dist,
			final Double tol)
	{
		if (Math.abs(dist) < tol * TOLERANCE_SAFETY_FACTOR)
		{
			return DistanceBin.WELL_WITHIN_TOLERANCE;
		}
		if (Math.abs(dist) < tol)
		{
			return DistanceBin.WITHIN_TOLERANCE;
		}
		return DistanceBin.OUTSIDE_TOLERANCE;
	}

	private Double getTightenedStepsize(final Double prevStepsize)
	{
		return interval.saturateStepsize(prevStepsize * TIGHTENING_FACTOR);
	}

	private Double getRelaxedStepsize(final Double prevStepsize)
	{
		return interval.saturateStepsize(prevStepsize * RELAXATION_FACTOR);
	}

	private Double getStronglyRelaxedStepsize(final Double prevStepsize)
	{
		return interval.saturateStepsize(prevStepsize * strongRelaxationFactor);
	}

	private Double getStepsizeToHitZerocrossing(final Double prevStepsize,
			final Double nStepsTo0X)
	{
		return interval.saturateStepsize(prevStepsize * nStepsTo0X);
	}

}
