package org.intocps.multimodelparser.data

sealed trait ParameterValue {
  def getValue() : Any
}

case class BooleanVal(v: Boolean) extends ParameterValue {
  override def getValue(): Any = v
}
case class RealVal(v: Double) extends ParameterValue {
  override def getValue(): Any = v
}
case class StringVal(v: String) extends ParameterValue{ override def getValue(): Any = v}
case class IntegerVal(v: Int) extends ParameterValue{ override def getValue(): Any = v}