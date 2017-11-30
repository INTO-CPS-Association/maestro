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

import org.intocps.orchestration.coe.modeldefinition.ModelDescription

/**
 * @author kel
 */
object ValueConverter {

  /**
   * Value converter, converts a from type where the value is read to the target type where the value will be written. Note that the units of the type must be taken into account
   */
  def convertValue(fromType: ModelDescription.Types, toType: ModelDescription.Types, value: Object): Object = {

    if (fromType == toType)
      return value
    else if (fromType == ModelDescription.Types.Boolean && toType == ModelDescription.Types.Real) {
      val v = value.asInstanceOf[Boolean]
      return if (v)
        new java.lang.Double(1.0)
      else
        new java.lang.Double(0.0)
    }
    else if (fromType == ModelDescription.Types.Boolean && toType == ModelDescription.Types.Integer)
      {
        val v = value.asInstanceOf[Boolean]
        return if (v)
          new java.lang.Integer(1)
        else
          new java.lang.Integer(0)
      }


    return value;
  }

}
