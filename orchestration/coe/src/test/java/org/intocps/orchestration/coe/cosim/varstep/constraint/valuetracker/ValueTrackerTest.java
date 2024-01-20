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
package org.intocps.orchestration.coe.cosim.varstep.constraint.valuetracker;

import org.intocps.orchestration.coe.config.ModelConnection.Variable;
import org.intocps.orchestration.coe.cosim.varstep.CurrentSolutionPoint;
import org.intocps.orchestration.coe.cosim.varstep.constraint.TestUtil;
import org.intocps.orchestration.coe.cosim.varstep.valuetracker.DoubleValueTracker;
import org.intocps.orchestration.coe.cosim.varstep.valuetracker.OptionalDifferenceTracker;
import org.intocps.orchestration.coe.cosim.varstep.valuetracker.ValueTracker;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Ignore
public class ValueTrackerTest
{

	@Test
	public void testDerivativeEstimationFromDatapointsForConstantInput()
	{
		final List<Variable> variables = TestUtil.makeZerocrossingConstraint().getPorts();
		final Variable a = variables.get(0);

		final CurrentSolutionPoint currentSolutionPoint = new CurrentSolutionPoint();
		final ValueTracker tracker = new DoubleValueTracker(currentSolutionPoint, a, 2);

		currentSolutionPoint.advance(0.0, TestUtil.setOutputValues(1.0, 0.0), null, null, false);
		assertEquals("xdot must be 0", new Double(0.0), tracker.getFirstDerivative());
		assertEquals("xdotdot must be 0", new Double(0.0), tracker.getSecondDerivative());

		currentSolutionPoint.advance(1.0, TestUtil.setOutputValues(1.0, 0.0), null, 1.0, false);
		assertEquals("xdot must be 0", new Double(0.0), tracker.getFirstDerivative());
		assertEquals("xdotdot must be 0", new Double(0.0), tracker.getSecondDerivative());

		currentSolutionPoint.advance(2.0, TestUtil.setOutputValues(1.0, 0.0), null, 1.0, false);
		assertEquals("xdot must be 0", new Double(0.0), tracker.getFirstDerivative());
		assertEquals("xdotdot must be 0", new Double(0.0), tracker.getSecondDerivative());
	}

	@Test
	public void testDerivativeEstimationFromDatapointsForStraightLineInput()
	{
		final List<Variable> variables = TestUtil.makeZerocrossingConstraint().getPorts();
		final Variable a = variables.get(0);

		final CurrentSolutionPoint currentSolutionPoint = new CurrentSolutionPoint();
		final ValueTracker tracker = new DoubleValueTracker(currentSolutionPoint, a, 2);

		currentSolutionPoint.advance(0.0, TestUtil.setOutputValues(0.0, 0.0), null, null, false);
		assertEquals("xdot must be 0", new Double(0.0), tracker.getFirstDerivative());
		assertEquals("xdotdot must be 0", new Double(0.0), tracker.getSecondDerivative());

		currentSolutionPoint.advance(1.0, TestUtil.setOutputValues(1.0, 0.0), null, 1.0, false);
		assertEquals("xdot must be 1.0", new Double(1.0), tracker.getFirstDerivative());
		assertEquals("xdotdot must be 0", new Double(0.0), tracker.getSecondDerivative());

		currentSolutionPoint.advance(7.3, TestUtil.setOutputValues(7.3, 0.0), null, 6.3, false);
		assertEquals("xdot must be 1.0", new Double(1.0), tracker.getFirstDerivative());
		assertEquals("xdotdot must be 0", new Double(0.0), tracker.getSecondDerivative());
	}

	@Test
	public void testDerivativeEstimationFromDatapointsForParabolaInput()
	{

		final List<Variable> variables = TestUtil.makeZerocrossingConstraint().getPorts();
		final Variable a = variables.get(0);

		final CurrentSolutionPoint currentSolutionPoint = new CurrentSolutionPoint();
		final ValueTracker tracker = new DoubleValueTracker(currentSolutionPoint, a, 2);

		currentSolutionPoint.advance(0.0, TestUtil.setOutputValues(0.0, 0.0), null, null, false);
		assertEquals("xdot must be 0", new Double(0.0), tracker.getFirstDerivative());
		assertEquals("xdotdot must be 0", new Double(0.0), tracker.getSecondDerivative());

		currentSolutionPoint.advance(1.0, TestUtil.setOutputValues(1.0, 0.0), null, 1.0, false);
		assertEquals("xdot must be 1.0", new Double(1.0), tracker.getFirstDerivative());
		assertEquals("xdotdot must be 0", new Double(0.0), tracker.getSecondDerivative());

		currentSolutionPoint.advance(2.0, TestUtil.setOutputValues(4.0, 0.0), null, 1.0, false);
		assertEquals("xdot must be 4.0", new Double(4.0), tracker.getFirstDerivative());
		assertEquals("xdotdot must be 2.0", new Double(2.0), tracker.getSecondDerivative());

		currentSolutionPoint.advance(4.0, TestUtil.setOutputValues(16.0, 0.0), null, 2.0, false);
		assertEquals("xdot must be 8.0", new Double(8.0), tracker.getFirstDerivative());
		assertEquals("xdotdot must be 2.0", new Double(2.0), tracker.getSecondDerivative());
	}

	@Test
	public void testDoubleValueTracker()
	{
		final List<Variable> variables = TestUtil.makeZerocrossingConstraint().getPorts();
		final Variable a = variables.get(0);
		final Variable b = variables.get(1);

		final CurrentSolutionPoint currentSolutionPoint = new CurrentSolutionPoint();
		final ValueTracker trackerA = new DoubleValueTracker(currentSolutionPoint, a, 2);
		final ValueTracker trackerB = new DoubleValueTracker(currentSolutionPoint, b, 2);

		// y(a) = a, yd(a) = 1, ydd(a) = 0
		// y(b) = 10 * x^2 - 7, yd(b) = 20 * x, ydd(b) = 20
		currentSolutionPoint.advance(0.0, TestUtil.setOutputValues(0.0, -7.0), null, null, false);
		currentSolutionPoint.advance(1.0, TestUtil.setOutputValues(1.0, 3.0), null, 1.0, false);
		currentSolutionPoint.advance(3.0, TestUtil.setOutputValues(3.0, 83.0), null, 2.0, false);

		assertEquals("trackerA: current value must be 3.0", new Double(3.0), trackerA.getCurrentValue());
		assertEquals("trackerA: previous value must be 1.0", new Double(1.0), trackerA.getPreviousValue());
		assertEquals("trackerA: 1st derivative must be 1.0", new Double(1.0), trackerA.getFirstDerivative());
		assertEquals("trackerA: 2nd derivative must be 0.0", new Double(0.0), trackerA.getSecondDerivative());

		assertEquals("trackerB: current value must be 83.0", new Double(83.0), trackerB.getCurrentValue());
		assertEquals("trackerB: previous value must be 3.0", new Double(3.0), trackerB.getPreviousValue());
		assertEquals("trackerB: current 1st derivative must be 60.0", new Double(60.0), trackerB.getFirstDerivative());
		assertEquals("trackerB: current 2nd derivative must be 20.0", new Double(20.0), trackerB.getSecondDerivative());
	}

	@Test
	public void testOptionalDifferenceTracker()
	{
		final List<Variable> variables = TestUtil.makeZerocrossingConstraint().getPorts();
		final Variable b = variables.get(1);

		final List<Variable> variable = new ArrayList<Variable>();
		variable.add(b);

		final CurrentSolutionPoint currentSolutionPoint = new CurrentSolutionPoint();
		final ValueTracker trackerA = new OptionalDifferenceTracker(currentSolutionPoint, variables, 2);
		final ValueTracker trackerB = new OptionalDifferenceTracker(currentSolutionPoint, variable, 2);
		final ValueTracker trackerC = new OptionalDifferenceTracker(currentSolutionPoint, b, 2);

		// y(a) = a, yd(a) = 1, ydd(a) = 0
		// y(b) = 10 * x^2 - 7, yd(b) = 20 * x, ydd(b) = 20
		currentSolutionPoint.advance(0.0, TestUtil.setOutputValues(0.0, -7.0), null, null, false);
		currentSolutionPoint.advance(1.0, TestUtil.setOutputValues(1.0, 3.0), null, 1.0, false);
		currentSolutionPoint.advance(3.0, TestUtil.setOutputValues(3.0, 83.0), null, 2.0, false);

		assertEquals("trackerA: current value must be -80.0", new Double(-80.0), trackerA.getCurrentValue());
		assertEquals("trackerA: previous value must be -2.0", new Double(-2.0), trackerA.getPreviousValue());
		assertEquals("trackerA: 1st derivative must be -59.0", new Double(-59.0), trackerA.getFirstDerivative());
		assertEquals("trackerA: 2nd derivative must be -20.0", new Double(-20.0), trackerA.getSecondDerivative());

		assertEquals("trackerB: current value must be 83.0", new Double(83.0), trackerB.getCurrentValue());
		assertEquals("trackerB: previous value must be 3.0", new Double(3.0), trackerB.getPreviousValue());
		assertEquals("trackerB: current 1st derivative must be 60.0", new Double(60.0), trackerB.getFirstDerivative());
		assertEquals("trackerB: current 2nd derivative must be 20.0", new Double(20.0), trackerB.getSecondDerivative());

		assertEquals("trackerC: current value must be 83.0", new Double(83.0), trackerC.getCurrentValue());
		assertEquals("trackerC: previous value must be 3.0", new Double(3.0), trackerC.getPreviousValue());
		assertEquals("trackerC: current 1st derivative must be 60.0", new Double(60.0), trackerC.getFirstDerivative());
		assertEquals("trackerC: current 2nd derivative must be 20.0", new Double(20.0), trackerC.getSecondDerivative());

	}

}
