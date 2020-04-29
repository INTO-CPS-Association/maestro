
import org.intocps.multimodelparser.data.{Connection, ConnectionScalarVariable, ConnectionType, IODependencyAcyclic, IODependencyCyclic, Instance}
import org.intocps.topologicalsorting.{Edge, TarjanGraph}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TarjanAlgorithmFMUTest extends AnyFlatSpec with Matchers {
  val interCon1Input = ConnectionScalarVariable("C1InV", Instance("C1I", "C1F"))
  val interCon1Output = ConnectionScalarVariable("C1OutV", Instance("C1I", "C1F"))
  val internalCon1 = Connection(interCon1Input, Set(interCon1Output), ConnectionType.Internal)

  val interCon2Input = ConnectionScalarVariable("C2InV", Instance("C2I", "C2F"))
  val interCon2Output1 = ConnectionScalarVariable("C2OutV1", Instance("C2I", "C2F"))
  val interCon2Output2 = ConnectionScalarVariable("C2OutV2", Instance("C2I", "C2F"))
  val interCon2_1OutPut = Connection(interCon2Input, Set(interCon2Output1), ConnectionType.Internal)
  val interCon2 = Connection(interCon2Input, Set(interCon2Output1, interCon2Output2), ConnectionType.Internal)

  val interCon3Input1 = ConnectionScalarVariable("C3InV1", Instance("C3I", "C3F"))
  val interCon3Input2 = ConnectionScalarVariable("C3InV2", Instance("C3I", "C3F"))

  val ext1 = Connection(interCon1Output, Set(interCon2Input), ConnectionType.External)
  val ext2 = Connection(interCon2Output1, Set(interCon1Input), ConnectionType.External)
  val ext1_1 = Connection(interCon1Output, Set(interCon2Input, interCon3Input1), ConnectionType.External)
  val ext4 = Connection(interCon2Output2, Set(interCon3Input2), ConnectionType.External)

  val allConnections = Set(internalCon1, interCon2, ext1, ext2, ext4)


  "Tarjan Connections" should "report an algebraic loop" in {
    val tests: Iterator[Set[Connection]] = allConnections.toList.combinations(5).map(x => x.toSet)
    assert(tests.forall((f: Set[Connection]) => {
      val edges = GraphBuilder.buildGraph(f)
      val g = new TarjanGraph[ConnectionScalarVariable](edges)
      g.hasCycle
    }))
  }

  "Tarjan Connections" should "report and print an algebraic loop" in {
    val edges = GraphBuilder.buildGraph(allConnections)
    val g = new TarjanGraph[ConnectionScalarVariable](edges)
    g.hasCycle
    val actual = g.topologicalSort
    actual match {
      case IODependencyCyclic(cycle) => println(cycle); assert(true)
      case IODependencyAcyclic(totalOrder) => println(totalOrder); assert(false)
    }
  }

  "Tarjan Connections" should "report NO algebraic loops" in {
    val tests: Iterator[Set[Connection]] = allConnections.toList.combinations(3).map(x => x.toSet)
    assert(tests.forall((f: Set[Connection]) => {
      val edges = GraphBuilder.buildGraph(f)
      val g = new TarjanGraph[ConnectionScalarVariable](edges)
      !g.hasCycle
    }))
  }

  "Tarjan topologicalSort Simple" should "find the correct init ordering" in {
    val connections = Set(internalCon1, interCon2_1OutPut, ext2)
    val edges = GraphBuilder.buildGraph(connections)
    val g = new TarjanGraph[ConnectionScalarVariable](edges)
    val actual = g.topologicalSort
    val expected = new IODependencyAcyclic(List(interCon2Input, interCon2Output1, interCon1Input, interCon1Output))
    actual match {
      case IODependencyCyclic(cycle) => println(cycle); assert(false)
      case IODependencyAcyclic(totalOrder) => println(totalOrder); assert(true)
    }
    assert(actual == expected)
  }

  "Tarjan topologicalSort on nodes that have np dependencies" should "find the correct init ordering" in {
    val connections = Set(ext1_1, ext2, ext4)
    val edges = GraphBuilder.buildGraph(connections)
    val g = new TarjanGraph[ConnectionScalarVariable](edges)
    val actual = g.topologicalSort
    val expected = new IODependencyAcyclic(List(interCon2Output2, interCon3Input2, interCon2Output1, interCon1Input, interCon1Output, interCon3Input1, interCon2Input))
    actual match {
      case IODependencyCyclic(cycle) => println(cycle); assert(false)
      case IODependencyAcyclic(totalOrder) => println(totalOrder); assert(true)
    }
    assert(actual == expected)
  }

  "Tarjan topologicalSort" should "find the correct init ordering" in {
    val connections = Set(internalCon1, interCon2, ext1)
    val edges = GraphBuilder.buildGraph(connections)
    val g = new TarjanGraph[ConnectionScalarVariable](edges)
    val actual = g.topologicalSort
    val expected = new IODependencyAcyclic(List(interCon1Input, interCon1Output, interCon2Input, interCon2Output2, interCon2Output1))
    actual match {
      case IODependencyCyclic(cycle) => println(cycle); assert(false)
      case IODependencyAcyclic(totalOrder) => println(totalOrder); assert(true)
    }
    println(g.topologicalSortedEdges)
    assert(actual == expected)
  }

  "Tarjan topologicalSort complicated case" should "find the correct init ordering" in {
    val fmu1_Input = ConnectionScalarVariable("fmu1_Input", Instance("C1I", "C1F"))
    val fmu1_Output = ConnectionScalarVariable("fmu1_Output", Instance("C1I", "C1F"))
    val fmu2_Output = ConnectionScalarVariable("fmu2_Output", Instance("C2I", "C2F"))
    val fmu3_Input1 = ConnectionScalarVariable("fmu3_Input1", Instance("C3I", "C3F"))
    val fmu3_Input2 = ConnectionScalarVariable("fmu3_Input2", Instance("C3I", "C3F"))
    val fmu3_Output1 = ConnectionScalarVariable("fmu3_Output1", Instance("C3I", "C3F"))
    val fmu3_Output2 = ConnectionScalarVariable("fmu3_Output2", Instance("C3I", "C3F"))
    val fmu4_Input1 = ConnectionScalarVariable("fmu4_Input1", Instance("C4I", "C4F"))
    val fmu4_Input2 = ConnectionScalarVariable("fmu4_Input2", Instance("C4I", "C4F"))
    val fmu4_Output1 = ConnectionScalarVariable("fmu4_Output1", Instance("C4I", "C4F"))
    val fmu5_Input = ConnectionScalarVariable("fmu5_Input", Instance("C5I", "C5F"))

    //Internal connections
    val in1 = Connection(fmu1_Input, Set(fmu1_Output), ConnectionType.Internal)
    val in2 = Connection(fmu3_Input1, Set(fmu3_Output1), ConnectionType.Internal)
    val in3 = Connection(fmu3_Input2, Set(fmu3_Output2), ConnectionType.Internal)
    val in4 = Connection(fmu4_Input1, Set(fmu4_Output1), ConnectionType.Internal)
    val in5 = Connection(fmu4_Input2, Set(fmu4_Output1), ConnectionType.Internal)

    //External connections
    val ex1 = Connection(fmu1_Output, Set(fmu3_Input1), ConnectionType.External)
    val ex2 = Connection(fmu2_Output, Set(fmu3_Input2), ConnectionType.External)
    val ex3 = Connection(fmu3_Output1, Set(fmu4_Input1), ConnectionType.External)
    val ex4 = Connection(fmu3_Output2, Set(fmu4_Input1), ConnectionType.External)
    val ex5 = Connection(fmu4_Output1, Set(fmu5_Input), ConnectionType.External)

    val connections = Set(in1, in2, in3, in4, in5, ex1, ex2, ex3, ex4, ex5)
    val edges = GraphBuilder.buildGraph(connections)
    val g = new TarjanGraph[ConnectionScalarVariable](edges)
    val actual = g.topologicalSort
    val expected = new IODependencyAcyclic(List(fmu2_Output, fmu3_Input2, fmu3_Output2, fmu4_Input2, fmu1_Output, fmu3_Input1, fmu3_Output1, fmu4_Input1, fmu4_Output1, fmu5_Input))
    actual match {
      case IODependencyCyclic(cycle) => println(cycle); assert(false)
      case IODependencyAcyclic(totalOrder) => println(totalOrder); assert(true)
    }
    println(g.topologicalSortedEdges)
    // This exact assert may fail since, there is some non-determinism in finding the topological order when converting from an unordered collection (Set)
    // But the result should still be a valid topological order since a topological order is not unique for a DAG
    //assert(actual == expected)
  }
}

object GraphBuilder {
  def buildGraph(connections: Set[Connection]): List[Edge[ConnectionScalarVariable]] = {
    var edges: List[Edge[ConnectionScalarVariable]] = List[Edge[ConnectionScalarVariable]]()
    connections.foreach(con => {
      edges = edges.:+(new Edge[ConnectionScalarVariable](con.from, con.to))
    })
    edges.distinct
  }
}
