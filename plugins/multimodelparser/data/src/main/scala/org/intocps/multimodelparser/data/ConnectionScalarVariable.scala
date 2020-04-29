package org.intocps.multimodelparser.data

import scala.jdk.CollectionConverters._
// A ConnectionScalarVariable is a ScalarVariable in context of a connection
case class ConnectionScalarVariable(vName: String, vInstance: Instance) {
  def getValueReference(instances: Set[InstanceFMUWithMD]): Option[Long] = {
    instances.find(i => i.fmu.key == vInstance.fmu).flatMap(i => i.fmu.modelDescription.getScalarVariables.asScala.toList.find(sv => sv.name == vName).map(sv => sv.getValueReference))
  }

  override def toString: String = vInstance.toString + "." + vName;
}

case class ConnectionScalarVariableNoContext(vName: String)


