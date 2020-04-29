package org.intocps.multimodelparser.parser

import org.intocps.multimodelparser.data.{ConnectionIndependent, ConnectionScalarVariableNoContext, ConnectionType}
import org.intocps.orchestration.coe.modeldefinition.ModelDescription

import scala.jdk.CollectionConverters._

object Connections {
  def calculateInternalConnections(md: ModelDescription): Set[ConnectionIndependent] = {
    val outputs = md.getOutputs.asScala.toSet
    val deps = outputs.flatMap(x => x.outputDependencies.keySet().asScala)

    deps.map { d =>
      val from: ConnectionScalarVariableNoContext = ConnectionScalarVariableNoContext(d.name)
      val to: Set[ConnectionScalarVariableNoContext] = outputs.collect { case o if o.outputDependencies.containsKey(d) => ConnectionScalarVariableNoContext(o.name) }
      ConnectionIndependent(from, to, ConnectionType.Internal)
    }
  }
}
