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
package org.intocps.orchestration.coe.cosim.varstep.constraint;

import org.intocps.orchestration.coe.AbortSimulationException;
import org.intocps.orchestration.coe.config.ModelConnection.Variable;
import org.intocps.orchestration.coe.cosim.varstep.StepsizeInterval;
import org.intocps.orchestration.coe.cosim.varstep.constraint.boundeddifference.BoundedDifferenceHandler;
import org.intocps.orchestration.coe.cosim.varstep.constraint.samplingrate.SamplingRateHandler;
import org.intocps.orchestration.coe.cosim.varstep.constraint.zerocrossing.ZerocrossingHandler;
import org.intocps.orchestration.coe.json.InitializationMsgJson.Constraint;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.Types;
import org.slf4j.Logger;

import java.util.*;

public class ConstraintHandlerFactory
{

	public static ConstraintHandler getHandler(final Observable obs,
			final Constraint jc, final StepsizeInterval interval,
			final Double strongRelaxationFactor,
			final Map<Variable,Types> portTypeMap,
			final Logger logger)
			throws  AbortSimulationException
	{
		Map<ConstraintType,Set<Types>> validTypeMap = new HashMap<ConstraintType,Set<Types>>();
		validTypeMap.put(ConstraintType.ZEROCROSSING, new HashSet<Types>(Arrays.asList(Types.Real)));
		validTypeMap.put(ConstraintType.BOUNDEDDIFFERENCE, new HashSet<Types>(Arrays.asList(Types.Real,Types.Integer)));
		
		if (ConstraintType.ZEROCROSSING.equals(jc.getType()))
		{
			validatePortTypes(portTypeMap,validTypeMap.get(ConstraintType.ZEROCROSSING),jc,logger);
			return new ZerocrossingHandler(obs, jc, interval, strongRelaxationFactor);
		}
		if (ConstraintType.BOUNDEDDIFFERENCE.equals(jc.getType()))
		{
			validatePortTypes(portTypeMap,validTypeMap.get(ConstraintType.BOUNDEDDIFFERENCE),jc,logger);
			return new BoundedDifferenceHandler(obs, jc, interval, strongRelaxationFactor);
		}
		if (ConstraintType.SAMPLINGRATE.equals(jc.getType()))
		{
			return new SamplingRateHandler(obs, jc, interval);
		}
		if (ConstraintType.FMUMAXSTEPSIZE.equals(jc.getType()))
		{
			return new FmuMaxStepSizeHandler(jc);
		}
		return null;
	}
	
	private static void validatePortTypes(Map<Variable,Types> portTypeMap,Set<Types> validTypes,Constraint jc, Logger logger)
			throws  AbortSimulationException{
		for (Variable var:jc.getPorts()) {
			String varname = var.instance.key + "." + var.instance.instanceName + "." + var.variable;
			Boolean isFound = false;
			for (Variable port:portTypeMap.keySet()){
				if (var.instance.key.equals(port.instance.key)
						&& var.instance.instanceName.equals(port.instance.instanceName)
						&& var.variable.equals(port.variable)){
					isFound = true;
					Types type = portTypeMap.get(port);
					if (!validTypes.contains(type)){
						String errMsg = "Datatype " + type.toString() + " of variable " + varname + " is not a valid port-type for constraint of type " + jc.type + ". Please use only ports with type " + validTypes.toString();
						logger.error(errMsg);
						throw new AbortSimulationException(errMsg);
					}
					break;
				}
			}
			if (!isFound){
				String foundMsg = "Variable '"+ varname + "' and datatype could not be validated.";
				logger.error(foundMsg);
				throw new AbortSimulationException(foundMsg);
			}	
				
		}
	}

}
