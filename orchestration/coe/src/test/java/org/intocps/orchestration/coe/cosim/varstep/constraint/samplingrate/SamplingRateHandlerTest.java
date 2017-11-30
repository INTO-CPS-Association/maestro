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
package org.intocps.orchestration.coe.cosim.varstep.constraint.samplingrate;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.intocps.orchestration.coe.cosim.varstep.CurrentSolutionPoint;
import org.intocps.orchestration.coe.cosim.varstep.StepsizeInterval;
import org.intocps.orchestration.coe.cosim.varstep.constraint.ConstraintHandler;
import org.intocps.orchestration.coe.cosim.varstep.constraint.TestUtil;
import org.intocps.orchestration.coe.cosim.varstep.constraint.zerocrossing.LinearFunction;
import org.intocps.orchestration.coe.cosim.varstep.constraint.zerocrossing.ScalarFunction;
import org.intocps.orchestration.coe.json.InitializationMsgJson.Constraint;
import org.junit.Test;

public class SamplingRateHandlerTest
{

	@Test
	public void testHandlerAgainstSimpleFunction()
	{
		assertTrue(executeTest(new LinearFunction(), 0.38, 10.0));
	}

	public Boolean executeTest(final ScalarFunction fun, final Double dt_init,
			final Double t_end)
	{
		final CurrentSolutionPoint currentSolutionPoint = new CurrentSolutionPoint();
		final Constraint constraint = TestUtil.makeSamplingRateConstraint();
		final StepsizeInterval interval = new StepsizeInterval(1E-6, 1.0);
		final ConstraintHandler handler = new SamplingRateHandler(currentSolutionPoint, constraint, interval);

		Double yMin = Double.MAX_VALUE;

		Double t = 0.0;
		currentSolutionPoint.advance(t, TestUtil.setOutputValue(fun.getValue(t)), null, null, true);
		Double dt = dt_init;
		final List<Double> timepoints = new ArrayList<Double>();
		timepoints.add(t);

		System.out.println("Testing handler against " + fun.getLabel()
				+ " in t = [0.0 ; " + t_end + "] using dt_init = " + dt_init);
		while (t < t_end)
		{
			t += dt;
			timepoints.add(t);
			final Double y = fun.getValue(t);
			yMin = Math.min(yMin, Math.abs(y));
			currentSolutionPoint.advance(t, TestUtil.setOutputValue(y), null, dt, true);
			dt = handler.getMaxStepSize();
			System.out.println("t = " + t + ", y = " + y + " -> dt = " + dt);
		}

		Boolean success = true;
		final Integer n = timepoints.size();
		if (timepoints.get(n - 1) != 10.0)
		{
			success = false;
			System.out.println("Last timepoint must be 10.0");
		}
		if (timepoints.get(n - 2) != 9.0)
		{
			success = false;
			System.out.println("Second last timepoint must be 9.0");
		}
		if (timepoints.get(n - 3) != 8.0)
		{
			success = false;
			System.out.println("Third last timepoint must be 8.0");
		}
		if (timepoints.get(n - 4) != 7.0)
		{
			success = false;
			System.out.println("Fourth last timepoint must be 7.0");
		}
		if (timepoints.get(n - 5) != 6.0)
		{
			success = false;
			System.out.println("Fifth last timepoint must be 6.0");
		}
		if (timepoints.get(n - 6) != 5.0)
		{
			success = false;
			System.out.println("Sixth last timepoint must be 5.0");
		}

		System.out.println(success ? "Success" : "*** FAILURE *** ");
		return success;
	}

}
