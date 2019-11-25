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
import java.util
import java.util.List

import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.apache.commons.lang3.time.DurationFormatUtils
import org.intocps.orchestration.coe.config.ModelConnection.ModelInstance
import org.intocps.orchestration.coe.config.{CoeConfiguration, ModelConnection, ModelParameter}
import org.intocps.orchestration.coe.cosim.base.CoSimInitializer
import org.intocps.orchestration.coe.cosim.varstep.derivatives.DerivativeEstimator
import org.intocps.orchestration.coe.cosim.{CoSimStepSizeCalculator, ExternalSignalsStepSizeCalculator}
import org.intocps.orchestration.coe.hierarchical.IExternalSignalHandler
import org.intocps.orchestration.coe.modeldefinition.ModelDescription
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.LogCategory
import org.intocps.orchestration.coe.scala.CoeObject.{FmiSimulationInstanceScalaWrapper, GlobalState}
import org.intocps.orchestration.coe.{AbortSimulationException, BasicInitializer}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.{LinkedList, ListBuffer}

class Coe(val resultRoot: File) {
  val logger = LoggerFactory.getLogger(this.getClass)
  val configuration: CoeConfiguration = new CoeConfiguration()
  val initializer: CoSimInitializer = new BasicInitializer(this)
  val derivativeEstimator: DerivativeEstimator = new DerivativeEstimator()
  var lastError: Exception = null;
  var messageDelegate: (String) => Unit = null
  var liveStreamPrepend: Option[String] = None;
  var status: CoeStatus = CoeStatus.Unitialized
  var additionalFmuResources: ListBuffer[File] = new ListBuffer[File];
  var result: File = new File(".")
  var parameters: List[ModelParameter] = new LinkedList[ModelParameter]()
  var lastExecTime: Long = 0
  var init: Tuple3[Map[ModelConnection.ModelInstance, CoeObject.FmiSimulationInstanceScalaWrapper], CoeObject.Outputs, CoeObject.Inputs] = null
  var logVariables: LogVariablesContainer = null
  var stepSizeCalculator: CoSimStepSizeCalculator = null
  var listeners: java.util.List[IStateChangeListener] = new java.util.Vector[IStateChangeListener]()
  var stopSimulationFlag = false;
  var externalSignalHandler: IExternalSignalHandler = null;

  def setLastError(e: Exception) = lastError = e;

  def stopSimulation() = {
    stopSimulationFlag = true;
    status = CoeStatus.Stopping
  }

  /**
   * Pre: init must have a value
   *
   * @return True if all modeldescriptions allows variable communication step size, false otherwise.
   */
  def canDoVariableStep: Boolean = {
    return !this.init._1.values.exists(x => x.config.modelDescription.getCanHandleVariableCommunicationStepSize == false)
  }

  def getState: String = {

    status + ""
  }

  def getLastExecTime: Long = {
    lastExecTime
  }

  def getConfiguration: CoeConfiguration = {
    configuration
  }

  def simulate(startTime: Double, endTime: Double, debugLoggingCategories: java.util.Map[ModelInstance, java.util.List[String]], reportProgress: Boolean, liveLogInterval: Double) {

    val handle: CoeSimulationHandle = getSimulateControlHandle(startTime, endTime, debugLoggingCategories, reportProgress, liveLogInterval);
    try {
      handle.preSimulation();
      handle.simulate();
    } finally {
      handle.postSimulation();
    }
  }

  def getSimulateControlHandle(startTime: Double, endTime: Double, debugLoggingCategories: java.util.Map[ModelInstance, java.util.List[String]], reportProgress: Boolean, liveLogInterval: Double): CoeSimulationHandle = {

    new CoeSimulationHandle(this, startTime, endTime, debugLoggingCategories, reportProgress, liveLogInterval)

  }

  def setMessageDelegate(f: String => Unit): Unit = {
    this.messageDelegate = f;
  }

  def setPrepend(p: String): Unit = {
    this.liveStreamPrepend = Some(p)
  }

  def initialize(files: java.util.Map[String, URI], connections: List[ModelConnection], parameters: List[ModelParameter], stepSizeCalculator: CoSimStepSizeCalculator, logVariables: LogVariablesContainer): java.util.Map[String, java.util.List[LogCategory]] = {

    this.stepSizeCalculator = if (!configuration.hasExternalSignals) stepSizeCalculator else new ExternalSignalsStepSizeCalculator(this, stepSizeCalculator)

    this.parameters = parameters
    this.logVariables = logVariables
    val logs = CoeInitialize.initialize(files.toMap, connections.toList, resultRoot, this) match {
      case (a, b, c, logs) => {
        init = Tuple3(a, b, c)
        logs
      }
    }

    if (init == null) {
      logger.debug("Initialization failed")
      return null
    }

    resultRoot.mkdirs();
    result = new File(resultRoot, "outputs.csv")

    listeners.add(new ResultLogger(result, logVariables.getCsvResultLoggerVariables))
    status = CoeStatus.Initialized
    logs.asJava
  }

  def addResource(f: File): Unit = {
    val fName = f.getName;
    val fExt = FilenameUtils.getExtension(f.getName)
    val fBaseName = FilenameUtils.removeExtension(f.getName);
    def calcResultsFile(root: File, n: Integer): File = {
      val fileName: String = if (n == 0) fName else fBaseName + "1." + fExt;
      val result = new File(root, fileName);
      if (result.exists())
        calcResultsFile(root, n + 1);
      else
        result
    }

    val newFile = calcResultsFile(resultRoot, 0);
    FileUtils.copyFile(f, newFile);

  }

  def getResult(): File = {
    result
  }

  def getResultRoot(): File = {
    resultRoot
  }

  def notifyStateChange(newState: CoeObject.GlobalState): Unit = {
    listeners.foreach(listner => try {
      listner.notifyStateChange(newState)
    } catch {
      case e: Exception => logger.error("Caught error while notifying listener", e)
    })
  }

  def processExternalSignals(newState: CoeObject.GlobalState): CoeObject.GlobalState = {
    externalSignalHandler.processExternalSignals(newState)
  }

  def configureExternalSignalHandler(instances: Map[ModelInstance, FmiSimulationInstanceScalaWrapper]) = externalSignalHandler.configure(instances)

  private def stopAllListeners = {
    listeners.foreach(listener => try {
      listener.stop()
    } catch {
      case e: Exception => logger.error("Caught error while stopping listener", e)
    })
  }

  private def configureLivestreamListeners(endTime: Double, reportProgress: Boolean, liveLogInterval: Double) = {
    if (logVariables.livestreamVariablesCache.isDefined && logVariables.livestreamVariablesCache.get.nonEmpty) {
      if (LiveStreamIntervalLogger.isActive() || liveLogInterval != 0) {
        val interval = if (liveLogInterval > 0) {
          liveLogInterval
        } else {
          LiveStreamIntervalLogger.getInterval()
        }
        listeners.add(new LiveStreamIntervalLogger(messageDelegate, logVariables, interval, liveStreamPrepend))
      } else {
        listeners.add(new LiveStreamLogger(messageDelegate, logVariables, liveStreamPrepend))
      }
    } else if (reportProgress) {
      listeners.add(new LiveStreamProgressLogger(messageDelegate, logVariables, endTime))
    }
  }

  class CoeSimulationHandle(val coe: Coe, val startTime: Double, val endTime: Double, val debugLoggingCategories: java.util.Map[ModelInstance, java.util.List[String]], val reportProgress: Boolean, val liveLogInterval: Double) {

    var execStartTime: Long = 0
    var initialized: Boolean = false
    var state: GlobalState = _
    //    var currentTime: Double = startTime;


    def getOutputs(requestedOutputs: util.Map[ModelConnection.ModelInstance, util.Set[ModelDescription.ScalarVariable]]): util.Map[ModelInstance, util.Map[ModelDescription.ScalarVariable, Object]] = {
      val outputs: util.Map[ModelInstance, util.Map[ModelDescription.ScalarVariable, Object]] = requestedOutputs.map { case (mi, svs) => {
        val output: Option[(ModelInstance, util.Map[ModelDescription.ScalarVariable, Object])] = for {
          (stateMi_ : ModelInstance, stateSvs: InstanceState) <- state.instanceStates.find { case (stateMi_, _) => stateMi_.toString.equals(mi.toString) }
          filteredStateSvs: Map[ModelDescription.ScalarVariable, Object] = stateSvs.state.filter { case (stateSvs, _) => svs.exists(sv => sv.name.equals(stateSvs.name)) }
        } yield (stateMi_, filteredStateSvs.asJava)
        output match {
          case Some(x) => x
          case None    => throw new AbortSimulationException("Failed to retrieve an output from: " + mi.toString)
        }
      }
      }.asJava
      outputs
    }

    def updateState(modelParameter: ModelParameter, modelInstanceToUpdate: ModelInstance, valueReferenceForSvToUpdate: Long): Unit = {
      val newGlobalState = for {
        (x: ModelInstance, y: InstanceState) <- state.instanceStates.find(x => x._1.toString.equals(modelInstanceToUpdate.toString))
        newInstanceStateState: (ModelDescription.ScalarVariable, AnyRef) <- y.state.find { case (sv, _) => sv.valueReference == valueReferenceForSvToUpdate }.map { case (sv, _) => (sv, modelParameter.value) }
        updatedInstanceState = (x, new InstanceState(y.time, y.state + newInstanceStateState, y.derivatives))
      } yield new GlobalState(state.instanceStates + updatedInstanceState, state.time, state.stepSize)

      state = newGlobalState.getOrElse(throw new AbortSimulationException("Failed to set input for: " + modelParameter.variable.toString))
    }

    def preSimulation() {
      configureLivestreamListeners(endTime, reportProgress, liveLogInterval)

      execStartTime = System.currentTimeMillis()
      status = CoeStatus.Simulating

      val (initialized, initialStates) = CoeSimulator.prepareSimulation(init._1, init._2, init._3, startTime, endTime, debugLoggingCategories.asScala.toMap, coe)
      this.initialized = initialized
      this.state = initialStates
      //      this.currentTime = state.time;

    }

    /**
     * Simulate to end time
     */
    def simulate() {
      state = CoeSimulator.doSimulationStep(init._1, init._2, init._3, state, startTime, endTime, coe)
    }

    /**
     * Simulate towards endtime with delta. If delta makes current time exceed end time nothing it done
     *
     * @param delta
     */
    def simulate(delta: Double) {
      val nextTime = this.state.time + delta
      if (nextTime <= endTime) {
        state = CoeSimulator.doSimulationStep(init._1, init._2, init._3, state, state.time, nextTime, coe)
      }
      //      currentTime = nextTime
    }

    def postSimulation() {

      CoeSimulator.cleanupSimulation(init._1, initialized, Some(coe.addResource))

      val execEndTime = System.currentTimeMillis()
      lastExecTime = execEndTime - execStartTime
      val initializationTime = initializer.lastExecutionTimeMilis()
      logger.info("Total simulation time {}, initialization time {}, Simulation time {}", DurationFormatUtils.formatDuration(lastExecTime, "HH:mm:ss,SSS"), DurationFormatUtils.formatDuration(initializationTime, "HH:mm:ss,SSS"), DurationFormatUtils.formatDuration(lastExecTime - initializationTime, "HH:mm:ss,SSS"))
      status = CoeStatus.Finished

      stopAllListeners
      CoeObject.totalSimulations += 1
      logger.info("Total global simulations performed: {}", CoeObject.totalSimulations)
    }

  }

}
