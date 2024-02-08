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
package org.intocps.orchestration.coe.cosim.varstep.constraint.zerocrossing;

import org.intocps.orchestration.coe.cosim.varstep.CurrentSolutionPoint;
import org.intocps.orchestration.coe.cosim.varstep.StepsizeInterval;
import org.intocps.orchestration.coe.cosim.varstep.constraint.ConstraintHandler;
import org.intocps.orchestration.coe.cosim.varstep.constraint.TestUtil;
import org.intocps.orchestration.coe.json.InitializationMsgJson.Constraint;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Ignore
public class ZeroCrossingHandlerTest
{

	@Test
	public void testFirstOrderHandlerAgainstSine()
	{
		assertFalse(executeTest(new SineFunction(), 0.1, 4.0, 1, 0.0));
	}

	@Test
	public void testConservativeFirstOrderHandlerAgainstSine()
	{
		assertTrue(executeTest(new SineFunction(), 0.1, 4.0, 1, 0.5));
	}

	@Test
	public void testSecondOrderHandlerAgainstSine()
	{
		assertTrue(executeTest(new SineFunction(), 0.1, 4.0, 2, 0.0));
	}

	@Test
	public void testFirstOrderHandlerAgainstSecondOrderParabola()
	{
		assertTrue(executeTest(new SecondOrderParabolaFunction(), 0.1, 3.0, 1, 0.0));
	}

	@Test
	public void testSecondOrderHandlerAgainstSecondOrderParabola()
	{
		assertTrue(executeTest(new SecondOrderParabolaFunction(), 0.1, 3.0, 2, 0.0));
	}

	@Test
	public void testFirstOrderHandlerAgainstThirdOrderParabola()
	{
		assertFalse(executeTest(new ThirdOrderParabolaFunction(), 0.1, 2.0, 1, 0.0));
	}

	@Test
	public void testConservativeFirstOrderHandlerAgainstThirdOrderParabola()
	{
		assertTrue(executeTest(new ThirdOrderParabolaFunction(), 0.1, 2.0, 1, 1.0));
	}

	@Test
	public void testSecondOrderHandlerAgainstThirdOrderParabola()
	{
		assertTrue(executeTest(new ThirdOrderParabolaFunction(), 0.1, 2.0, 2, 0.0));
	}

	@Test
	public void testFirstOrderHandlerAgainstExponentialFunction()
	{
		assertFalse(executeTest(new ExponentialFunction(), 0.1, 3.0, 1, 0.0));
	}

	@Test
	public void testConservativeFirstOrderHandlerAgainstExponentialFunction()
	{
		assertTrue(executeTest(new ExponentialFunction(), 0.1, 3.0, 1, 0.4));
	}

	@Test
	public void testSecondOrderHandlerAgainstExponentialFunction()
	{
		assertTrue(executeTest(new ExponentialFunction(), 0.1, 3.0, 2, 0.0));
	}

	@Test
	public void testFirstOrderHandlerAgainstLinearFunction()
	{
		assertTrue(executeTest(new LinearFunction(), 0.1, 3.0, 1, 0.0));
	}

	@Test
	public void testSecondOrderHandlerAgainstLinearFunction()
	{
		assertTrue(executeTest(new LinearFunction(), 0.1, 3.0, 2, 0.0));
	}

	@Test
	public void testFirstOrderHandlerAgainstSquareRoot()
	{
		assertTrue(executeTest(new SquareRootFunction(), 0.1, 3.0, 1, 0.0));
	}

	@Test
	public void testSecondOrderHandlerAgainstSquareRoot()
	{
		assertTrue(executeTest(new SquareRootFunction(), 0.1, 3.0, 2, 0.0));
	}

	public Boolean executeTest(final ScalarFunction fun, final Double dt_init,
			final Double t_end, final Integer order, final Double safety)
	{
		final CurrentSolutionPoint currentSolutionPoint = new CurrentSolutionPoint();
		final Constraint constraint = TestUtil.makeScalarZerocrossingConstraint();
		constraint.order = order;
		constraint.safety = safety;
		final String orderStr = order == 1 ? "first" : "second";
		final StepsizeInterval interval = new StepsizeInterval(1E-6, 1.0);
		final ConstraintHandler handler = new ZerocrossingHandler(currentSolutionPoint, constraint, interval, null);

		Double yMin = Double.MAX_VALUE;

		Double t = 0.0;
		currentSolutionPoint.advance(t, TestUtil.setOutputValue(fun.getValue(t)), null, null, false);
		Double dt = dt_init;

		System.out.println("Testing " + orderStr + " order handler against "
				+ fun.getLabel() + " in t = [0.0 ; " + t_end
				+ "] using dt_init = " + dt_init);
		while (t < t_end)
		{
			t += dt;
			final Double y = fun.getValue(t);
			yMin = Math.min(yMin, Math.abs(y));
			currentSolutionPoint.advance(t, TestUtil.setOutputValue(y), null, dt, false);
			dt = handler.getMaxStepSize();
			System.out.println("t = " + t + ", y = " + y + " -> dt = " + dt);
		}

		final Boolean success = yMin < constraint.getAbsoluteTolerance();
		final String verdict = success ? "Success" : "*** FAILURE *** ";

		System.out.println(verdict + ": Minimal distance to zerocrossing is "
				+ yMin + " (absTol = " + constraint.getRelativeTolerance()
				+ ")\n");
		return success;
	}

}
