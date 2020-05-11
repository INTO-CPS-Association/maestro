package org.intocps.topologicalsorting

import org.intocps.topologicalsorting.data.{AcyclicDependencyResult, CyclicDependencyResult, DependencyResult, Edge}

import scala.collection.mutable
class TarjanGraph[A](src: Iterable[Edge[A]]) {
  lazy val tarjan: mutable.Buffer[mutable.Buffer[A]] = {
    var s = mutable.Buffer.empty[A] //Stack to keep track of nodes reachable from current node
    val index = mutable.Map.empty[A, Int] //index of each node
    val lowLink = mutable.Map.empty[A, Int] //The smallest index reachable from the node
    val ret = mutable.Buffer.empty[mutable.Buffer[A]] //Keep track of SCC in graph

    def visit(v: A): Unit = {
      //Set index and lowlink of node on first visit
      index(v) = index.size
      lowLink(v) = index(v)
      //Add to stack
      s += v

      src.filter(_.from == v).flatMap(_.to).foreach(w => {
        if (!index.contains(w)) {
          //Perform DFS from node W, if node w is not explored yet
          visit(w)
        }
        if (s.contains(w)) {
          // Node w is on the stack meaning - it means there is a path from w to v
          // and since node w is a neighbor to node v there is also a path from v to w
          lowLink(v) = math.min(lowLink(w), lowLink(v))
        }
      })

      //The lowlink value haven't been updated meaning it is the root of a cycle/SCC
      if (lowLink(v) == index(v)) {
        //Add the elements to the cycle that has been added to the stack and whose lowlink has been updated by node v's lowlink
        //This is the elements on the stack that is placed behind v
        val n = s.length - s.indexOf(v)
        ret += s.takeRight(n)
        //Remove these elements from the stack
        s.dropRightInPlace(n)
      }
    }

    //Perform a DFS from  all no nodes that hasn't been explored
    src.foreach(v => if (!index.contains(v.from)) visit(v.from))
    ret
  }

  // A cycle exist if there is a SCC with at least two components
  lazy val hasCycle: Boolean = tarjan.exists(_.size >= 2)
  lazy val tarjanCycle: Iterable[Seq[A]] = tarjan.filter(_.size >= 2).distinct.map(_.toSeq).toSeq
  lazy val topologicalSortedEdges: Seq[Edge[A]] =
    if (hasCycle) Seq[Edge[A]]()
    else tarjan.flatten.reverse.flatMap(x => src.find(_.from == x)).toSeq

  lazy val topologicalSort: DependencyResult[A] =
    if (hasCycle) CyclicDependencyResult[A](tarjanCycle.map(o => o.reverse.mkString("Cycle: ", " -> ", " -> " + o.reverse.head.toString)).mkString("\n"))
    else AcyclicDependencyResult[A](tarjan.flatten.reverse.toList)
}