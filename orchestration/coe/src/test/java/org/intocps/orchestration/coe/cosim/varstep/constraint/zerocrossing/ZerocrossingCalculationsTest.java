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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.intocps.orchestration.coe.cosim.varstep.CurrentSolutionPoint;
import org.intocps.orchestration.coe.cosim.varstep.constraint.TestUtil;
import org.intocps.orchestration.coe.cosim.varstep.constraint.zerocrossing.detection.ZerocrossingDetector;
import org.intocps.orchestration.coe.cosim.varstep.oscillation.OscillationDetector;
import org.intocps.orchestration.coe.cosim.varstep.valuetracker.OptionalDifferenceTracker;
import org.intocps.orchestration.coe.json.InitializationMsgJson.Constraint;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ZerocrossingCalculationsTest
{

	@Test
	public void test()
	{
		final Constraint constraint = TestUtil.makeScalarZerocrossingConstraint();
		final CurrentSolutionPoint currentSolutionPoint = new CurrentSolutionPoint();
		final OptionalDifferenceTracker tracker = new OptionalDifferenceTracker(currentSolutionPoint, constraint.getPorts(), constraint.getOrder());

		ZerocrossingDetector zerocrossingDetector = new ZerocrossingDetector(tracker);
		OscillationDetector oscillationDetector = new OscillationDetector(tracker);

		currentSolutionPoint.advance(0.0, TestUtil.setOutputValue(10.0), null, null, false);
		currentSolutionPoint.advance(1.0, TestUtil.setOutputValue(11.0), null, 1.0, false);
		assertFalse("not approaching zero crossing", zerocrossingDetector.isApproachingZerocrossing());
		assertFalse("oscillations are not building up", oscillationDetector.areOscillationsBuildingUp());

		currentSolutionPoint.advance(2.0, TestUtil.setOutputValue(10.0), null, 1.0, false);
		assertTrue("approaching zero crossing", zerocrossingDetector.isApproachingZerocrossing());
		assertEquals("current distance to zero crossing = 10.0", new Double(10.0), tracker.getCurrentValue());

		currentSolutionPoint.advance(3.0, TestUtil.setOutputValue(-10.0), null, 1.0, false);
		assertTrue("zero crossing occurred", zerocrossingDetector.hasZerocrossingOccurred());
		assertEquals("current distance to zero crossing = -10.0", new Double(-10.0), tracker.getCurrentValue());
		assertFalse("oscillations are not building up", oscillationDetector.areOscillationsBuildingUp());

		currentSolutionPoint.advance(4.0, TestUtil.setOutputValue(-11.0), null, 1.0, false);
		assertFalse("not approaching zero crossing", zerocrossingDetector.isApproachingZerocrossing());
		assertEquals("current distance to zero crossing = -11.0", new Double(-11.0), tracker.getCurrentValue());
		assertFalse("oscillations are not building up", oscillationDetector.areOscillationsBuildingUp());

		currentSolutionPoint.advance(5.0, TestUtil.setOutputValue(-10.0), null, 1.0, false);
		assertTrue("approaching zero crossing", zerocrossingDetector.isApproachingZerocrossing());

		currentSolutionPoint.advance(6.0, TestUtil.setOutputValue(11.0), null, 1.0, false);
		assertFalse("oscillations are not building up", oscillationDetector.areOscillationsBuildingUp());

		currentSolutionPoint.advance(7.0, TestUtil.setOutputValue(-12.0), null, 1.0, false);
		assertTrue("oscillations are building up", oscillationDetector.areOscillationsBuildingUp());

		currentSolutionPoint.advance(8.0, TestUtil.setOutputValue(-11.0), null, 1.0, false);
		assertFalse("oscillations are not building up", oscillationDetector.areOscillationsBuildingUp());

	}
}
