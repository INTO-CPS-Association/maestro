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
package org.intocps.orchestration.coe.cosim

import org.intocps.orchestration.coe.config.ModelConnection
import org.intocps.orchestration.coe.config.ModelConnection.Variable
import org.intocps.orchestration.coe.scala.CoeObject
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._
import org.intocps.orchestration.coe.scala.VariableResolver
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance

class BasicFixedStepSizeCalculator(val size: Double) extends CoSimStepSizeCalculator {

  val logger = LoggerFactory.getLogger(this.getClass)

  def initialize(instances: Map[ModelConnection.ModelInstance, FmiSimulationInstance], outputs: CoeObject.Outputs, inputs: CoeObject.Inputs) = {
    logger.trace("Initializing the fixed step size calculator")

    //TODO validate the tagged variables and throw a AbortSimulationException if any issues exist where a variables doesnt exist in any instance.
    // taggedRelatedVariables.asScala.foreach(t=> println("Tag: "+t._1 +" Variables: "+ t._2.toString() ))

    //TODO initialize any needed java code
  }

  def getStepSize(currentTime: Double, state: CoeObject.GlobalState): Double = {

    logger.trace("Calculating the stepsize for the fixed step size calculator: {}",size)

    size
  }

  def getLastStepsize(): Double = { size }

  def getObservableOutputs(variableResolver: VariableResolver): CoeObject.Outputs = {
    Map()
  }

  def validateStep(nextTime: Double, newState: CoeObject.GlobalState): StepValidationResult = {
    return new StepValidationResult(true, false, 0)
  }

  def setEndTime(endTime: Double) = {}

}
