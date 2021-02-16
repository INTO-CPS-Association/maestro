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
package org.intocps.maestro.interpreter.values.variablestep.oscillation;

import org.intocps.maestro.interpreter.values.variablestep.valuetracker.OptionalDifferenceTracker;

public class OscillationDetector
{

	private OptionalDifferenceTracker tracker;

	public OscillationDetector(final OptionalDifferenceTracker tracker)
	{
		this.tracker = tracker;
	}

	public Boolean areOscillationsBuildingUp()
	{
		final Double currentDistance = tracker.getCurrentValue();
		final Double previousDistance = tracker.getPreviousValue();
		final Double prevPrevDistance = tracker.getPrevPrevValue();
		if (prevPrevDistance == null)
		{
			return false;
		}
		return (currentDistance * previousDistance < 0)
				&& (currentDistance * prevPrevDistance >= 0)
				&& (Math.abs(previousDistance) < Math.abs(currentDistance))
				&& (Math.abs(prevPrevDistance) < Math.abs(previousDistance));
	}

}
