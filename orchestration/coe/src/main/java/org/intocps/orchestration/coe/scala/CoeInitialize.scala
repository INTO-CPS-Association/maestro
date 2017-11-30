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

import java.io.{File, FileNotFoundException, IOException}
import java.net.URI

import org.intocps.fmi.{Fmi2Status, IFmu, IFmuCallback}
import org.intocps.orchestration.coe.{AbortSimulationException, FmuFactory}
import org.intocps.orchestration.coe.config.ModelConnection
import org.intocps.orchestration.coe.config.ModelConnection.ModelInstance
import org.intocps.orchestration.coe.modeldefinition.ModelDescription
import org.intocps.orchestration.coe.modeldefinition.ModelDescription._
import org.intocps.orchestration.fmi.VdmSvChecker
import org.intocps.orchestration.fmi.VdmSvChecker.ScalarVariableConfigException
import org.xml.sax.SAXParseException

import scala.collection.JavaConversions.{asJavaCollection, asScalaBuffer, bufferAsJavaList}
import scala.collection.immutable.{HashMap, Map}
import scala.util.{Failure, Try}

/**
  * @author kel
  */
object CoeInitialize
{

  import CoeObject._

  def initialize(fmuFiles: Map[String, URI], connections: List[ModelConnection], root: File, coe: Coe): (Map[ModelInstance, FmiSimulationInstanceScalaWrapper], Outputs, Inputs, Map[String, java.util.List[LogCategory]]) =
  {
    def getCountOfInstances(keys: Iterable[String], connections: List[ModelConnection]): Map[String, Int] =
    {
      keys.map(key => key ->
        connections.collect
        {
          case bothInstances: ModelConnection if bothInstances.to.instance.key == key && bothInstances.from.instance.key == key =>
            List(bothInstances.to.instance.instanceName, bothInstances.from.instance.instanceName)
          case toInstance: ModelConnection if toInstance.to.instance.key == key =>
            List(toInstance.to.instance.instanceName)
          case fromInstance: ModelConnection if fromInstance.from.instance.key == key =>
            List(fromInstance.from.instance.instanceName)
        }.flatten.toSet.size
      ).toMap
    }
    logger.info("loading fmus")

    // Get the instances for each guid.
    // This is used to verify that FMUs with CanOnlyInstanteOncePerProcess are for used once.
    val instancesCount = getCountOfInstances(fmuFiles.keys, connections)

    //http://danielwestheide.com/blog/2012/12/26/the-neophytes-guide-to-scala-part-6-error-handling-with-try.html
    val fmuMap: Map[String, (IFmu, (ModelDescription, List[ScalarVariable]))] = loadFmusFromFiles(coe.getResultRoot(), fmuFiles, instancesCount)

    val (tmpOutputs, inputs) = constructOuputInputMaps(connections, fmuMap, coe.logVariables)

    val variableResolver = new VariableResolver(fmuMap)

    val stepsizeOutputs = coe.stepSizeCalculator.getObservableOutputs(variableResolver)

    //A Set of pairs (key, value) can be converted to a map using .toMap.
    val outputs: Outputs = (tmpOutputs.keys ++ stepsizeOutputs.keys).map
    { x =>
      x ->
        {
          tmpOutputs.getOrElse(x, Set()) ++ stepsizeOutputs.getOrElse(x, Set())
        }
    }.toMap

    //load libraries
    val loadStatus = (outputs.keys ++ inputs.keys).map(mi => mi.key -> Try(fmuMap(mi.key)._1.load()))

    //a library didnt load
    if (loadStatus.exists(s => s._2.isFailure))
      {
        val msg = loadStatus.collect
        { case (res, Failure(ex)) => res + " " + ex.getMessage }
        throw new AbortSimulationException("An FMU library didn't load", msg)
      }
    //TODO check canBeInstantiatedOnlyOncePerProcess

    //instantiate
    val instances = instantiateSimulationInstances(outputs, inputs, root, fmuMap, coe)

    if(coe.getConfiguration.hasExternalSignals){
      coe.configureExternalSignalHandler(instances)
    }

    try
      {
        //validate instantiations
        if (instances.exists(p => p._2 == null))
          {
            val msg = "Invalid start condition the instances could not be created in: " + instances.filter(p => p._2 == null).map(m => m._1.key + "." + m._1.instanceName)
            logger.error(msg)
            throw new AbortSimulationException(msg)
          }

        coe.stepSizeCalculator.initialize(instances, outputs, inputs)

        //TODO check canHandleVariableCommunicationStepSize and report error if variable step size algorithm is selected

        //TODO set logging

        logger.trace("Obtaining logging categories from the modelDescription.xml files")
        val loggingCats = instances.map(f =>
          {
            f._1.key + "." + f._1.instanceName -> f._2.config.modelDescription.getLogCategories
          })

        logger.trace("Initialization done")
        (instances, outputs, inputs, loggingCats)
      } catch
      {
        case e: Exception =>
          {
            CoeSimulator.cleanupSimulation(instances,false)
            (outputs.keys ++ inputs.keys).map(mi => mi.key -> Try({
              logger.trace("Unloading library for {}", mi.key)
              fmuMap(mi.key)._1.unLoad()
            }))
            throw e;
          }
      }
  }

  def loadFmusFromFiles(sessionRoot: java.io.File, fmuFiles: Map[String, URI], instancesCount: Map[String, Int]): Map[String, (IFmu, (ModelDescription, List[ScalarVariable]))] =
  {
    //http://danielwestheide.com/blog/2012/12/26/the-neophytes-guide-to-scala-part-6-error-handling-with-try.html
    val fmuInitMap: Predef.Map[String, Try[Predef.Map[String, (IFmu, (ModelDescription, List[ScalarVariable]))]]] =
    fmuFiles.map
    { case (key, file) =>
      key -> Try(FmuFactory.create(sessionRoot, file)).flatMap
      { fmu =>
        Try(new ModelDescription(fmu.getModelDescription)).flatMap
        { modelDescription =>
          if (modelDescription.getCanBeInstantiatedOnlyOncePerProcess && instancesCount(key) > 1)
            {
              throw new InstanceConfigurationException("The FMU " + modelDescription.getModelId + " does not support multiple instances per process, and can therefore only be used with one instance. A plugin exists for fix this see the documentation 'Program properties' for the property to enable the plugin. Plugin location: http://overture.au.dk/artifactory/simple/into-cps-distribution/org/intocps/orchestration/distribution/bundle/")
            }
          //Previous: Maps the guid to the FMU. We are not interested in this anymore.
          //Future: We are interested in mapping the key to an fmu.
          Try(Map(key ->
            (fmu,
              {
                validateModelDescription(modelDescription)
                VdmSvChecker.validateModelVariables(modelDescription.getScalarVariables)
                (modelDescription, modelDescription.getScalarVariables.toList)
              })))
        }
      }
    }

    if (fmuInitMap.exists(p => p._2.isFailure))
      {
        val errors = fmuInitMap.collect
        {
          case (file, Failure(ex)) =>
            val mh = "Failed to load and parse model definition in: " + file + " Reason: "
            ex match
            {
              case e: FileNotFoundException => mh + " File not found: " + e.getMessage
              case e: java.util.zip.ZipException => mh + " Error opening zip file: \n" + e.getMessage
              case e: IOException => mh + e.getMessage
              case e: SAXParseException => mh + " Invalid ModelDescription: " + e.getMessage + " at " + e.getLineNumber + ":" + e.getColumnNumber
              case e: ScalarVariableConfigException => mh + " Invalid configuration of scalar variables: \n" + e.getMessage
              case e: ModelDescriptionParseException => mh + " Invalid configuration of model description: \n" + e.getMessage
              case e: InstanceConfigurationException => mh + "Invalid configuration of instances: " + e.getMessage
              case _ =>
                mh + ex.printStackTrace
                mh + ex.getMessage
            }
        }
        throw new AbortSimulationException(errors.fold("")((a, b) => a + "\n" + b))
      }
    fmuInitMap.filter(p => p._2.isSuccess).flatMap(f => f._2.get)
  }

  /**
    * Validate model description overall structure
    */
  def validateModelDescription(description: ModelDescription) =
  {

    val outputs = description.getScalarVariables.filter
    { sv => sv.causality == Causality.Output }
    val declaredOutputs = description.getOutputs

    val invalidDeclaredOutputs = declaredOutputs.filter
    { sv => sv.causality != Causality.Output }
    if (invalidDeclaredOutputs.nonEmpty)
      {
        throw new ScalarVariableConfigException("Declared outputs in model description model structure contains scalar variables that has Causality != Output: " + invalidDeclaredOutputs)
      }

    if (outputs.nonEmpty)
      {
        if (declaredOutputs == null || declaredOutputs.size != outputs.size || !declaredOutputs.containsAll(outputs))
          {
            throw new ScalarVariableConfigException("The model description does not declare the following outputs in the model structure: " + outputs.filter
            { x => !declaredOutputs.contains(x) })
          }
      }
  }

  def constructOuputInputMaps(connections: List[ModelConnection], fmuMap: Map[String, (IFmu, (ModelDescription, List[ScalarVariable]))], logVariables: LogVariablesContainer): (Outputs, Inputs) =
  {

    ///Find all output scalar variables
    val outputs = getOutputs(connections, fmuMap, logVariables)

    //calculate inputs
    val inMapTry: List[Try[Predef.Map[ModelInstance, Predef.Map[ScalarVariable, (ModelInstance, ScalarVariable)]]]] = connections.map
    { mc =>
      Try({
        val fromSv: ScalarVariable = fmuMap(mc.from.instance.key)._2._2.filter
        { sv => sv.getName.equals(mc.from.variable) }.head
        val listInput: List[ScalarVariable] = fmuMap(mc.to.instance.key)._2._2.filter
        { sv => sv.getName.equals(mc.to.variable) }
        if (listInput.nonEmpty)
          {
            val toSv = listInput.head
            if (toSv.causality == Causality.Input)
              {
                Map(mc.to.instance -> Map(toSv -> Tuple2(mc.from.instance, fromSv)))
              } else
              {
                throw new Exception(mc.to + "." ++ mc.to.variable + " is not an input")
              }
          } else
          {
            throw new Exception(mc.to + " does not have a scalar variable named: " + mc.from.variable)
          }
      })
    }

    //Throw exception in case of any failures
    if (inMapTry.exists(p => p.isFailure))
      {
        val messages = inMapTry.map(t => t match
        {
          case Failure(ex) => ex.getMessage
          case _ =>
        })
        throw new AbortSimulationException("Failure in input calculation. " + messages.fold("")((a, b) => a + "\n" + b))
      }

    val inMapSuccess: List[Predef.Map[ModelInstance, Predef.Map[ScalarVariable, (ModelInstance, ScalarVariable)]]] = inMapTry.map(_.get)

    //This does not work, because you have several of the same to.instance but with different sv. It is necessary to run through the already accumulated map and add it to the existing.
    //Suggestion: Create a distinct list of all the to instance, iterate through this list and add the map entries and thereby make it a list.
    val k: Set[ModelInstance] = inMapSuccess.map(p => p.keys.head).toSet


    val inputs: Inputs = k.map
    { x =>
      x -> inMapSuccess.filter(_.keys.exists
      {
        _ == x
      }).flatMap(_.values.head).toMap
    }.toMap

    logger.trace(connections.map(mc => mc.from.instance).toSet.toString)

    outputs.foreach(o =>
      {
        logger.info(String.format("Outputs from: %s = %s", o._1, o._2.map
        { x => x.name }.toString))
      })

    inputs.foreach(in =>
      {
        logger.info("Inputs for: " + in._1.toString)
        in._2.foreach
        { v => logger.info(String.format("\t%s.%s --> %s", v._2._1.toString, v._2._2.name, v._1.name)) }
      })

    (outputs, inputs)
  }

  /** Returns a map of instances and their output scalar variables
    *
    * @param connections
    * @param fmuMap
    * @return
    */
  def getOutputs(connections: List[ModelConnection], fmuMap: Map[String, (IFmu, (ModelDescription, List[ScalarVariable]))],
                 logVariables: LogVariablesContainer): Map[ModelInstance, Set[ScalarVariable]] =
  {
    //Retrueves the outputs from connections with causality=output
    val (validOutput, invalidOutput) = connections.map(mc =>
      {
        fmuMap(mc.from.instance.key)._2._2.find(sVar => sVar.getName.equals(mc.from.variable)) match
        {
          case None => Left(mc.from + " does not have a scalar variable named: " + mc.from.variable)
          case Some(sVar) =>
            if (sVar.causality == Causality.Output)
              {
                Right((mc.from.instance, sVar))
              }
            else
              {
                Left(mc.from + "." + mc.from.variable + " is not an output")
              }
        }
      }).partition(mc => mc.isRight)

    if (invalidOutput.nonEmpty)
      {
        throw new AbortSimulationException("An error happened when processing the outputs: " + invalidOutput.map(_.left.get).mkString(", "))
      }
    else
      {
        val validOutputs = validOutput.map(_.right.get)
        val connectedOutputVariables: Map[ModelInstance, Set[ScalarVariable]] = validOutputs.map
        { case (mi, _) => mi -> validOutputs.collect
        { case (modelInstance, sv) if mi == modelInstance => sv }.toSet
        }.toMap

        val  lookUpVariable: (ModelInstance, ScalarVariable) => ScalarVariable = (mi: ModelInstance, sVar: ScalarVariable)=>
        {
          fmuMap(mi.key)._2._2.find(sv => sv.getName.equals(sVar.getName)) match
          {
            case None => throw new AbortSimulationException("It was not possible to find the variable: " + sVar.getName + " in the model instance: " + mi.key + "." + mi.instanceName)
            case Some(sv) => sv
          }
        }

        logVariables.initialize(lookUpVariable, connectedOutputVariables)

        logVariables.getRequiredStateVariables(lookUpVariable)
      }
  }

  def instantiateSimulationInstances(outputs: Outputs,
                                     inputs: Inputs,
                                     root: File,
                                     fmuMap: Map[String, (IFmu, (ModelDescription, List[ScalarVariable]))], coe: Coe): Map[ModelInstance, FmiSimulationInstanceScalaWrapper] =
  {
    val formatter = "{} {} {} {}"


    //instantiate
    (outputs.keys ++ inputs.keys).map
    { mi =>
      mi ->
        {
          val fm: (IFmu, (ModelDescription, List[ScalarVariable])) = fmuMap(mi.key)
          val fmuFolder = new File(root, mi.key)
          if (!fmuFolder.exists())
            {
              fmuFolder.mkdirs()
            }
          val instanceName = mi.instanceName
          logger.debug(String.format("Loading native FMU. GUID: %s, NAME: %s", "" + fm._2._1.getGuid, "" + mi.instanceName))
          val compLogger = getCoSimInstanceLogger(fmuFolder, instanceName)

          val md: ModelDescription = fm._2._1
          logger.info("Instantiating FMU. ModelName: '{}', GUID: '{}', Vendor tool: '{}', Generated by: '{}', at: {} Execution tool required: {}", Array(md.getModelId, md.getGuid, md.getVendorToolName, md.getGenerationTool, md.getGenerationDateAndTime, md.getNeedsExecutionTool).asInstanceOf[Array[Object]]: _*)

          if (md.getNeedsExecutionTool)
            {
              logger.warn("Make sure the execution tool: '{}' is available during this simulation.", md.getVendorToolName)
            }

          val comp = fm._1.instantiate(fm._2._1.getGuid, mi.instanceName, coe.configuration.visible, coe.configuration.loggingOn, new IFmuCallback()
          {
            def log(instanceName: String, status: Fmi2Status,
                    category: String, message: String)
            {
              status match
              {
                case Fmi2Status.OK | Fmi2Status.Discard | Fmi2Status.Pending => compLogger.info(formatter, category, status, instanceName, message);
                case Fmi2Status.Error | Fmi2Status.Fatal => compLogger.error(formatter, category, status, instanceName, message);
                case Fmi2Status.Warning => compLogger.warn(formatter, category, status, instanceName, message);
                case _ => compLogger.trace(formatter, category, status, instanceName, message);
              }
            }

            def stepFinished(status: Fmi2Status)
            {
              // ignore: not using asynchronous doStep
            }
          })

          if (comp == null)
            {
              logger.error("Could not instantiate: {}.{}", mi.key,mi.instanceName:Any)
              null
            } else
            {
              val modelDescription = fm._2._1
              val scarlarVariables = fm._2._2
              //create cached get info
              val typeSvMap = scarlarVariables.filter
              { sv => outputs.keys.contains(mi) && outputs(mi).contains(sv) }.map
              { sv => Map(sv.`type`.`type` -> List(sv)) }.fold(new HashMap())((a, b) =>
                if (b.isEmpty)
                  {
                    a
                  }
                else
                  {
                    a ++ b.map{case(tp, svs) => if (a.keys.contains(tp))
                      {
                        val k = a(tp)
                        (tp, List.concat(k, svs))
                      } else
                      {
                        (tp,svs)
                      }}
                  }).map(tme => tme._1 -> tme._2.sortWith(_.valueReference < _.valueReference).toArray)

              val typeSvIndexMap = typeSvMap.map(tme => tme._1 -> tme._2.map
              { x => x.valueReference }.toArray)

              new FmiSimulationInstanceScalaWrapper(comp, new FmiInstanceConfigScalaWrapper(modelDescription, scarlarVariables, typeSvMap, typeSvIndexMap, modelDescription.getMaxOutputDerivativeOrder, modelDescription.getCanGetAndSetFmustate))
            }
        }
    }.toMap
  }
}
