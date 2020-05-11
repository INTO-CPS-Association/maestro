package org.intocps.topologicalsorting.data

case class Edge[A](from: A, to: Set[A])
