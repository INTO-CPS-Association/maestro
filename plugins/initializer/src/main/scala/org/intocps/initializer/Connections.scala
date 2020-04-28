
import org.intocps
import org.intocps.multimodelparser.data
import org.intocps.multimodelparser.data.{Connection, ConnectionIndependent, ConnectionScalarVariable, ConnectionScalarVariableNoContext, FMUWithMD, Instance}

object  Connections {
  /*
        Converts internal FMU scalar variables without context (just the scalar variable name)
        to scalar variable with context (scalar variable name, instance and FMU)
   */
  def FMUInternalConnectionsToConnections(fIs: Map[FMUWithMD, Set[Instance]]): Set[Connection] = {
    def IndependantToDependant(fmu: String, instance: String)(connectionIndependent: ConnectionIndependent): Connection = {
      def singleConv(cSV: ConnectionScalarVariableNoContext): ConnectionScalarVariable = {
        ConnectionScalarVariable(cSV.vName, Instance(instance, fmu))
      }
      data.Connection(
        singleConv(connectionIndependent.from),
        connectionIndependent.to.map(singleConv(_)),
        connectionIndependent.typeOf)
    }

    val res1: Set[Connection] = fIs.flatMap { case (k, v) =>
      v.flatMap(i => k.connections.map(IndependantToDependant(k.key, i.name)))
    }.toSet

    res1
  }
}
