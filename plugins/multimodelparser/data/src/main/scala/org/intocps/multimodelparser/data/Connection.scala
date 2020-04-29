package org.intocps.multimodelparser.data

import ConnectionType.ConnectionType

// A connection is a from output to inputs.
case class Connection(from: ConnectionScalarVariable, to: Set[ConnectionScalarVariable], typeOf: ConnectionType)

case class ConnectionIndependent(from: ConnectionScalarVariableNoContext, to: Set[ConnectionScalarVariableNoContext], typeOf: ConnectionType)