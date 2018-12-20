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
import java.util

import org.apache.commons.lang3.StringUtils
import org.intocps.fmi._
import org.intocps.orchestration.coe.AbortSimulationException
import org.intocps.orchestration.coe.config.ModelConnection
import org.intocps.orchestration.coe.config.ModelConnection.ModelInstance
import org.intocps.orchestration.coe.cosim.base.FmiInstanceConfig
import org.intocps.orchestration.coe.hierarchical.HierarchicalCoeComponent
import org.intocps.orchestration.coe.modeldefinition.ModelDescription
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.{ScalarVariable, Types}
import org.intocps.orchestration.coe.util.{Numpy, Util}

import scala.collection.JavaConversions
import scala.collection.JavaConversions.{asJavaCollection, _}
import scala.collection.JavaConverters._
import scala.collection.immutable.{Iterable, Map}
import scala.util.Try

/**
  * @author kel
  */
object CoeSimulator
{
  //Todo: Move to central location
  type StepSize = Double
  val simulationProgramDelay: Boolean = java.lang.Boolean.parseBoolean(System.getProperty("simulation.program.delay.enable", "false"))

  val paralleliseResolveInputs: Boolean = java.lang.Boolean.parseBoolean(System.getProperty("simulation.parallelise.resolveinputs", "false"))
  val paralleliseSetInputs: Boolean = java.lang.Boolean.parseBoolean(System.getProperty("simulation.parallelise.setinputs", "false"))
  val paralleliseDoStep: Boolean = java.lang.Boolean.parseBoolean(System.getProperty("simulation.parallelise.dostep", "false"))
  val paralleliseObtainState: Boolean = java.lang.Boolean.parseBoolean(System.getProperty("simulation.parallelise.obtainstate", "false"))
  val profileExecutionTime: Boolean = java.lang.Boolean.parseBoolean(System.getProperty("simulation.profile.executiontime", "false"))


  import CoeObject._


  /**
    * Performs the simulation by the following steps:
    *
    * @param instances
    * @param outputs : Map[FMU -> set[outputVariables]]
    * @param inputs  : Map[FMUWithInputs -> Map[InputVariable -> Tuple2[MatchingFmuWithOutput -> OutputVariable]]]
    * @param startTime
    * @param endTime
    * @param logLevels
    * @param coe
    * @return
    */
  def simulate(instances: Map[ModelConnection.ModelInstance, FmiSimulationInstanceScalaWrapper],
               outputs: Outputs,
               inputs: Inputs, startTime: Double, endTime: Double, logLevels: Map[ModelInstance, java.util.List[String]], coe: Coe) =
  {
    var globalState: GlobalState = null
    var initialized = false
    try
      {
        if (logLevels != null)
          {

            setLogLevels(instances, logLevels)
          }

        fmiCallAll("Setup Experiment", instances, (instance: ModelConnection.ModelInstance, i: IFmiComponent) =>
          {
            // If the component is a hierarchical coe component, then it must be able to send data through the websocket.
            if(i.isInstanceOf[HierarchicalCoeComponent])
            {
              val hierCoeComp : HierarchicalCoeComponent = i.asInstanceOf[HierarchicalCoeComponent]
              // Directly accessing Coe as the messageDelegate is a function and it is difficult to go through hierCoeComp, since it is a java, and not scala, class.
              hierCoeComp.getCoe.setMessageDelegate(coe.messageDelegate)
              hierCoeComp.getCoe.setPrepend(instance.toString ++ ".")
            }
            logger.debug("Calling setupExperiment for {}", instance)
            i.setupExperiment(false, 0, startTime, true, endTime)

          })

        coe.initializer.initialize(CoeScalaUtil.asJava(inputs), JavaConversions.mapAsJavaMap(instances), coe.parameters)
        initialized = true

        coe.stepSizeCalculator.setEndTime(endTime)

        var initialStates: GlobalState = getInitialState(instances, outputs)
        coe.notifyStateChange(initialStates)

        if (coe.getConfiguration.parallelSimulation)
          {
            logger.info("Simulating using parallel simulation")
          }

        if (coe.configuration.hasExternalSignals)
          {
            initialStates = coe.processExternalSignals(initialStates)
          }


        globalState = doSimulationStep(instances, outputs, inputs, initialStates, startTime, endTime, coe)
      }
    finally
      {
        cleanupSimulation(instances, initialized, Some(coe.addResource))
      }
    globalState
  }

  def getRollbackStates(instances: Map[ModelInstance, FmiSimulationInstanceScalaWrapper]): Map[ModelInstance, IFmiComponentState] =
  {
    instances.filter(mi => mi._2.config.asInstanceOf[FmiInstanceConfigScalaWrapper].canGetSetState).map
    { mi =>
      mi._1 -> mi._2.instance.getState.result
    }
  }

  /**
    * Generates a new global state with new derivatives
    */
  def updateDerivatives(coe: Coe, state: GlobalState, stepsize: Double): GlobalState =
  {

    //only consider instances with real scalar variables and filter all scalar variable of those to only be reals
    val readValuesReal: Map[ModelInstance, util.Map[ScalarVariable, Object]] = state.instanceStates.filter
    { case (mi, s) => s.state.exists
    { case (sv, o) => sv.`type`.`type` == Types.Real }
    }.map
    { case (mi, instanceState) => mi -> instanceState.state.filter
    { case (sv, v) => sv.`type`.`type` == Types.Real }.asJava
    }


    val readDerivatives = state.instanceStates.map
    { case (mi, instanceState) => mi ->
      {
        if (instanceState.derivatives != null)
          {
            instanceState.derivatives.map((sv: (ScalarVariable, Predef.Map[Int, StepSize])) => sv._1 -> sv._2.map(orderDer => java.lang.Integer.valueOf(orderDer._1) -> java.lang.Double.valueOf(orderDer._2)).asJava).asJava
          } else
          {
            null
          }
      }
    }

    //this is the actual call all other code there is for conversion
    val estimatedDerivatives: util.Map[ModelInstance, util.Map[ScalarVariable, util.Map[Integer, java.lang.Double]]] = coe.derivativeEstimator.advance(readValuesReal, readDerivatives, if (-1 == stepsize)
      {
        null
      } else
      {
        stepsize
      })


    new GlobalState(state.instanceStates.map
    { case (mi, state) => mi ->
      {
        val d = estimatedDerivatives.get(mi)
        if (d != null && !d.isEmpty)
          {
            state.mergeDerivatives(estimatedDerivatives(mi))
          } else
          {
            state
          }
      }
    }, state.time, stepsize)

  }

  def timedCall[T](f: => T, message: String): T =
  {
    val execStartTime = System.currentTimeMillis()
    val res: T = f
    if (profileExecutionTime)
      {
        val execEndTime = System.currentTimeMillis()
        val lastExecTime = execEndTime - execStartTime
        logger.debug("\t--- Execution time of '" + message + "' was " + lastExecTime + " [ms]")
      }
    res
  }

  def doIntermediateStabalizationSteps(instances: Map[ModelInstance, FmiSimulationInstanceScalaWrapper],
                                       outputs: Outputs,
                                       inputs: Inputs,
                                       endTime: StepSize,
                                       coe: Coe,
                                       instanceStateMap: Map[ModelInstance, IFmiComponentState],
                                       currentCommunicationPoint: StepSize,
                                       communicationStepSize: StepSize,
                                       initState: GlobalState): GlobalState =
  {

    val globalAtol = coe.configuration.global_absolute_tolerance
    val globalRtol = coe.configuration.global_relative_tolerance

    val instancesToStabalize = instances.filter
    { case (mi, d) => d.config.modelDescription.getCanGetAndSetFmustate }

    var state = initState
    var remainingStabalizationSteps = 5
    while (remainingStabalizationSteps > 0)
      {
        //rollback instances
        instanceStateMap.filterKeys(instancesToStabalize.keySet).foreach
        { case (mi, s) => instancesToStabalize.get(mi).get.instance.setState(s) }

        // resolve inputs again aginst the newly created state which is not T+1 since rollback was called on the instance

        logger.trace("Resolving inputs for doStep")
        val resolvedInputs: Map[ModelConnection.ModelInstance, InputState] = timedCall(resolveInputs(instancesToStabalize, inputs.filterKeys(instancesToStabalize.keySet), state, coe), "resolveInputs")

        // set the inputs
        logger.trace("Setting inputs")
        timedCall(setAllInputs(instancesToStabalize, resolvedInputs, coe), "setallinputs")

        //perform the simulation step, this will throw if it cannot proceed
        val nextState = doIntermediateSimulationStep(instances, outputs, state, endTime, coe, instanceStateMap, currentCommunicationPoint, communicationStepSize)

        remainingStabalizationSteps -= 1


        val f: Iterable[Boolean] = state.instanceStates.map
        { case (mi, s) => s.state.filter
        { case (sv, _) => sv.`type`.`type` == Types.Real }.map
        { case (sv, v) =>
          {
            val b = nextState.instanceStates(mi).state.get(sv)

            val isClose = Numpy.isClose(v.asInstanceOf[Double], b.get.asInstanceOf[Double], globalAtol, globalRtol)
            if (!isClose)
              {
                logger.trace(String.format("Unstable signal %s.%s.%s = %s", mi.key, mi.instanceName, sv.name, b.get))
              }
            isClose
          }
        }.forall(v => v)


        }

        if (f.forall(v => v))
          {
            remainingStabalizationSteps = 0
          }

        state = nextState
      }
    state
  }

  def doSimulationStep(instances: Map[ModelInstance, FmiSimulationInstanceScalaWrapper],
                       outputs: Outputs,
                       inputs: Inputs,
                       initialStates: GlobalState, startTime: Double, endTime: Double, coe: Coe): GlobalState =
  {
    val execStartTime = System.currentTimeMillis()

    logger.trace("Estimate derivatives to min order 2")
    val newGlobalState = timedCall(updateDerivatives(coe, initialStates, coe.stepSizeCalculator.getLastStepsize()), "updateDerivatives")

    logger.trace("Resolving inputs for doStep")
    val resolvedInputs: Map[ModelConnection.ModelInstance, InputState] = timedCall(resolveInputs(instances, inputs, newGlobalState, coe), "resolveInputs")

    logger.trace("Setting inputs")
    timedCall(setAllInputs(instances, resolvedInputs, coe), "setallinputs")

    val instanceStateMap: Map[ModelInstance, IFmiComponentState] = timedCall(getRollbackStates(instances), "serilizeStates")

    logger.trace("Calculating step-size")
    val currentCommunicationPoint = startTime
    val communicationStepSize = timedCall(coe.stepSizeCalculator.getStepSize(currentCommunicationPoint, newGlobalState), "getStepSize")

    var newState: GlobalState = doIntermediateSimulationStep(instances, outputs, newGlobalState, endTime, coe, instanceStateMap, currentCommunicationPoint, communicationStepSize)

    if (coe.configuration.isStabalizationEnabled)
      {
        newState = doIntermediateStabalizationSteps(instances, outputs, inputs, endTime, coe, instanceStateMap, currentCommunicationPoint, communicationStepSize, newState)
      }

    // complete step we do not get here if an error occurred and a throw is executed
    logger.trace("Freeing serialized states from: " + newState.time)
    val freeStateStatuses = instanceStateMap.map
    { case (mi, state) => mi ->
      {
        if (state != null)
          {
            val status = state.freeState()
            if (status != Fmi2Status.OK)
              {
                logger.warn("Problem freeing state from '{}' returned: {}", mi: Any, status: Any)

              }
            mi -> status
          }
      }
    }

    freeStateStatuses.filter
    { case (_, s) => s == Fmi2Status.Fatal || s == Fmi2Status.Error }.foreach
    { case (mi, status) => throw new AbortSimulationException("Error freeing state for " + mi + " status: " + status) }

    timedCall(coe.notifyStateChange(newState), "notifier")

    val execEndTime = System.currentTimeMillis()

    val lastExecTime = execEndTime - execStartTime
    if (lastExecTime / 1000 > newState.stepSize)
      {
        logger.warn("Model not suited for HIL simulation. Step took too long to complete. Expected {} [s] but took {} [ms]", newState.stepSize, lastExecTime)
      }

    if (coe.configuration.hasExternalSignals)
      {
        newState = coe.processExternalSignals(newState)
      }


    val nextTime = currentCommunicationPoint + newState.stepSize

    if ((nextTime + communicationStepSize < endTime) && !coe.stopSimulationFlag)
    //Recursive
      {
        return doSimulationStep(instances, outputs, inputs, newState, nextTime, endTime, coe)
      }
    else
      {
        if (coe.stopSimulationFlag)
          {
            logger.trace("Simulation stopped");
          }
        //Base
        return newState
      }
  }


  def doIntermediateSimulationStep(instances: Map[ModelInstance, FmiSimulationInstanceScalaWrapper],
                                   outputs: Outputs,
                                   initialStates: GlobalState,
                                   endTime: Double,
                                   coe: Coe,
                                   instanceStateMap: Map[ModelInstance, IFmiComponentState],
                                   currentCommunicationPoint: Double, communicationStepSize: Double): GlobalState =
  {


    val doStepRes = timedCall(doStep(instances, instanceStateMap, currentCommunicationPoint, communicationStepSize, coe), "doStep")

    //val newCurrentTime = currentCommunicationPoint + communicationStepSize;

    val processedStepResult: Either[(DoStepResult, GlobalState), Exception] = timedCall(processResult(doStepRes, outputs, instances, currentCommunicationPoint, coe), "processResults")


    processedStepResult match
    {
      case Left(r: (DoStepResult, GlobalState)) =>
        {
          if (r._1.status == Fmi2Status.OK)
            {
              // Simulation OK
              val newCurrentTime = currentCommunicationPoint + r._1.suggestedAlternativeNewTimeStepsize
              //complete the step and prepare for next step
              logger.debug(" ## DoStep at: " + newCurrentTime + " Completed with: ok")

              return r._2
            } else
            {
              // Simulation Discard, retry smaller step
              logger.debug("### Rolling back to {}", currentCommunicationPoint)
              //reset state by rolling back
              rollback(instances, instanceStateMap)
              // roll back derivative estimation
              coe.derivativeEstimator.rollback()
              // FIXME: This is wrong. It should not suggest the value of derivatives in the future.
              val newGlobalState = updateDerivatives(coe, initialStates, r._1.suggestedAlternativeNewTimeStepsize)

              //FIXME: re-set derivatives in the FMU's

              //re-step with alternative time
              return doIntermediateSimulationStep(instances, outputs, newGlobalState, endTime, coe, instanceStateMap, currentCommunicationPoint, r._1.suggestedAlternativeNewTimeStepsize)
            }
        }

      case Right(error) => throw error
    }


  }

  def processResult(doStepRes: DoStepResult, outputs: Outputs, instances: Map[ModelInstance, FmiSimulationInstanceScalaWrapper], currentCommunicationPoint: Double, coe: Coe): Either[(DoStepResult, GlobalState), Exception] =
  {
    if (doStepRes.status == Fmi2Status.Discard)
      {
        // The reduced step size is part of doStepRes
        Left(doStepRes, null)
      } else
      {
        // The step was a success.
        getGlobalState(outputs, instances, currentCommunicationPoint + doStepRes.suggestedAlternativeNewTimeStepsize, doStepRes.suggestedAlternativeNewTimeStepsize, coe) match
        {

          case Left(newState) =>
            {
              val stepValidationRes = coe.stepSizeCalculator.validateStep(currentCommunicationPoint + doStepRes.suggestedAlternativeNewTimeStepsize, newState)

              if (!stepValidationRes.isValid)
                {
                  if (stepValidationRes.hasReducedStepsize)
                    {
                      Left(new DoStepResult(Fmi2Status.Discard, stepValidationRes.reducedStepsize), null)
                    } else
                    {
                      //                      Right(new AbortSimulationException("Invalidated step size constraints"))
                      logger.warn("The step could not be validated by the constraint at time " + currentCommunicationPoint.toString() + ". Continue nevertheless with next simulation step on smaller stepsize!");
                      Left(doStepRes, newState);
                    }
                } else
                {
                  Left(doStepRes, newState)
                }
            }
          case Right(ex) => Right(ex)
        }
      }
  }

  def rollback(instances: Map[ModelInstance, FmiSimulationInstanceScalaWrapper], instanceStateMap: Map[ModelInstance, IFmiComponentState]) =
  {
    if (!instances.forall
    {
      case (mi, instance) => instance.instance.setState(instanceStateMap(mi)) == Fmi2Status.OK
    })
      {
        throw new AbortSimulationException(String.format("Failed to reset states. Stopping Simulation NOW!"))
      }
  }

  class DoStepResult(val status: Fmi2Status, val suggestedAlternativeNewTimeStepsize: Double)

  def doStep(instances: Map[ModelConnection.ModelInstance, FmiSimulationInstanceScalaWrapper],
             instanceStateMap: Map[ModelInstance, IFmiComponentState],
             currentCommunicationPoint: Double,
             communicationStepSize: Double,
             coe: Coe): DoStepResult =
  {

    logger.trace("Calling doStep communication point {} step size {}", currentCommunicationPoint, communicationStepSize)

    if (communicationStepSize == 0)
      {
        val msg = "Aborting, reached doStep with communicationStepSize==0. This does not conform to the minimum step-size of FMI 2."
        logger.error(msg)
        throw new AbortSimulationException(msg)
      }

    val executionStartTime: Long = System.currentTimeMillis()

    val noSetFMUStatePriorToCurrentPoint = true

    val internalStepFunction = (mi: ModelInstance, instance: FmiSimulationInstanceScalaWrapper) =>
      {
        logger.trace("DoStep on '{}' currentCommunicationPoint: " + currentCommunicationPoint + " communicationStepSize: " + communicationStepSize, mi.instanceName);
        instance.instance.doStep(currentCommunicationPoint, communicationStepSize, noSetFMUStatePriorToCurrentPoint)
      };

    val doStepStatus =
      {


        if (paralleliseDoStep || coe.getConfiguration.parallelSimulation)
          {
            instances.par.map
            { case (mi, instance) => mi ->
              {
                internalStepFunction(mi, instance)
              }
            }

          } else
          {
            instances.map
            {
              case (mi, instance) => mi ->
                internalStepFunction(mi, instance)
            }
          }

      }.seq

    //error handling
    if (!doStepStatus.forall
    { x => x._2 == Fmi2Status.OK })
      {

        //something went wrong see if resun is possible
        var ex: Exception = null

        if (doStepStatus.exists(p => p._2 == Fmi2Status.Error))
          {
            //It is allowed to call the instance doStep again with another time or with new input values but it seems unlikely that it would produce a new result
            ex = new AbortSimulationException(String.format("Error simulation error. Stopping simulation NOW!. Status: %s", "" + doStepStatus))
          }

        if (doStepStatus.exists(p => p._2 == Fmi2Status.Fatal))
          {
            ex = new AbortSimulationException(String.format("Fatal simulation error. Stopping simulation NOW!. Status: %s", "" + doStepStatus))
          }

        if (ex == null && doStepStatus.exists(p => p._2 == Fmi2Status.Pending))
          {
            ex = new AbortSimulationException(String.format("A FMU returned status %s from doStep. Async doStep is not supported. Status: %s", Fmi2Status.Pending, "" + doStepStatus))
          }

        if (ex == null)
          {
            //only discard is left
            val canBackStep = instances.forall
            {
              case (mi, instance) => instance.config.asInstanceOf[FmiInstanceConfigScalaWrapper].canGetSetState && instanceStateMap.contains(mi) && instanceStateMap(mi) != null
            }
            if (!canBackStep)
              {
                ex = new AbortSimulationException(String.format("A FMU discarded the communication step current time %s step size %s and it is not possible to reset the complete simulation state of all FMUs so stopping NOW!. Status: %s", "" + currentCommunicationPoint + communicationStepSize, "" + communicationStepSize, "" + doStepStatus))
              }

            if (ex == null)
              {
                //ok we can discard the current state and reset the old and restart from previous time point
                val maxStepSize = doStepStatus.filter(p => p._2 == Fmi2Status.Discard).map(mi => mi._1 -> instances(mi._1).instance.getRealStatus(Fmi2StatusKind.LastSuccessfulTime)).map(res =>
                  if (res._2.status == Fmi2Status.OK)
                    {
                      logger.trace("Got LastSuccessfulTime: {} from " + res._1, res._2.result)
                      res._2.result
                    }
                  else
                    {
                      ex = new AbortSimulationException("no status for " + res._1)
                      res._2.result
                    })

                if (maxStepSize.isEmpty)
                  {
                    ex = new AbortSimulationException("Unable to obtain any last successful time. " + doStepStatus)
                  }

                if (ex == null)
                  {

                    maxStepSize.foreach
                    { x => logger.trace("possible rollback point: {}", x) }
                    val suggestedNewTime = maxStepSize.min
                    val suggestedNewTimeStep = suggestedNewTime - currentCommunicationPoint
                    logger.debug("Will attempt to rollback to: {} using step-size: {}", suggestedNewTime, suggestedNewTimeStep)

                    if (suggestedNewTimeStep < communicationStepSize && suggestedNewTimeStep > 0)
                      {

                        return new DoStepResult(Fmi2Status.Discard, suggestedNewTimeStep)
                      } else
                      {
                        ex = new AbortSimulationException("Unable to obtain suitable step-size for rollback. Proposed step size: " + suggestedNewTimeStep + " is rejected.")
                      }
                  }
              }
          }

        if (ex != null)
          {
            throw ex
          }

        throw new AbortSimulationException("Fatal simulation error in doStep: " + doStepStatus)
      }

    val executionEndTime: Long = System.currentTimeMillis()

    if (simulationProgramDelay || coe.getConfiguration.simulationProgramDelay)
      {
        val totalTime: Long = executionEndTime - executionStartTime
        val diff = (communicationStepSize * 1000) - totalTime
        if (diff > 0)
          {
            logger.debug("Introducing program sleep delay {}", diff);
            Thread.sleep(diff.toLong)
          }
      }

    return new DoStepResult(Fmi2Status.OK, communicationStepSize)
  }

  /**
    * *
    * Sets inputs for all instanced based on the resolved inputs
    */
  def setAllInputs(instances: Map[ModelInstance, FmiSimulationInstanceScalaWrapper], resolvedInputs: Map[ModelInstance, InputState], coe: Coe) =
  {

    val setInputFunc = (mi: ModelInstance, state: InputState) =>
      {

        val comp = instances(mi).instance

        val types = state.inputs.keySet.map
        { sv => sv.`type`.`type` }

        types.foreach
        { fmiType =>
          Util.setRaw(comp, fmiType,
            {
              val valueRefTypeMap = state.inputs.filter
              { case (sv, _) => sv.`type`.`type` == fmiType }.map
              { case (sv, v) =>
                sv.valueReference -> v
              }

              //convert res to a type java likes
              val valueRefTypeMapCasted = valueRefTypeMap.asInstanceOf[Map[java.lang.Long, java.lang.Object]]
              val valueRefTypeMapJava: java.util.Map[java.lang.Long, java.lang.Object] = scala.collection.JavaConversions.mapAsJavaMap(valueRefTypeMapCasted)
              valueRefTypeMapJava
            }.asScala)
        }

        if (instances(mi).config.modelDescription.getCanInterpolateInputs())
          {

            val inPutDerivatives: Seq[(Long, Int, StepSize)] = state.derivatives.flatMap
            { case (sv, ders) => ders.map
            { case (order, value) => (sv.valueReference, order, value) }
            }.toSeq

            if (!inPutDerivatives.isEmpty)
              {

                val valueRefs: Array[Long] = inPutDerivatives.map
                { case (valueReference, _, _) => valueReference.toLong }.toArray
                val order: Array[Int] = inPutDerivatives.map
                { case (_, order, _) => order }.toArray
                val values: Array[Double] = inPutDerivatives.map
                { case (_, _, value) => value }.toArray
                logger.trace("Calling setRealInputDerivatives. {}", Array(mi: Any, valueRefs: Any, order: Any, values: Any))
                val res = comp.setRealInputDerivatives(valueRefs, order, values)
                if (res == Fmi2Status.Fatal || res == Fmi2Status.Discard)
                  {
                    logger.error("{}.setRealInputDerivatives returned: {}", mi, res: Any)
                    throw new AbortSimulationException("setRealInputDerivatives error: " + mi)

                  } else if (res == Fmi2Status.Warning)
                  {
                    logger.warn("{}.setRealInputDerivatives returned: {}", mi, res: Any)
                  }
              }
          }
      }

    //TODO handle fmi status
    if (paralleliseSetInputs || coe.getConfiguration.parallelSimulation)
      {
        resolvedInputs.par.foreach
        { case (mi, state) =>
          setInputFunc(mi, state)
        }
      } else
      {
        resolvedInputs.foreach
        { case (mi, state) =>
          setInputFunc(mi, state)
        }
      }
  }

  def cleanupSimulation(instances: Map[ModelConnection.ModelInstance, FmiSimulationInstanceScalaWrapper], initialized: Boolean, passResult : Option[(File) => Unit]) =
  {
    //Filter none existing instances. This occurs if called after a failing instantiate.

    val filteredInstances = instances.filter(i => i._2 != null)

    if (initialized)
      {
        //instances are only allowed to be terminated if initialized
        try
          {
            fmiCallAll("Terminate", filteredInstances, (instance: ModelConnection.ModelInstance, i: IFmiComponent) =>
              {

                if (i.isInstanceOf[HierarchicalCoeComponent])
                {
                  passResult match {
                    case Some(f) => {
                      logger.debug("Calling getResult for: {}", instance)
                      f(i.asInstanceOf[HierarchicalCoeComponent].getResult());
                    }
                    case None => ()
                  }

                }
                logger.debug("Calling terminate for: {}", instance)
                i.terminate()
              })
          } catch
          {
            case ea: AbortSimulationException =>
              {
                logger.warn(ea.getMessage)
              }
          }
      }

    try
      {
        fmiCallAll("Free Instance", filteredInstances, (instance: ModelConnection.ModelInstance, i: IFmiComponent) =>
          {
            logger.debug("Calling freeInstance for: {}", instance)
            i.freeInstance()
            Fmi2Status.OK
          })
      } catch
      {
        case ea: AbortSimulationException =>
          {
            logger.warn(ea.getMessage)
          }
      }

    //Invoke unload of every DLL, not every instance!!!
    //One dll might contain multiple instances.
    filteredInstances.values.groupBy(_.instance.getFmu).keys.foreach(x => Try(x.unLoad()))
  }

  /**
    * Set the instance debug log levels
    */
  def setLogLevels(instances: Map[ModelConnection.ModelInstance, FmiSimulationInstanceScalaWrapper], logLevels: Map[ModelInstance, java.util.List[String]]) =
  {
    val logSetter = instances.filter(p => logLevels.keySet.contains(p._1) && logLevels.get(p._1).isDefined && !logLevels.get(p._1).get.isEmpty).map(in =>
      in._1 ->
        Try
        {
          val logLevel = logLevels.get(in._1)
          val instance = in._2


          //underspecified method
          logger.debug("Calling setDebugLogging for: {}, levels: " + StringUtils.join(logLevel.get, ","), instance)
          (instance, (i: IFmiComponent) => instance.instance.setDebugLogging(true, Util.getArray(logLevel.get)))

        })

    val logRes = logSetter.map(f => f._1 ->
      {
        val pair = f._2.get //FIXME remember this could go wrong
        Try(pair._2(pair._1.instance))
      })

    handleCallResults("Set Log levels", logRes)
  }


  /**
    * Maps the input variables to the output values.
    */
  def resolveInputs(instances: Map[ModelInstance, FmiSimulationInstanceScalaWrapper], inputs: Inputs, state: GlobalState, coe: Coe): Map[ModelInstance, InputState] =
  {
    /**
      * Retrieves the output values corresponding to the input scalar variables for the given model instance.
      */
    def extractInputState(sourceMi: ModelInstance, config: FmiInstanceConfig, input: Map[ScalarVariable, (ModelInstance, ScalarVariable)], globalState: GlobalState): InputState =
    {
      val instanceInputs = input.map
      { case (sv, (targetMi, targetSv)) =>
        {
          val targetVal = globalState.instanceStates(targetMi).state(targetSv)
          val targetType = targetSv.getType.`type`
          val toType = sv.getType.`type`
          val dersv: Map[Int, StepSize] = if (config.modelDescription.getCanInterpolateInputs)
            {
              val derivatives = globalState.instanceStates(targetMi).derivatives
              val maxOrder = config.modelDescription.getMaxOutputDerivativeOrder
              if (derivatives != null && derivatives.containsKey(targetSv))
                {
                  // Dymola derivative fix for setRealInputDerivatives
                  globalState.instanceStates(targetMi).derivatives(targetSv).filter
                  { case (order, _) => order <= maxOrder }
                } else
                {
                  null
                }
            } else
            {
              null
            }
          val newValue = ValueConverter.convertValue(targetType, toType, targetVal)
          ValueValidator.validate(logger, sourceMi, sv, newValue)
          sv -> (newValue, dersv)
        }
      }


      val derivativeInputs: Map[ScalarVariable, Map[Int, StepSize]] = instanceInputs.filter
      { case (_, (_, derivatives)) => derivatives != null }.map
      { case (sv, (_, derivatives)) => sv -> derivatives }

      new InputState(instanceInputs.map
      { case (sv, (value, _)) => sv -> value }, derivativeInputs)
    }



    if (paralleliseResolveInputs || coe.getConfiguration.parallelSimulation)
      {
        inputs.par.map
        { case (mi, input) => mi -> extractInputState(mi, instances(mi).config, input, state) }.seq
      }

    else
      {
        inputs.map
        { case (mi, input) => mi -> extractInputState(mi, instances(mi).config, input, state) }
      }
  }

  /**
    * Gets a new combined state for all instances
    */
  def getInitialState(instances: Map[ModelInstance, FmiSimulationInstanceScalaWrapper], outputs: Outputs): GlobalState =
  {
    new GlobalState(instances.filter(m => outputs.keys.contains(m._1)).map(m => m._1 -> new InstanceState(0, getAllScalarVariableValues(m._1, m._2), null)), 0, 0)
  }

  def getGlobalState(outputs: Outputs, instances: Map[ModelInstance, FmiSimulationInstanceScalaWrapper], curTime: Double, stepsize: Double, coe: Coe): Either[GlobalState, Exception] =
  {

    val getOut = (out: ModelInstance) =>
      {
        val simInst = instances(out)
        val md = simInst.config.modelDescription

        val derivatives = getDerivatives(simInst, md, outputs(out))

        val tmp = getAllScalarVariableValues(out, simInst)

        val s: InstanceState = new InstanceState(curTime, tmp, derivatives)

        out -> s
      }

    val state1: GlobalStateMap = if (paralleliseObtainState || coe.getConfiguration.parallelSimulation)
      {
        outputs.keys.par.map
        { out => getOut(out)
        }.seq.toMap
      } else
      {
        outputs.keys.map
        { out => getOut(out)
        }.seq.toMap
      }

    val noneOutputs = (instances.keySet -- outputs.keySet).map(m =>
      {
        m ->
          new InstanceState(curTime, Map(), null)

      }).toMap

    return Left(new GlobalState(noneOutputs ++ state1, curTime, stepsize))
  }


  /**
    * utility function to obtain derivatives for a singel simulation unit
    *
    * @param instanceWrapper
    * @param md
    * @return
    */
  def getDerivatives(instanceWrapper: FmiSimulationInstanceScalaWrapper, md: ModelDescription, outputs:Set[ScalarVariable]): Map[ScalarVariable, Map[Int, Double]] =
  {

    //get up to - MaxOutputDerivativeOrder.
    //       It delivers the maximum order of the output derivative. If the actual
    //order is lower (because the order of integration algorithm is low), the retrieved value is 0.
    //Example: If the internal polynomial is of order 1 and the master inquires the second derivative of an
    //output, the slave will return zero.

    val derMaxOrder = instanceWrapper.config.asInstanceOf[FmiInstanceConfigScalaWrapper].maxOutputDerivativeOrder // md.getMaxOutputDerivativeOrder()

    if (derMaxOrder > 0)
      {
        logger.debug("Derivative maxorder is {}", derMaxOrder)
        // All the SVs that has corresponding derivatives
        val derRoots: Array[ScalarVariable] = md.getDerivativesMap.keySet().asScala.filter(outputs).toArray[ModelDescription.ScalarVariable]
        if (derRoots.length > 0)
          {
            // An array from 1 to derivative max order
            val nativeOrdersSingeList: Array[Int] = (1 to derMaxOrder).toArray
            // An array of all the value references to pass to native code.
            // E.g. two SVs called "a" and b both have derivatives and the derivative max order is 2 it looks as: "[a,a,b,b]"
            val nativeValRefs: Array[ScalarVariable] = derRoots.flatMap(x => nativeOrdersSingeList.map(_ => x))
            //List of the orders corresponding to the example above, so it looks like: [1,2,1,2]
            val nativeOrders: Array[Int] = derRoots.flatMap(_ => (1 to derMaxOrder))
            // Get all the derivatives
            logger.debug("Reading derivatives. Refs: {} -- Orders: {}", nativeValRefs.map(x => x.valueReference), nativeOrders: Any)
            val derValuesRes: FmuResult[Array[StepSize]] = instanceWrapper.instance.getRealOutputDerivatives(nativeValRefs.map(x => x.valueReference), nativeOrders)
            logger.debug("getRealOutputDerivatives returned status: {}", derValuesRes.status)
            val derValues = derValuesRes.result;
            logger.debug("Derivatives retrieved: {}", derValues)
            // Create the structure sv -> order -> val using the return values from the FMU.
            val derivatives: Predef.Map[ScalarVariable, Predef.Map[Int, Double]] = (0 to derRoots.size - 1).toArray.map(svIndex =>
              {
                // arrayIndexStart is where to start in the return values. E.g. for "a" it is 0, for "b" it is 2 and so on.
                val arrayIndexStart = svIndex * derMaxOrder
                // Create the mapping.
                // The calculation takes the arrayIndex, adds the order and subtracts 1.
                // The subtraction is because order is 1-based whereas arrays are 0-based
                // e.g. for "a" it is 0 and 1, for "b" it is 2 and 3.
                derRoots(svIndex) -> nativeOrdersSingeList.map(order => order -> derValues((arrayIndexStart + (order - 1)))).toMap
              }).toMap
            logger.debug("Derivatives processed to: {}", derivatives)
            derivatives
          }
        else
          {
            null
          }
      } else
      {
        null
      }
  }

  /**
    * The the component state if supported
    *
    * @param instanceWrapper
    * @return
    */
  def getState(instanceWrapper: FmiSimulationInstanceScalaWrapper): Either[IFmiComponentState, Exception] =
  {

    val config = instanceWrapper.config.asInstanceOf[FmiInstanceConfigScalaWrapper]
    if (config.canGetSetState)
      {
        val res = instanceWrapper.instance.getState
        if (res.status == Fmi2Status.OK)
          {
            return Left(res.result)
          } else
          {
            return Right(new AbortSimulationException(String.format("Error simulation error. Stopping simulation NOW!. Status: %s", res.status)))
          }
      }
    Left(null)
  }


  /**
    * Obtain the values for all scalar variables for the current instance
    *
    * @param instance
    * @param comp
    * @return
    */
  def getAllScalarVariableValues(instance: ModelConnection.ModelInstance, comp: FmiSimulationInstanceScalaWrapper): Map[ScalarVariable, Object] =
  {
    val config = comp.config.asInstanceOf[FmiInstanceConfigScalaWrapper]
    val state = config.typeSvIndexMap.flatMap(tpIndx =>
      {
        val v = Util.getRaw(comp.instance, config.typeSvMap(tpIndx._1), tpIndx._2, tpIndx._1)
        if (v == null)
          {
            throw new AbortSimulationException("Unable to obtain output value for " + instance.instanceName + "." + config.typeSvMap(tpIndx._1).map(sv => sv.name).mkString("[", ",", "]"))
          }
        else
          {
            v.asScala.toMap
          }
      })

    state.foreach
    {
      case (sv, v) => ValueValidator.validate(logger, instance, sv, v)
    }
    state
  }
}
