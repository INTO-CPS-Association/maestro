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

package org.intocps.orchestration.coe.hierarchical

import org.intocps.orchestration.coe.config.ModelConnection.ModelInstance
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.ScalarVariable
import org.intocps.orchestration.coe.scala.CoeObject.GlobalState
import org.intocps.orchestration.coe.scala.InstanceState

import scala.collection.JavaConversions._

/**
  * Created by kel on 06/10/2017.
  */
object GlobalStateMerger
{
  def merge(s: GlobalState, mi: ModelInstance, toMerge: java.util.Map[ScalarVariable, Object]): GlobalState =
  {
    if (s.instanceStates.get(mi).isDefined)
      {
        val oldState = s.instanceStates.get(mi).get
        val mergedState = s.instanceStates + (mi -> new InstanceState(oldState.time, toMerge.toMap, oldState.derivatives))
        new GlobalState(mergedState, s.time, s.stepSize)
      } else
      {
        s
      }
  }
}
