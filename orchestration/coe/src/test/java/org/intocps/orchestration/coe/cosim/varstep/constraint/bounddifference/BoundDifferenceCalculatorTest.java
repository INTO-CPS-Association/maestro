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

import static org.junit.Assert.assertEquals;

import org.intocps.orchestration.coe.cosim.varstep.CurrentSolutionPoint;
import org.intocps.orchestration.coe.cosim.varstep.constraint.TestUtil;
import org.intocps.orchestration.coe.cosim.varstep.constraint.boundeddifference.BoundedDifferenceCalculator;
import org.intocps.orchestration.coe.json.InitializationMsgJson.Constraint;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class BoundDifferenceCalculatorTest
{

	@Test
	public void testBoundDifferenceBetweenTwoPorts()
	{
		final Constraint constraint = TestUtil.makeBoundDifferenceConstraint();
		final CurrentSolutionPoint currentSolutionPoint = new CurrentSolutionPoint();
		final BoundedDifferenceCalculator calc = new BoundedDifferenceCalculator(currentSolutionPoint, constraint.getPorts());

		currentSolutionPoint.advance(0.0, TestUtil.setOutputValues(1.0, 1.0), null, null, false);
		currentSolutionPoint.advance(1.0, TestUtil.setOutputValues(1.0, 2.0), null, 1.0, false);
		assertEquals(new Double(1.0), calc.getAbsoluteDifference());
		assertEquals(new Double(0.5), calc.getRelativeDifference());
		currentSolutionPoint.advance(1.0, TestUtil.setOutputValues(-9.0, -10.0), null, 1.0, false);
		assertEquals(new Double(1.0), calc.getAbsoluteDifference());
		assertEquals(new Double(0.1), calc.getRelativeDifference());
	}

	@Test
	public void testBoundDifferenceOfPreviousAndCurrentPortValue()
	{
		final Constraint constraint = TestUtil.makeScalarZerocrossingConstraint();
		final CurrentSolutionPoint currentSolutionPoint = new CurrentSolutionPoint();
		final BoundedDifferenceCalculator calc = new BoundedDifferenceCalculator(currentSolutionPoint, constraint.getPorts());

		currentSolutionPoint.advance(0.0, TestUtil.setOutputValue(1.0), null, null, false);
		currentSolutionPoint.advance(1.0, TestUtil.setOutputValue(2.0), null, 1.0, false);
		assertEquals(new Double(1.0), calc.getAbsoluteDifference());
		assertEquals(new Double(0.5), calc.getRelativeDifference());
		currentSolutionPoint.advance(0.0, TestUtil.setOutputValue(-9.0), null, 1.0, false);
		currentSolutionPoint.advance(1.0, TestUtil.setOutputValue(-10.0), null, 1.0, false);
		assertEquals(new Double(-1.0), calc.getAbsoluteDifference());
		assertEquals(new Double(-0.1), calc.getRelativeDifference());
	}

}
