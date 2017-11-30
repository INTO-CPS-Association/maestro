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
package org.intocps.orchestration.coe.cosim.varstep.constraint.bounddifference;

import static org.junit.Assert.assertTrue;

import org.intocps.orchestration.coe.cosim.varstep.CurrentSolutionPoint;
import org.intocps.orchestration.coe.cosim.varstep.StepsizeInterval;
import org.intocps.orchestration.coe.cosim.varstep.constraint.ConstraintHandler;
import org.intocps.orchestration.coe.cosim.varstep.constraint.TestUtil;
import org.intocps.orchestration.coe.cosim.varstep.constraint.boundeddifference.BoundedDifferenceHandler;
import org.intocps.orchestration.coe.json.InitializationMsgJson.Constraint;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class BoundDifferenceHandlerTest
{

	@Test
	public void testHandlerAgainstSimpleFunction()
	{
		assertTrue(executeTest(new SimpleBoundDifferenceFunction(), 0.01, 0.1));
	}

	public Boolean executeTest(final BoundDifferenceFunction fun,
			final Double dt_init, final Double t_end)
	{
		final CurrentSolutionPoint currentSolutionPoint = new CurrentSolutionPoint();
		final Constraint constraint = TestUtil.makeBoundDifferenceConstraint();
		final StepsizeInterval interval = new StepsizeInterval(1E-6, 1.0);
		final ConstraintHandler handler = new BoundedDifferenceHandler(currentSolutionPoint, constraint, interval, null);

		Double dMaxAbs = Double.MAX_VALUE;
		Double dMaxRel = Double.MAX_VALUE;
		Double t = 0.0;

		currentSolutionPoint.advance(t, TestUtil.setOutputValues(fun.getValues(t)[0], fun.getValues(t)[1]), null, null, false);
		Double dt = dt_init;

		System.out.println("Testing handler against " + fun.getLabel()
				+ " in t = [0.0 ; " + t_end + "] using dt_init = " + dt_init);
		while (t < t_end)
		{
			t += dt;
			final Double[] y = fun.getValues(t);
			dMaxAbs = Math.min(dMaxAbs, Math.abs(y[0] - y[1]));
			dMaxRel = Math.min(dMaxRel, Math.abs(y[0] - y[1])
					/ Math.max(Math.abs(y[0]), Math.abs(y[1])));
			currentSolutionPoint.advance(t, TestUtil.setOutputValues(y[0], y[1]), null, dt, false);
			dt = handler.getMaxStepSize();
			System.out.println("t = " + t + ", y[0] = " + y[0] + ", y[1] = "
					+ y[1] + ", dist = " + (y[0] - y[1]) + " -> dt = " + dt);
		}

		Boolean success = true;

		if (t < t_end)
		{
			System.out.println("t < t_end");
			success = false;
		}

		if (dMaxAbs > constraint.getAbsoluteTolerance())
		{
			System.out.println("absolute tolerance violated: " + dMaxAbs
					+ " > " + constraint.getAbsoluteTolerance());
			success = false;
		}

		if (dMaxRel > constraint.getRelativeTolerance())
		{
			System.out.println("Relative tolerance violated: " + dMaxRel
					+ " > " + constraint.getRelativeTolerance());
			success = false;
		}

		return success;
	}
}
