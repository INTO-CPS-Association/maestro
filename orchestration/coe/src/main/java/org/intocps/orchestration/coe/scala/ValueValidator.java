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
package org.intocps.orchestration.coe.scala;

import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.slf4j.Logger;

/**
 * Created by kel on 13/12/16.
 */
public class ValueValidator
{
	public static void validate(Logger logger,
			ModelConnection.ModelInstance svOwner,
			ModelDescription.ScalarVariable sv, Object value)
	{
		ModelDescription.Type type = sv.type;
		if (type instanceof ModelDescription.RealType)
		{
			ModelDescription.RealType b = (ModelDescription.RealType) type;
			if (value instanceof Double)
			{
				Double val = (Double) value;
				if ((b.min != null && b.min > val) || (b.max != null
						&& b.max < val))
				{
					logger.warn("{} value for '{}.{}' is out of bounds [{}, {}]: {}",sv.causality, svOwner.instanceName, sv.name, b.min, b.max, val);
				}
			}
		} else if (type instanceof ModelDescription.IntegerType)
		{
			ModelDescription.IntegerType b = (ModelDescription.IntegerType) type;
			if (value instanceof Integer)
			{
				Integer val = (Integer) value;
				if ((b.min != null && b.min > val) || (b.max != null
						&& b.max < val))
				{
					logger.warn("{} value for '{}.{}' is out of bounds [{}, {}]: {}",sv.causality, svOwner.instanceName, sv.name, b.min, b.max, val);
				}
			}
		}

	}

}
