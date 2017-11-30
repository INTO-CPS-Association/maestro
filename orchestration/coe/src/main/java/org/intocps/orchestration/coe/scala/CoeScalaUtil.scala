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

import org.intocps.orchestration.coe.config.ModelConnection.ModelInstance
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.ScalarVariable
import java.util.HashMap

object CoeScalaUtil {


  def asJava(inputs: Map[ModelInstance, Map[ScalarVariable, Tuple2[ModelInstance, ScalarVariable]]]): java.util.Map[ModelInstance, java.util.Map[ScalarVariable, org.intocps.orchestration.coe.cosim.base.Tuple2[ModelInstance, ScalarVariable]]] = {
    var m:java.util.Map[ModelInstance, java.util.Map[ScalarVariable, org.intocps.orchestration.coe.cosim.base.Tuple2[ModelInstance, ScalarVariable]]] = new HashMap[ModelInstance, java.util.Map[ScalarVariable, org.intocps.orchestration.coe.cosim.base.Tuple2[ModelInstance, ScalarVariable]]]()
    
    inputs.foreach(f=>{
      
      var k: java.util.Map[ScalarVariable, org.intocps.orchestration.coe.cosim.base.Tuple2[ModelInstance, ScalarVariable]] = new HashMap[ScalarVariable, org.intocps.orchestration.coe.cosim.base.Tuple2[ModelInstance, ScalarVariable]]()
      
      f._2.foreach(g=>{
        
        k.put(g._1, new org.intocps.orchestration.coe.cosim.base.Tuple2[ModelInstance, ScalarVariable](g._2._1,g._2._2))
        
      })
      m.put(f._1, k)
    })

    
    m
  }

}
