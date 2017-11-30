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
package org.intocps.orchestration.coe.cosim.base;

import java.util.List;
import java.util.Map;

import org.intocps.fmi.FmuInvocationException;
import org.intocps.orchestration.coe.AbortSimulationException;
import org.intocps.orchestration.coe.config.*;
import org.intocps.orchestration.coe.config.ModelConnection.ModelInstance;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.ScalarVariable;

/**
 * initializer interface responsible for initializing all components.</br> Methods on this interface will be called in
 * the FMI initialization state
 * 
 * @author kel
 */
public interface CoSimInitializer
{
	/**
	 * @param inputs: Map[FMUWithInputs -> Map[InputVariable -> Tuple2[MatchingFmuWithOutput -> OutputVariable]]]
	 * @param instances: All FMU instances
	 * @param parameters
	 * @return
	 * @throws FmuInvocationException
	 */
	Map<ModelInstance, Map<ScalarVariable, Object>> initialize(
			Map<ModelInstance, Map<ScalarVariable, Tuple2<ModelInstance, ScalarVariable>>> inputs,
			Map<ModelInstance, FmiSimulationInstance> instances, List<ModelParameter> parameters)
			throws FmuInvocationException, AbortSimulationException;

	/**
	 * returns the last execution time in miliseconds
	 * @return
	 */
	long lastExecutionTimeMilis();
}
