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

import org.intocps.orchestration.coe.scala.CoeObject.GlobalState

/**
  * Created by kel on 18/07/16.
  */

object LiveStreamIntervalLogger
{

  val LIVESTREAM_FILTER_PROPERTY = "coe.livestream.filter"


  def isActive(): Boolean =
  {
    return System.getProperty(LiveStreamIntervalLogger.LIVESTREAM_FILTER_PROPERTY) != null
  }

  def getInterval(): Double =
  {
    return java.lang.Double.parseDouble(System.getProperty(LiveStreamIntervalLogger.LIVESTREAM_FILTER_PROPERTY, "0.001"))
  }
}

class LiveStreamIntervalLogger(messageDelegate: (String) => Unit, logVariables: LogVariablesContainer, interval: Double) extends LiveStreamLogger(messageDelegate, logVariables)
{
  var nextReportTime: Double = 0

  override def notifyStateChange(newState: GlobalState): Unit =
  {

    if (newState.time >= nextReportTime)
      {
        super.notifyStateChange(newState)
        setNextLogTime(newState.time)
      }
  }

  def setNextLogTime(time: Double): Unit =
  {
    nextReportTime += interval
  }


}
