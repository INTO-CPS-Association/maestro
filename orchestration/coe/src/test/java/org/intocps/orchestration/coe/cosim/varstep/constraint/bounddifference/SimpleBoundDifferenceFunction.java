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

public class SimpleBoundDifferenceFunction implements BoundDifferenceFunction
{

	private static final Double T_INIT = 293.15;
	private static final Integer C = 100;

	private Double prevTime = null;
	private Double temperature1 = T_INIT;
	private Double temperature2 = T_INIT;

	@Override
	public Double[] getValues(Double time)
	{
		if (prevTime != null)
		{
			final Double dt = time - prevTime;
			doStep(time, dt);
		}
		prevTime = time;
		return new Double[] { temperature1, temperature2 };
	}

	@Override
	public String getLabel()
	{
		return "Simple function";
	}

	private void doStep(final Double t, final Double dt)
	{
		temperature1 = T_INIT + t;
		temperature2 = temperature1 + 0.01 * Math.pow(t - C, 2) * dt;
	}

	// class AnotherSimpleBoundDifferenceTestModel
	// {
	//
	// private Double temperature1;
	// private Double temperature2;
	// private SimpleBoundDifferenceTestModel otherModel;
	//
	// public AnotherSimpleBoundDifferenceTestModel(
	// final Double temperature1_init,
	// final SimpleBoundDifferenceTestModel otherModel)
	// {
	// temperature1 = temperature1_init;
	// temperature2 = temperature1;
	// this.otherModel = otherModel;
	// }
	//
	// private void doStep(final Double t, final Double dt)
	// {
	// temperature1 = otherModel.getT1() + 0.013
	// * Math.pow(t - C * 0.97, 2) * dt;
	// temperature2 = otherModel.getT2() + 0.007
	// * Math.pow(t - C * 1.12, 2) * dt;
	// }
	//
	// private Double getT1()
	// {
	// return temperature1;
	// }
	//
	// private Double getT2()
	// {
	// return temperature2;
	// }
	//
	// }

}
