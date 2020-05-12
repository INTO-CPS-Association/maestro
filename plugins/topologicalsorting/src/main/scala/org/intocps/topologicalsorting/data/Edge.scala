package org.intocps.topologicalsorting.data

case class Edge[A,B](from: A, to: Set[A], label:B)
case class Edge11[A,B](from: A, to: A, label:B)
