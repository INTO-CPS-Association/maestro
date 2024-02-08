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
package org.intocps.orchestration.coe.cosim

import org.intocps.orchestration.coe.config.ModelConnection
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance
import org.intocps.orchestration.coe.scala.{CoeObject, VariableResolver}

/**
 * Interface that provides features to determine a step size which can be taken
 * by FMI doStep
 *
 * @author kel
 *
 */
trait CoSimStepSizeCalculator {

  /**
   * Initializes the calculator with a set of components
   *
   * @param instances: map of all instances of FMUs
   * @param outputs: map of model instance to a set of output of scalar variables
   * @param inputs: map specifying where each model instancs's input scalar variable come from
   */
  def initialize(instances: Map[ModelConnection.ModelInstance, FmiSimulationInstance], outputs: CoeObject.Outputs, inputs: CoeObject.Inputs);

  /**
   * Gets the outputs required by the step-size calculator or an empty map
   */
  def getObservableOutputs(variableResolver: VariableResolver): CoeObject.Outputs

  /**
   * Calculates the step size which can be taken in the current state of the
   * components. This method will be called after new inputs on the component
   * have been set but before <code>doStep</code>
   *
   * @return a valid step size that all components will accept, and is able to
   *         perform a stable calculation based on with <code>doStep</code>
   */
  def getStepSize(currentTime: Double, globalState: CoeObject.GlobalState): Double
  
  
  /**
   * Memory function returning the last returned step-size from getStepSize
   */
  def getLastStepsize(): Double;

  /**
   * Validates that a given step completed with OK in relation to the model constraints added to the step size calculator
   */
  def validateStep(nextTime: Double,
                   newState: CoeObject.GlobalState): StepValidationResult;
  
  /**
   * Sets the end time of the co-simulation so that the calculator does not set a too large stepsize at the very last step
   */
  def setEndTime(endTime: Double)

  /**
   * Class to hold the results of a step validation. If the step was invalid then it should be retried with the reduced step-size
   */
  class StepValidationResult(val isValid: Boolean, val hasReducedStepsize: Boolean, val reducedStepsize: Double);

}
