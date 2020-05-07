package org.intocps.topologicalsorting.data

sealed trait DependencyResult[A]
case class CyclicDependencyResult[A](cycle: String) extends DependencyResult[A]
case class AcyclicDependencyResult[A](totalOrder : List[A]) extends DependencyResult[A]
