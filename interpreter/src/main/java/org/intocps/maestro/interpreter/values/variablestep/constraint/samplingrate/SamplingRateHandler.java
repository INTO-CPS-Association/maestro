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

import java.util.Observable;
import java.util.Observer;

import org.intocps.maestro.interpreter.values.variablestep.CurrentSolutionPoint;
import org.intocps.maestro.interpreter.values.variablestep.InitializationMsgJson;
import org.intocps.maestro.interpreter.values.variablestep.StepsizeInterval;
import org.intocps.maestro.interpreter.values.variablestep.constraint.ConstraintHandler;


public class SamplingRateHandler implements Observer, ConstraintHandler
{
	private Double currentTime;
	private Sampling sampling;
	private StepsizeInterval interval;
	private String id;

	public SamplingRateHandler(final Observable observable,
			final InitializationMsgJson.Constraint jc, final StepsizeInterval interval)
	{
		sampling = jc.getSampling();
		this.interval = interval;
		this.id = jc.getId();
		observable.addObserver(this);
	}

	@Override
	public void update(final Observable obs, final Object arg)
	{
		if (obs instanceof CurrentSolutionPoint)
		{
			CurrentSolutionPoint cs = (CurrentSolutionPoint) obs;
			currentTime = cs.getCurrentTime();
		}
	}

	@Override
	public Double getMaxStepSize()
	{
		return Math.min(sampling.calcTimeToNextSample(currentTime), interval.getMaximalStepsize());
	}

	@Override
	public String getDecision()
	{
		return "adjust stepsize to hit next sampling time";
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public Boolean isRelaxingStrongly()
	{
		return false;
	}

	@Override
	public Boolean wasStepValid()
	{
		return true;
	}

}
