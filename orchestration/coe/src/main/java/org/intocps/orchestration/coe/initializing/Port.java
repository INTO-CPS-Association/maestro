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
*		Kenneth Lausdahl
*		Casper Thule
*/
package org.intocps.orchestration.coe.initializing;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

/**
 * Created by kel on 25/07/16.
 */
public class Port
{
	public final ModelConnection.ModelInstance modelInstance;
	public final FmiSimulationInstance simInstance;
	public final ModelDescription.ScalarVariable sv;

	public Port(ModelConnection.ModelInstance modelInstance,
			FmiSimulationInstance simInstance, ModelDescription.ScalarVariable sv)
	{
		this.modelInstance = modelInstance;
		this.simInstance = simInstance;
		this.sv = sv;
	}

	@Override public boolean equals(Object obj)
	{
		if (obj instanceof Port)
			return this.modelInstance.equals(((Port) obj).modelInstance)
					&& this.simInstance.equals(((Port) obj).simInstance)
					&& sv.equals(((Port) obj).sv);
		return false;
	}

	@Override public int hashCode()
	{
		HashCodeBuilder builder = new HashCodeBuilder();
		builder.append(modelInstance);
		builder.append(simInstance);
		builder.append(sv);
		return builder.hashCode();
	}

	@Override public String toString()
	{
		return modelInstance.toString() + "." + sv.name + ": "
				+ sv.causality;
	}
}
