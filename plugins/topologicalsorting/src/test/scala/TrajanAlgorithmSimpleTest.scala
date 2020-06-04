import org.intocps.topologicalsorting.TarjanGraph
import org.intocps.topologicalsorting.data.{AcyclicDependencyResult, CyclicDependencyResult, Edge}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.annotation.tailrec

class TarjanAlgorithmSimpleTest extends AnyFlatSpec with Matchers {
  val edge1: Edge[Int,String] = Edge(1, Set(2), "A")
  val edge2: Edge[Int, String] = Edge(2, Set(3), "A")
  val edge3: Edge[Int, String] = Edge(3, Set(4), "A")
  val edge4: Edge[Int, String] = Edge(4, Set(1), "A")
  val allEdges: Set[Edge[Int, String]] = Set(edge1, edge2, edge3, edge4)

  @tailrec
  private def generateEdges(n: Int, edges: List[Edge[Int, String]] = List[Edge[Int, String]]()): List[Edge[Int, String]] = {
    if (n == 0) edges
    else generateEdges(n - 1, edges.:+(Edge(n - 1, Set(n), "A")))
  }

  "Tarjan " should "report an algebraic loop" in {
    val tests: Iterator[Set[Edge[Int, String]]] = allEdges.toList.combinations(4).map(_.toSet)
    tests.foreach(f => {
      val g = new TarjanGraph[Int, String](f)
      assert(g.hasCycle)
      val sort = g.topologicalSort
      sort match {
        case CyclicDependencyResult(cycle) => println(cycle); assert(true)
        case AcyclicDependencyResult(totalOrder) => assert(false)
      }
    })
  }

  "Tarjan Graph with two loops" should "report two algebraic loops" in {
    val edge5: Edge[Int, String] = Edge(4, Set(5), "A")
    val edge6: Edge[Int, String] = Edge(5, Set(6), "A")
    val edge7: Edge[Int, String] = Edge(6, Set(7), "A")
    val edge8: Edge[Int, String] = Edge(7, Set(5), "A")

    val edges = allEdges.toSeq ++ (Set(edge5, edge6, edge7, edge8)).toList
    val g = new TarjanGraph[Int, String](edges)
    assert(g.hasCycle)
    assert(g.tarjanCycle.size == 2)
    val sort = g.topologicalSort
    sort match {
      case CyclicDependencyResult(cycle) => println(cycle); assert(true)
      case AcyclicDependencyResult(totalOrder) => assert(false)
    }
  }

  "Tarjan Graph with three loops" should "report three algebraic loops" in {
    val edge5: Edge[Int, String] = Edge(4, Set(5), "A")
    val edge6: Edge[Int, String] = Edge(5, Set(6), "A")
    val edge7: Edge[Int, String] = Edge(6, Set(7), "A")
    val edge8: Edge[Int, String] = Edge(6, Set(5), "A")
    val edge9: Edge[Int, String] = Edge(8, Set(7), "A")
    val edge10: Edge[Int, String] = Edge(7, Set(8), "A")

    val edges = allEdges.toSeq ++ (Set(edge5, edge6, edge7, edge8, edge9, edge10)).toList
    val g = new TarjanGraph[Int, String](edges)
    assert(g.hasCycle)
    assert(g.tarjanCycle.size == 3)
    val sort = g.topologicalSort
    sort match {
      case CyclicDependencyResult(cycle) => println(cycle); assert(true)
      case AcyclicDependencyResult(totalOrder) => assert(false)
    }
  }

  "Tarjan BigGrahp 1000+ edges" should "report AN algebraic loop" in {
    val alotOfEdges = generateEdges(1000).:+(Edge(1000, Set(1), "A"))
    val g = new TarjanGraph[Int, String](alotOfEdges)
    assert(g.hasCycle)
    val sort = g.topologicalSort
    sort match {
      case CyclicDependencyResult(cycle) => println(cycle); assert(true)
      case AcyclicDependencyResult(totalOrder) => assert(false)
    }
  }

  "Tarjan" should "report NO algebraic loop" in {
    val tests: Iterator[Set[Edge[Int, String]]] = allEdges.toList.combinations(3).map(_.toSet)
    assert(tests.forall((f: Set[Edge[Int, String]]) => {
      val g = new TarjanGraph[Int, String](f)
      val actual = g.hasCycle
      !actual
    }))
  }

  "Tarjan BigGrahp 1000+ edges" should "report NO algebraic loop" in {
    val alotOfEdges = generateEdges(1001)
    val g = new TarjanGraph[Int, String](alotOfEdges)
    assert(!g.hasCycle)
  }

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) + "ns")
    result
  }

  "Tarjan BigGrahp 100000+ edges" should "report NO algebraic loop" ignore  {
    val alotOfEdges = generateEdges(100000)
    time {
      val g = new TarjanGraph[Int, String](alotOfEdges)
      assert(!g.hasCycle)
    }
  }

  "Tarjan complicated example" should "report NO algebraic loop" in {
    val A: Edge[Char, String] = Edge('A', Set('B', 'D'), "A")
    val B: Edge[Char, String] = Edge('B', Set('E'), "A")
    val C: Edge[Char, String] = Edge('C', Set('F'), "A")
    val D: Edge[Char, String] = Edge('D', Set('E', 'F'), "A")
    val E: Edge[Char, String] = Edge('E', Set('H', 'G'), "A")
    val F: Edge[Char, String] = Edge('F', Set('G', 'I'), "A")
    val G: Edge[Char, String] = Edge('G', Set('J', 'I'), "A")
    val H: Edge[Char, String] = Edge('H', Set('J'), "A")

    val graphEdges = Set(A, B, C, D, E, F, G, H)
    val g = new TarjanGraph[Char, String](graphEdges)
    assert(!g.hasCycle)
    println(g.topologicalSortedEdges)
    val expectedOrderingEdges = List(A, D, C, F, B, E, H, G)
    // This exact assert may fail since, there is some non-determinism in finding the topological order when converting from an unordered collection (Set)
    // But the result should still be a valid topological order since a topological order is not unique for a DAG
    //assert(g.topologicalSortedEdges == expectedOrderingEdges)
  }
}
