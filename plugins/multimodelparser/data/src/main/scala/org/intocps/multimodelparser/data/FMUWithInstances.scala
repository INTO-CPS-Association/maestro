package org.intocps.multimodelparser.data

case class FMUWithInstances(fmu: FMUWithMD, instances: Set[Instance])
