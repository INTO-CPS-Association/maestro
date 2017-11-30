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

import java.io.{BufferedOutputStream, File, FileOutputStream, OutputStream}

import org.intocps.orchestration.coe.modeldefinition.ModelDescription.ScalarVariable
import org.intocps.orchestration.coe.scala.CoeObject.GlobalState
import org.slf4j.LoggerFactory

import org.apache.commons.io.FileUtils
import org.intocps.orchestration.coe.config.ModelConnection.ModelInstance
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.OutputStream
import scala.collection.immutable.SortedMap

import java.io.File

/**
  * Created by kel on 18/07/16.
  */
class ResultLogger(file: File, enabledVariables: Map[ModelInstance, Set[ScalarVariable]]) extends IStateChangeListener
{

  val logger = LoggerFactory.getLogger(this.getClass)
  //logging
  var resultOutputStream: OutputStream = null

  val quoteHeaders: Boolean = System.getProperty("coe.csv.quote.header", "false").toBoolean
  val numericBooleans: Boolean = System.getProperty("coe.csv.boolean.numeric", "false").toBoolean
  var enabledPresentKeys: Set[ModelInstance] = null

  override def notifyStateChange(newState: GlobalState): Unit =
  {
    if (resultOutputStream == null)
      {
        enabledPresentKeys = newState.instanceStates.keySet.intersect(enabledVariables.keySet)
      }

    //Filter state to enabled scalar variables.
    val stateToStore: Map[ModelInstance, Map[ScalarVariable, Object]] = newState.instanceStates.filterKeys(mi => enabledPresentKeys.contains(mi)).map
    { case (mi, s) => mi -> s.state.filterKeys(enabledVariables.get(mi).get) }

    if (resultOutputStream == null)
      {
        logger.debug("Starting logger with file: {}", file)
        resultOutputStream = createLogFileHeaders(stateToStore, file)
      }

    if(logger.isTraceEnabled)
      {
        logger.trace("{}", stateToStore.flatMap
        { case (mi, s) => s.map
        { case (sv, v) => mi.instanceName + "." + sv.name + "=" + v }
        }.mkString(", "))
      }
    storeState(resultOutputStream, stateToStore, newState.time, newState.stepSize)
  }

  override def stop(): Unit =
  {
    resultOutputStream.close()
  }


  //  def getStateToStore( state: CoeObject.GlobalState): Map[ModelInstance, Map[ScalarVariable, Object]] =
  //  {
  //
  //    val r: Map[ModelInstance, Map[ScalarVariable, Object]] = state.instanceStates.filterKeys(mi=> enabledPresentKeys.contains(mi)).map{case(mi,s)=>mi->s.state.filterKeys(enabledVariables.get(mi).get)}
  //
  //    return r
  //
  //
  //  }

  implicit class ToSortedMap[A, B](tuples: TraversableOnce[(A, B)])
                                  (implicit ordering: Ordering[A])
  {
    def toSortedMap =
      SortedMap(tuples.toSeq: _*)
  }

  /**
    * Writes the log file headers
    */
  def createLogFileHeaders(initialStates: Map[ModelInstance, Map[ScalarVariable, Object]], file: File): OutputStream =
  {
    //

    val headers = initialStates.map(m => m._2.map(s => m._1.toString + "." + s._1.toString)).flatten.toList.sorted

    if (file.exists())
      {
        file.delete()
      }

    val output = new BufferedOutputStream(new FileOutputStream(file))

    var fullHeader = List("time", "step-size") ::: headers

    if (quoteHeaders)
      {
        fullHeader = fullHeader.map(item => "\"" + item + "\"")
      }

    output.write((fullHeader.mkString(",") + "\n").getBytes)
    output
  }

  def storeState(outStream: OutputStream, initialStates: Map[ModelInstance, Map[ScalarVariable, Object]], curTime: Double, communicationStepSize: Double) =
  {

    // val headers = initialStates.instanceStates.map(m => m._2.state.map(s => m._1.toString + "." + s._1.toString.toSeq)).toSeq.flatten.sorted

    val values =
      {
        val _values = initialStates.flatMap(m => m._2.map(s =>
          {
            val name = m._1.toString + "." + s._1.toString
            val value = if (numericBooleans && s._2.isInstanceOf[Boolean])
              {
                if (s._2.asInstanceOf[Boolean])
                  {
                    "1"
                  } else
                  {
                    "0"
                  }
              } else if (s._2.isInstanceOf[String])
              {
                "\"" + s._2.toString.replace("\"", "\"\"") + "\""
              } else
              {
                s._2.toString
              }
            name -> value
          }))

        _values.toSortedMap
      }.values.toList

    val fullValues = List(curTime, communicationStepSize) ::: values

    outStream.write((fullValues.mkString(",") + "\n").getBytes)
  }

}
