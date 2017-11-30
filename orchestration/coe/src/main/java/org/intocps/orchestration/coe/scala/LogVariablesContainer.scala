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

import org.intocps.orchestration.coe.AbortSimulationException
import org.intocps.orchestration.coe.config.ModelConnection
import org.intocps.orchestration.coe.config.ModelConnection.ModelInstance
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.{Causality, ScalarVariable}

import scala.collection.JavaConverters._

/**
  * Created by ctha on 11-07-2016.
  */
case class LogVariablesContainer(private val livestream: java.util.Map[ModelConnection.ModelInstance, java.util.Set[ScalarVariable]], private val logVariables: java.util.Map[ModelConnection.ModelInstance, java.util.Set[ScalarVariable]])
{
  var livestreamVariablesCache: Option[Map[ModelInstance, Set[ScalarVariable]]] = _
  private var logVariablesCache: Option[Map[ModelInstance, Set[ScalarVariable]]] = _
  private var connectedOutputVariables: Map[ModelInstance, Set[ScalarVariable]] = _
  private var initialized = false

  private def checkInitialized() =
  {
    if (!initialized)
      {
        throw new AbortSimulationException("log variables container not initialized")
      }
  }

  private def parseVariables(variables: java.util.Map[ModelConnection.ModelInstance, java.util.Set[ScalarVariable]]) =
  {
    if (variables != null)
      {
        Some(variables.asScala.mapValues(_.asScala.toSet).toMap)
      }
    else
      {
        None
      }
  }

  private def getKeys(par: Option[Map[ModelInstance, Set[ScalarVariable]]]) =
  {
    par match
    {
      case None => Set()
      case Some(lsVar) => lsVar.keySet
    }
  }

  private def getModelInstances() =
  {
    getKeys(livestreamVariablesCache) ++ getKeys(logVariablesCache) ++ connectedOutputVariables.keySet;
  }

  private def getLivestreamAndLogVariables(): Map[ModelInstance, Set[ScalarVariable]] =
  {
    def getScalars(par: Option[Map[ModelInstance, Set[ScalarVariable]]], mi: ModelInstance) =
    {
      par.flatMap(x => x.get(mi)).getOrElse(Set())
    }
    val modelInstances = getKeys(livestreamVariablesCache) ++ getKeys(logVariablesCache)
    modelInstances.map(mi => mi -> (getScalars(livestreamVariablesCache, mi) ++ getScalars(logVariablesCache, mi))).toMap
  }

  def getCsvResultLoggerVariables: Map[ModelInstance, Set[ScalarVariable]] =
  {
    checkInitialized()

    val relevantModelInstances = (getKeys(logVariablesCache) ++ connectedOutputVariables.keys).toSet

    val k = relevantModelInstances.map(mi => mi -> (
      connectedOutputVariables.getOrElse(mi, Set()) ++
        {
          logVariablesCache match
          {
            case None => Set()
            case Some(miSv) => miSv.getOrElse(mi, Set())
          }
        })).toMap

    k
  }


  def initialize(lookUpVariable: (ModelInstance, ScalarVariable) => ScalarVariable, connectedOutputVariables: Map[ModelInstance, Set[ScalarVariable]]): Unit =
  {
    if(this.livestream!=null)
      {
        for (k <- this.livestream.asScala)
          {
            k match
            {
              case (mi, svs) =>
                {
                  this.livestream.put(mi, svs.asScala.map(sv => lookUpVariable(mi, sv)).asJava)
                }
            }
          }
      }

    if(this.logVariables!=null)
      {
        for (k <- this.logVariables.asScala)
          {
            k match
            {
              case (mi, svs) =>
                {
                  this.logVariables.put(mi, svs.asScala.map(sv => lookUpVariable(mi, sv)).asJava)
                }
            }
          }
      }

    this.livestreamVariablesCache = parseVariables(livestream)
    this.logVariablesCache = parseVariables(logVariables)

    this.connectedOutputVariables = connectedOutputVariables
    this.initialized = true
  }

  def getRequiredStateVariables(lookUpVariable: (ModelInstance, ScalarVariable) => ScalarVariable): scala.collection.immutable.Map[ModelInstance, Set[ScalarVariable]] =
  {
    checkInitialized()

    //Valid in this case means a causality of local or output
    def getValidVariables(variables: scala.collection.immutable.Map[ModelInstance, Set[ScalarVariable]], mi: ModelInstance): Set[ScalarVariable] =
    {

      variables.get(mi) match
      {
        case None => Set()
        case Some(sVars) =>
          val populatedSVars = sVars.map(sv => lookUpVariable(mi, sv))
          val (validSVars, invalidSVars) = populatedSVars.partition(sVar =>
            {
              sVar.causality == Causality.Output || sVar.causality == Causality.Local
            })
          if (invalidSVars.nonEmpty)
            {
              throw new AbortSimulationException("An error happened when processing the logVariables for the model instance " + mi.toString +
                ". The causality was neither local nor output for the following logVariables: " + invalidSVars.map(_.toString).mkString(", ") + ".")
            }
          else
            {
              //Look up all the validSvars to get the correct sVar
              validSVars
            }
      }


    }


    this.getModelInstances().map
    { mi =>
      mi -> (getValidVariables(this.getLivestreamAndLogVariables(), mi) ++ connectedOutputVariables.getOrElse(mi,Set()))
    }.toMap
  }

}
