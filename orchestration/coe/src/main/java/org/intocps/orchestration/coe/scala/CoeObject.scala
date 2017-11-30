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
import java.io.FileNotFoundException
import java.io.IOException
import scala.collection.JavaConversions.asJavaCollection
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.immutable.Map
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.apache.log4j.FileAppender
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout
import org.apache.log4j.spi.Filter
import org.apache.log4j.spi.LoggingEvent
import org.intocps.fmi.Fmi2Status
import org.intocps.fmi.Fmi2StatusKind
import org.intocps.fmi.IFmiComponent
import org.intocps.fmi.IFmu
import org.intocps.fmi.IFmuCallback
import org.intocps.orchestration.coe.AbortSimulationException
import org.intocps.orchestration.coe.FmuFactory
import org.intocps.orchestration.coe.config.ModelConnection
import org.intocps.orchestration.coe.config.ModelConnection.ModelInstance
import org.intocps.orchestration.coe.cosim.base.FmiInstanceConfig
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance
import org.intocps.orchestration.coe.modeldefinition.ModelDescription
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.Causality
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.LogCategory
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.ModelDescriptionParseException
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.ScalarVariable
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.Types
import org.intocps.orchestration.coe.util.Util
import org.intocps.orchestration.fmi.VdmSvChecker
import org.intocps.orchestration.fmi.VdmSvChecker.ScalarVariableConfigException
import org.slf4j.LoggerFactory
import org.xml.sax.SAXParseException
import java.io.OutputStream
import scala.util.Success
import scala.util.Success

object CoeObject
{

  var totalSimulations:Long =0

  val logger = LoggerFactory.getLogger(CoeObject.getClass)

  type Inputs = Map[ModelInstance, Map[ScalarVariable, Tuple2[ModelInstance, ScalarVariable]]]
  type Outputs = Map[ModelInstance, Set[ScalarVariable]]
  type LiveStreaming = Map[ModelInstance, Set[ScalarVariable]]
  type GlobalStateMap = Map[ModelInstance, InstanceState]


  class GlobalState(val instanceStates: GlobalStateMap, val time: Double, val stepSize: Double)


  /**
    * Temporary class for a fmi component model description, It needs to fetch and cache the model description for faster simulation access
    */
  class FmiInstanceConfigScalaWrapper(modelDescription: ModelDescription,
                                      scalarVariables: List[ScalarVariable],
                                      val typeSvMap: Map[Types, Array[ScalarVariable]],
                                      val typeSvIndexMap: Map[Types, Array[Long]],
                                      val maxOutputDerivativeOrder: Int,
                                      val canGetSetState: Boolean) extends FmiInstanceConfig(modelDescription, scalarVariables)
  {}

  //
  //  /**
  //   * Class holding the simulation unit (e.i. fmi instance) and any information thats needed along with it during simulation
  //   */
  //  class FmiSimulationInstance(val instance: IFmiComponent, val config: FmiInstanceConfig)

  class FmiSimulationInstanceScalaWrapper(instance: IFmiComponent, config: FmiInstanceConfig) extends FmiSimulationInstance(instance, config)
  {}

  /**
    * New resolved state containing mappings from inputs to new values - ready to be set
    */
  class InputState(val inputs: Map[ScalarVariable, Object], val derivatives: Map[ScalarVariable, Map[Int, Double]])

  /**
    * Invokes call on all instances
    *
    * @param instances : Map of the ModelInstance and the instantiated FMU
    * @param call      : Function invoked on the instance of the instantiated FMU thar returns an Fmi2Status
    */
  def fmiCallAll(callDescription: String,instances: Map[ModelConnection.ModelInstance, FmiSimulationInstanceScalaWrapper], call: (ModelConnection.ModelInstance,IFmiComponent) => Fmi2Status) =
  {
    val setupRes = instances.map(mi =>
      mi._1 ->
        Try(call(mi._1,mi._2.instance)))

    handleCallResults(callDescription,setupRes)
  }

  /**
    * Throws an exception if any of the results invokes on the instances are failures or does not return Fmi2Status.OK.
    *
    * @param setupRes : Map of the instances and the result of an operation invoked on them.
    */
  def handleCallResults(callDescription: String, setupRes: Map[ModelConnection.ModelInstance, Try[Fmi2Status]])
  {

    if (setupRes.exists(p => p._2.isFailure) || !setupRes.forall(p => p._2.get == Fmi2Status.OK))
    {
      val msgs = setupRes.filter(p => p._2.isFailure || p._2.get != Fmi2Status.OK).map(f => f._2 match
      {
        case Success(v) => f._1 + " did not successfully complete. It returned: " + v
        case Failure(e) => f._1 + " did not successfully complete. It resulted in an internal error: " + e.getMessage
      })
      throw new AbortSimulationException(callDescription+" failed", msgs.toList)
    }
  }

  def getCoSimInstanceLogger(root: File,logName:String): org.slf4j.Logger =
  {
    //Define log pattern layout
    val layout = new PatternLayout("%d{ISO8601} %-5p - %m%n")
    val logger = LoggerFactory.getLogger("fmi.instance."+logName)
    //Define file appender with layout and output log file name
    val fileAppender = new FileAppender(layout, new File(root, logName+".log").getAbsolutePath, false)

    val f = new Filter()
    {
      def decide(event: LoggingEvent): Int =
      {
        if (event.getLoggerName.equals(logger.getName))
        {
          return Filter.ACCEPT
        }
        return Filter.DENY
      }
    }
    fileAppender.addFilter(f)
    Logger.getRootLogger().addAppender(fileAppender)

    return logger
  }
}
