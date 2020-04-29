package org.intocps.multimodelparser.data

case class Instance(name: String, fmu: String){
  override def toString: String = fmu+"."+name;
}
