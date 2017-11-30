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
package org.intocps.orchestration.coe.scala

import org.intocps.orchestration.coe.config.ModelConnection.ModelInstance
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.ScalarVariable
import org.intocps.orchestration.coe.scala.CoeObject.GlobalState
import argonaut.Argonaut._
import argonaut._

/**
  * Created by kel on 18/07/16.
  */
class LiveStreamLogger(messageDelegate: (String) => Unit, logVariables: LogVariablesContainer) extends IStateChangeListener
{
  override def notifyStateChange(newState: GlobalState): Unit =
  {

    //Report live streaming
    if (messageDelegate != null && logVariables.livestreamVariablesCache.isDefined)
    {
      liveStreamData(messageDelegate, logVariables.livestreamVariablesCache.get, newState)
    }

  }

  override def stop(): Unit =
  {}


  /**
    * This function converts passes the output values defined in livestream to json and sends them to the messageDelegate
    * The logic structure of the json is the following:
    * fmu:{
    * instance:{
    * variable:value
    * }
    * }
    *
    * @param messageDelegate
    * @param liveStreaming
    * @param newState
    */
  def liveStreamData(messageDelegate: (String) => Unit, liveStreaming: CoeObject.Outputs, newState: CoeObject.GlobalState) =
  {
    val liveStreamingData: Predef.Map[String, Predef.Map[String, Predef.Map[String, String]]] =
      liveStreaming.groupBy
      { case (key: ModelInstance, value: Set[ScalarVariable]) => key.key }
        .map
        { case (key: String, valueMiSc: Predef.Map[ModelInstance, Set[ScalarVariable]]) =>
          key ->
            valueMiSc.map
            { case (keyMi: ModelInstance, valueSc: Set[ScalarVariable]) =>
              keyMi.instanceName ->
                valueSc.map
                { scalarVariable =>
                  scalarVariable.getName -> newState.instanceStates.get(keyMi).get.state.collectFirst
                  { case (key, value) if (key.getName == scalarVariable.getName) => value }.get.toString
                }.toMap
            }
        }
    messageDelegate(LivestreamMessage(newState.time, liveStreamingData).jencode.toString())

  }
    case class LivestreamMessage(time: Double, values: Predef.Map[String, Predef.Map[String, Predef.Map[String, String]]])
    implicit def livestreamMessageEncodeJson: EncodeJson[LivestreamMessage] =
      EncodeJson((msg: LivestreamMessage) => ("time" := msg.time) ->: ("data" := msg.values) ->: jEmptyObject)

}
