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
package org.intocps.orchestration.coe.cosim.varstep.constraint.zerocrossing.detection;

import org.intocps.orchestration.coe.cosim.varstep.valuetracker.OptionalDifferenceTracker;

public class ZerocrossingDetector
{

	private OptionalDifferenceTracker tracker;
	private ZerocrossingConstraintState state;

	public ZerocrossingDetector(final OptionalDifferenceTracker tracker)
	{
		this.tracker = tracker;
	}

	public Double getResolvedDistanceToZerocrossing()
	{
		if (!hasZerocrossingOccurred())
		{
			return null;
		}
		final Double currentDistance = Math.abs(tracker.getCurrentValue());
		final Double nextDistance = Math.abs(tracker.getNextValue());
		return Math.min(currentDistance, nextDistance);
	}

	public Boolean hasZerocrossingViolatedTolerance(final Double tolerance)
	{
		final Double resolvedDistance = getResolvedDistanceToZerocrossing();
		return resolvedDistance > tolerance;
	}

	public void updateZeroCrossingState()
	{
		if (hasZerocrossingOccurred())
		{
			state = ZerocrossingConstraintState.CROSSED;
			return;
		}
		if (isApproachingZerocrossing())
		{
			state = ZerocrossingConstraintState.APPROACHING;
			return;
		}
		state = ZerocrossingConstraintState.DISTANCING;
	}

	public ZerocrossingConstraintState getZerocrossingState()
	{
		return state;
	}

	public Boolean isApproachingZerocrossing()
	{
		final Double currentDistance = tracker.getCurrentValue();
		final Double nextDistance = tracker.getNextValue();
		return Math.abs(nextDistance) - Math.abs(currentDistance) < 0;
	}

	public Boolean hasZerocrossingOccurred()
	{
		final Double currentDistance = tracker.getCurrentValue();
		final Double nextDistance = tracker.getNextValue();
		if (currentDistance == Double.MAX_VALUE
				|| nextDistance == Double.MAX_VALUE)
		{
			return false;
		}
		return currentDistance * nextDistance <= 0;
	}

}
