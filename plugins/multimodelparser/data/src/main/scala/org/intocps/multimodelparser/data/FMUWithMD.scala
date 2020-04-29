package org.intocps.multimodelparser.data

import org.intocps.orchestration.coe.modeldefinition.ModelDescription
case class FMUWithMD (key: String, modelDescription: ModelDescription, connections: Set[ConnectionIndependent]){
  def keyNoBrackets = key.substring(1, key.length-1)
}