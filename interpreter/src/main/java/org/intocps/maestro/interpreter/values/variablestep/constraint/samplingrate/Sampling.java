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
package org.intocps.maestro.interpreter.values.variablestep.constraint.samplingrate;

import java.math.BigDecimal;

public class Sampling
{
	private Integer base;
	private Integer rate;
	private Integer startTime;

	public Sampling(final Integer base, final Integer rate,
			final Integer startTime)
	{
		this.base = base;
		this.rate = rate;
		this.startTime = startTime;
	}

	public Double calcTimeToNextSample(final Double currentTime)
	{
		final Double dStartTime = Math.pow(10, base) * startTime;
		final Double dRate = Math.pow(10, base) * rate;
		if (currentTime < dStartTime)
		{
			return dStartTime - currentTime;
		}
		
		final Double shiftedCurrentTime = rmEpsError(currentTime - dStartTime);
		final Double nNextSample = Math.floor(rmEpsError(shiftedCurrentTime / dRate)) + 1;
		final Double shiftedNextSampleTime = rmEpsError(dRate * nNextSample);
		return rmEpsError(shiftedNextSampleTime - shiftedCurrentTime);
	}

	
	static double rmEpsError(double value) {
		final Integer epsExp = 14;
        BigDecimal b = new BigDecimal(value);
        return  b.setScale(epsExp,BigDecimal.ROUND_HALF_UP).doubleValue();
	}
}
