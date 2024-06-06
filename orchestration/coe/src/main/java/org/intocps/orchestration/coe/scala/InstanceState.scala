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

import org.intocps.orchestration.coe.modeldefinition.ModelDescription.ScalarVariable

import scala.collection.JavaConversions._

class InstanceState(val time: Double,
                    val state: Map[ScalarVariable, Object],
                    val derivatives: Map[ScalarVariable, Map[Int, Double]]) {

  override def toString(): String = {

    return "Time " + time + ", Derivatives " + derivatives + ", State" + state

  }

  def mergeDerivatives(newDerivatives: scala.collection.mutable.Map[ScalarVariable, java.util.Map[java.lang.Integer, java.lang.Double]]): InstanceState = {
    val newDers = newDerivatives.map(d => d._1 -> d._2.map(op => op._1.asInstanceOf[Int] -> op._2.asInstanceOf[Double]).toMap)
    // merge and prefer original
    val ders = if(this.derivatives==null) newDers.toMap else   (this.derivatives.keys++newDers.keys).map(k=>k->(newDers.getOrElse(k,Map())++this.derivatives.getOrElse(k,Map()))).toMap
    new InstanceState(time, state, ders)
  }
}
