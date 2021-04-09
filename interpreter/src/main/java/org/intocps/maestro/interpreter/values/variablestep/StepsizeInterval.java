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

public class StepsizeInterval
{
	private static final Double DEFAULT_MIN_STEPSIZE = 1E-6;
	private static final Double DEFAULT_MAX_STEPSIZE = 1.0;

	private Double minimalStepsize;
	private Double maximalStepsize;

	public StepsizeInterval(final Double min, final Double max)
	{
		minimalStepsize = Math.min(min, max);
		maximalStepsize = Math.max(min, max);
	}

	public StepsizeInterval()
	{
		this(DEFAULT_MIN_STEPSIZE, DEFAULT_MAX_STEPSIZE);
	}

	public Double saturateStepsize(final Double stepsize)
	{
		final Double stepsizeClippedAtMin = Math.max(minimalStepsize, stepsize);
		return Math.min(maximalStepsize, stepsizeClippedAtMin);
	}

	public Double getMinimalStepsize()
	{
		return minimalStepsize;
	}

	public Double getMaximalStepsize()
	{
		return maximalStepsize;
	}

}
