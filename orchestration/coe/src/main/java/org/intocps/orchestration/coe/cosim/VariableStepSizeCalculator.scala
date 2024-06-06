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

import org.intocps.fmi.Fmi2Status
import org.intocps.orchestration.coe.config.ModelConnection
import org.intocps.orchestration.coe.config.ModelConnection.Variable
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance
import org.intocps.orchestration.coe.cosim.varstep.{StepsizeCalculator, StepsizeInterval}
import org.intocps.orchestration.coe.json.InitializationMsgJson
import org.intocps.orchestration.coe.json.InitializationMsgJson.Constraint
import org.intocps.orchestration.coe.scala.CoeObject.FmiInstanceConfigScalaWrapper
import org.intocps.orchestration.coe.scala.{CoeObject, VariableResolver}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.JavaConverters.mapAsJavaMapConverter

class VariableStepSizeCalculator(constraints: java.util.Set[Constraint],
                                 val stepsizeInterval: StepsizeInterval,
                                 initialStepsize: java.lang.Double) extends CoSimStepSizeCalculator {

  var calc: StepsizeCalculator = null
  var supportsRollback: Boolean = true

  val logger = LoggerFactory.getLogger(this.getClass)

  var instances: Map[ModelConnection.ModelInstance, FmiSimulationInstance] = null

  var lastStepsize: Double = -1

  def initialize(instances: Map[ModelConnection.ModelInstance, FmiSimulationInstance], outputs: CoeObject.Outputs, inputs: CoeObject.Inputs) = {
    logger.trace("Initializing the variable step size calculator")

    supportsRollback = instances.forall(x => x match {
      case (mi, instance) => instance.config.asInstanceOf[FmiInstanceConfigScalaWrapper].canGetSetState
    })
    calc = new StepsizeCalculator(constraints, stepsizeInterval, initialStepsize, instances.asJava);

    this.instances = instances
  }

  // Invoke GetMaxStepSize on all FMUs.
  // Precondition: acc = maxStepSize.
  // Invariant: minStepSize <= acc <= maxStepSize
  // Postcondition: minStepSize <= result <= maxStepSize
  // Req. 1: If ANY are less than minStepsize, choose minStepsize.
  // Req. 2: If ALL are greater than maxStepsize, choose maxStepsize
  // Req. 3: If ANY are between minStepsize and maxStepsize AND none are less than minStepsize, then choose minimum of these.
  // Req. 4: If NONE succeeds in getMaxStepSize, choose maxStepsize
  def calcFMUsMaxStepSize(minStepsize: Double, maxStepSize: Double, instances: Map[ModelConnection.ModelInstance, FmiSimulationInstance]): Double = {
    def f(acc: Double, instances: Map[ModelConnection.ModelInstance, FmiSimulationInstance]): Double = {
      if (instances.isEmpty) {
        // Due to precondition and invariant, minStepSize <= acc <= maxStepSize
        return acc;
      }
      else {
        val (mi, simInst) = instances.head;
        val result = simInst.instance.getMaxStepSize;
        if (result.status != Fmi2Status.OK) {
          logger.debug("GetMaxStepSize failed for the FMU: " + mi + ". The return was " + result.status);
          f(acc, instances.tail);
        }
        else {
          if (result.result < minStepsize) {
            // Req. 1 -> FMU returns step size such that step size < minStepSize, choose minStepsize
            logger.warn("'{}'.GetMaxStepSize = {} is smaller than minimum {} diff {}. Choosing minimum", mi.toString, result.result.toString, minStepsize.toString, (minStepsize - result.result).toString);
            return minStepsize;
          }
          else {
            logger.trace("'{}'.GetMaxStepSize = {}", mi, result.result: Any)
            // FMU returns step size such that minStepSize <= step size, choose step size or acc
            if (result.result < acc) {
              // FMU returns step size such that minStepSize <= step size < acc <= maxStepSize. Choose step size.
              f(result.result, instances.tail);
            }
            else {
              // FMU returns step size such that minStepsize <= acc <= step size, choose acc.
              f(acc, instances.tail);
            }
          }
        }
      }
    }

    f(maxStepSize, instances);
  }

  def getStepSize(currentTime: Double, state: CoeObject.GlobalState): Double = {

    logger.trace("Calculating the stepsize for the variable step size calculator")

    val data = state.instanceStates.map(s => s._1 -> s._2.state.asJava)
    val der = state.instanceStates.map(s => s._1 -> {
      if (s._2.derivatives != null) {
        s._2.derivatives.map(derMap => derMap._1 -> derMap._2.map(orderValMap => java.lang.Integer.valueOf(orderValMap._1) -> java.lang.Double.valueOf(orderValMap._2)).asJava).asJava
      } else {
        null
      }
    })

    val minStepsize = stepsizeInterval.getMinimalStepsize
    val maxStepsize = stepsizeInterval.getMaximalStepsize

    val FMUsMaxStepSize = calcFMUsMaxStepSize(minStepsize, maxStepsize, instances);

    logger.trace("Max FMU step size used by step-size calculator: {}", FMUsMaxStepSize)

    lastStepsize = calc.getStepsize(currentTime, data.asJava, if (der != null) {
      der.asJava
    } else {
      null
    }, FMUsMaxStepSize)


    logger.trace("Calculated step size: {}", lastStepsize)
    return lastStepsize
  }

  def getLastStepsize(): Double = {
    lastStepsize
  }

  def getObservableOutputs(variableResolver: VariableResolver): CoeObject.Outputs = {

    //TODO rewrite this into a function and reuse in the coe for get output and input
    val tmp = constraints.toSet[Constraint].map { x =>
      x.getPorts.toSet[Variable].map { v => variableResolver.resolve(v) }
    }
    val t2 = tmp.flatten
    val t22 = t2.map(f => f._1)
    val t222 = t22.map { x => x -> t2.filter(p => p._1 == x).map(f => f._2) }
    val t = t222.toMap

    t
  }

  def validateStep(nextTime: Double, newState: CoeObject.GlobalState): StepValidationResult = {
    val state = newState.instanceStates.map(s => s._1 -> s._2.state.asJava);
    val r = calc.validateStep(nextTime, state.asJava, supportsRollback)
    return new StepValidationResult(r.isValid(), r.hasReducedStepsize(), r.getStepsize)
  }

  def setEndTime(endTime: Double) = {
    calc.setEndTime(endTime)
  }

}
