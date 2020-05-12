# Topological Sorting

This plugin can be useful to detect a correct order to initialize the FMUs (The topological order). 
It should be noted that multiple topological order can exist. So the topological order returned by this plugin could be different from time to time, but all of the topological orders share the same properties.
In case of a cyclic dependency between the FMUs, there is no correct order. The plugin detects this cyclic dependency and tells which components the loop consists of.
Manual intervention is, in this case, needed to remove the cyclic dependency between the components.

### Terminology
* Neighbor to node _X_: A reachable node from node _"X"_ using only one edge.
* Edge: A direct connection from component _"A"_ to a set of all its neighbors.

### How to use the plugin
The plugin relies on a single generic class - TarjanGraph. This class takes in the constructor a set of all the edges in the graph. All the edges enable the algorithm to look for SCCs and top look for the topological order.

After the initialization of the class _TarjanGraph_, it should be checked for cycles using the boolean function _hasCycle_. If the graph contains cycles, the cycles can be investigated using the method _tarjanCycle_.

The plugin contains a method _topologicalSort_ that either returns the cycles in the graph in case such exists. In the case of an acyclic graph, it returns the topological order of the graph.

#### Examples:
This example shows how to get the topological order of a graph with no SCCs. 
```scala
//Generation of Edges - Edge is a generic class, so the nodes can be of an arbitrary type.
 val A: Edge[Char] = Edge('A', Set('B', 'D'))
  ...
 val H: Edge[Char] = Edge('H', Set('J'))

 val graphEdges = Set(A, B, C, D, E, F, G, H)
//Initialize generic class
 val g = new TarjanGraph[Char](graphEdges)
 //Make sure there is no cycle in the graph
 assert(!g.hasCycle)
 val topologicalOrder = g.topologicalSortedEdges
```

This example shows how to use the _topologicalSort_-method
```scala
//Initialize generic class
val g = new TarjanGraph[Int](alotOfEdges)
val sort = g.topologicalSort
sort match {
  //A cycle exist in the graph
  case IODependencyCyclic(cycle) => println(cycle); 
  //No cycle exists - topological order can be used
  case IODependencyAcyclic(topOrder) => doSomething();
}
```

#### Performance:
On a modern PC, the algorithm can, within a couple of minutes (2 minutes), investigate a graph with over 100,000 components and a similar number of edges.
All the methods of the class are lazy properties, so they will only run when called and store the result for later use.

#### Tarjan-algorithm.
The algorithm chosen for the detection of Strongly Connected Components and finding of the topological order is Tarjan-algorithm since the FMUs and their connection can be seen as a directed graph. 
Tarjan algorithm is currently being studied and proved in Isabelle by the author of this plugin, to ensure the correctness of this plugin.
