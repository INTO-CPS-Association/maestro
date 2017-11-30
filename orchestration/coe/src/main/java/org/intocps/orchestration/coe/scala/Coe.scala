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

import java.io.File
import java.net.URI
import java.util.List

import org.apache.commons.lang3.time.DurationFormatUtils
import org.intocps.orchestration.coe.BasicInitializer
import org.intocps.orchestration.coe.config.ModelConnection.ModelInstance
import org.intocps.orchestration.coe.config.{CoeConfiguration, ModelConnection, ModelParameter}
import org.intocps.orchestration.coe.cosim.{CoSimStepSizeCalculator, ExternalSignalsStepSizeCalculator}
import org.intocps.orchestration.coe.cosim.base.CoSimInitializer
import org.intocps.orchestration.coe.cosim.varstep.derivatives.DerivativeEstimator
import org.intocps.orchestration.coe.hierarchical.IExternalSignalHandler
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.LogCategory
import org.intocps.orchestration.coe.scala.CoeObject.FmiSimulationInstanceScalaWrapper
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.LinkedList

class Coe(val resultRoot: File)
{
  var lastError: Exception = null;

  var messageDelegate: (String) => Unit = null
  val logger = LoggerFactory.getLogger(this.getClass)

  val configuration: CoeConfiguration = new CoeConfiguration()
  var status: CoeStatus = CoeStatus.Unitialized

  var result: File = new File(".")
  var parameters: List[ModelParameter] = new LinkedList[ModelParameter]()
  var lastExecTime: Long = 0
  var init: Tuple3[Map[ModelConnection.ModelInstance, CoeObject.FmiSimulationInstanceScalaWrapper], CoeObject.Outputs, CoeObject.Inputs] = null

  var logVariables: LogVariablesContainer = null

  val initializer: CoSimInitializer = new BasicInitializer(this)
  var stepSizeCalculator: CoSimStepSizeCalculator = null
  val derivativeEstimator: DerivativeEstimator = new DerivativeEstimator()
  var listeners: java.util.List[IStateChangeListener] = new java.util.Vector[IStateChangeListener]()

  def setLastError(e: Exception) = lastError = e;

  var stopSimulationFlag = false;

  var externalSignalHandler :IExternalSignalHandler=null;

  def stopSimulation() =
  {
    stopSimulationFlag = true;
    status = CoeStatus.Stopping
  }

  /**
    * Pre: init must have a value
    *
    * @return True if all modeldescriptions allows variable communication step size, false otherwise.
    */
  def canDoVariableStep: Boolean =
  {
    return !this.init._1.values.exists(x => x.config.modelDescription.getCanHandleVariableCommunicationStepSize == false)
  }

  def getState: String =
  {

    return status + ""
  }

  def getLastExecTime: Long =
  {
    lastExecTime
  }

  def getConfiguration: CoeConfiguration =
  {
    configuration
  }

  def simulate(startTime: Double, endTime: Double, logLevels: java.util.Map[ModelInstance, java.util.List[String]], reportProgress: Boolean, liveLogInterval: Double)
  {
    if (logVariables.livestreamVariablesCache.isDefined && logVariables.livestreamVariablesCache.get.nonEmpty)
      {
        if (LiveStreamIntervalLogger.isActive() || liveLogInterval != 0)
          {
            val interval = if (liveLogInterval > 0)
              {
                liveLogInterval
              } else
              {
                LiveStreamIntervalLogger.getInterval()
              }
            listeners.add(new LiveStreamIntervalLogger(messageDelegate, logVariables, interval))
          } else
          {
            listeners.add(new LiveStreamLogger(messageDelegate, logVariables))
          }
      } else if (reportProgress)
      {
        listeners.add(new LiveStreamProgressLogger(messageDelegate, logVariables, endTime))
      }
    status = CoeStatus.Simulating
    val execStartTime = System.currentTimeMillis()
    status = CoeStatus.Simulating
    CoeSimulator.simulate(init._1, init._2, init._3, startTime, endTime, logLevels.asScala.toMap, this)
    val execEndTime = System.currentTimeMillis()
    lastExecTime = execEndTime - execStartTime
    val initializationTime = this.initializer.lastExecutionTimeMilis()
    logger.info("Total simulation time {}, initialization time {}, Simulation time {}", DurationFormatUtils.formatDuration(lastExecTime, "HH:mm:ss,SSS"), DurationFormatUtils.formatDuration(initializationTime, "HH:mm:ss,SSS"), DurationFormatUtils.formatDuration(lastExecTime - initializationTime, "HH:mm:ss,SSS"))
    status = CoeStatus.Finished

    listeners.foreach(listener => try
      {
        listener.stop()
      } catch
      {
        case e: Exception => logger.error("Caught error while stopping listener", e)
      })
    CoeObject.totalSimulations += 1
    logger.info("Total global simulations performed: {}", CoeObject.totalSimulations)

  }


  def initialize(files: java.util.Map[String, URI], connections: List[ModelConnection], parameters: List[ModelParameter], stepSizeCalculator: CoSimStepSizeCalculator, logVariables: LogVariablesContainer): java.util.Map[String, java.util.List[LogCategory]] =
  {

    this.stepSizeCalculator = if(!configuration.hasExternalSignals)stepSizeCalculator else new ExternalSignalsStepSizeCalculator(this,stepSizeCalculator)

    this.parameters = parameters
    this.logVariables = logVariables
    val logs = CoeInitialize.initialize(files.toMap, connections.toList, resultRoot, this) match
    {
      case (a, b, c, logs) =>
        {
          init = Tuple3(a, b, c)
          logs
        }
    }

    if (init == null)
      {
        logger.debug("Initialization failed")
        return null
      }

    result = new File(resultRoot, "outputs.csv")
    result.mkdirs()

    listeners.add(new ResultLogger(result, logVariables.getCsvResultLoggerVariables))
    status = CoeStatus.Initialized
    logs.asJava
  }

  def getResult(): File =
  {
    result
  }

  def getResultRoot(): File =
  {
    resultRoot
  }

  def notifyStateChange(newState: CoeObject.GlobalState): Unit =
  {
    listeners.foreach(listner => try
      {
        listner.notifyStateChange(newState)
      } catch
      {
        case e: Exception => logger.error("Caught error while notifying listener", e)
      })
  }

  def processExternalSignals(newState: CoeObject.GlobalState): CoeObject.GlobalState =
  {
    externalSignalHandler.processExternalSignals(newState)
  }

  def configureExternalSignalHandler(instances: Map[ModelInstance, FmiSimulationInstanceScalaWrapper]) = externalSignalHandler.configure(instances)

}
