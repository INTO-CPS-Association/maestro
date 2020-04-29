package org.intocps.multimodelparser.data

sealed trait ParameterValue

case class BooleanVal(v: Boolean) extends ParameterValue
case class RealVal(v: Double) extends ParameterValue
case class StringVal(v: String) extends ParameterValue
case class IntegerVal(v: Int) extends ParameterValue