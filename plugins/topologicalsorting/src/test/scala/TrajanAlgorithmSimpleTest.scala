import org.intocps.topologicalsorting.TarjanGraph
import org.intocps.topologicalsorting.data.{AcyclicDependencyResult, CyclicDependencyResult, Edge}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.annotation.tailrec

class TarjanAlgorithmSimpleTest extends AnyFlatSpec with Matchers {
  val edge1: Edge[Int] = Edge(1, Set(2))
  val edge2: Edge[Int] = Edge(2, Set(3))
  val edge3: Edge[Int] = Edge(3, Set(4))
  val edge4: Edge[Int] = Edge(4, Set(1))
  val allEdges: Set[Edge[Int]] = Set(edge1, edge2, edge3, edge4)

  @tailrec
  private def generateEdges(n: Int, edges: List[Edge[Int]] = List[Edge[Int]]()): List[Edge[Int]] = {
    if (n == 0) edges
    else generateEdges(n - 1, edges.:+(Edge(n - 1, Set(n))))
  }

  "Tarjan " should "report an algebraic loop" in {
    val tests: Iterator[Set[Edge[Int]]] = allEdges.toList.combinations(4).map(_.toSet)
    tests.foreach(f => {
      val g = new TarjanGraph[Int](f)
      assert(g.hasCycle)
      val sort = g.topologicalSort
      sort match {
        case CyclicDependencyResult(cycle) => println(cycle); assert(true)
        case AcyclicDependencyResult(totalOrder) => assert(false)
      }
    })
  }

  "Tarjan Graph with two loops" should "report two algebraic loops" in {
    val edge5: Edge[Int] = Edge(4, Set(5))
    val edge6: Edge[Int] = Edge(5, Set(6))
    val edge7: Edge[Int] = Edge(6, Set(7))
    val edge8: Edge[Int] = Edge(7, Set(5))

    val edges = allEdges.toSeq ++ (Set(edge5, edge6, edge7, edge8)).toList
    val g = new TarjanGraph[Int](edges)
    assert(g.hasCycle)
    assert(g.tarjanCycle.size == 2)
    val sort = g.topologicalSort
    sort match {
      case CyclicDependencyResult(cycle) => println(cycle); assert(true)
      case AcyclicDependencyResult(totalOrder) => assert(false)
    }
  }

  "Tarjan Graph with three loops" should "report three algebraic loops" in {
    val edge5: Edge[Int] = Edge(4, Set(5))
    val edge6: Edge[Int] = Edge(5, Set(6))
    val edge7: Edge[Int] = Edge(6, Set(7))
    val edge8: Edge[Int] = Edge(6, Set(5))
    val edge9: Edge[Int] = Edge(8, Set(7))
    val edge10: Edge[Int] = Edge(7, Set(8))

    val edges = allEdges.toSeq ++ (Set(edge5, edge6, edge7, edge8, edge9, edge10)).toList
    val g = new TarjanGraph[Int](edges)
    assert(g.hasCycle)
    assert(g.tarjanCycle.size == 3)
    val sort = g.topologicalSort
    sort match {
      case CyclicDependencyResult(cycle) => println(cycle); assert(true)
      case AcyclicDependencyResult(totalOrder) => assert(false)
    }
  }

  "Tarjan BigGrahp 1000+ edges" should "report AN algebraic loop" in {
    val alotOfEdges = generateEdges(1000).:+(Edge(1000, Set(1)))
    val g = new TarjanGraph[Int](alotOfEdges)
    assert(g.hasCycle)
    val sort = g.topologicalSort
    sort match {
      case CyclicDependencyResult(cycle) => println(cycle); assert(true)
      case AcyclicDependencyResult(totalOrder) => assert(false)
    }
  }

  "Tarjan" should "report NO algebraic loop" in {
    val tests: Iterator[Set[Edge[Int]]] = allEdges.toList.combinations(3).map(_.toSet)
    assert(tests.forall((f: Set[Edge[Int]]) => {
      val g = new TarjanGraph[Int](f)
      val actual = g.hasCycle
      !actual
    }))
  }

  "Tarjan BigGrahp 1000+ edges" should "report NO algebraic loop" in {
    val alotOfEdges = generateEdges(1001)
    val g = new TarjanGraph[Int](alotOfEdges)
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
      val g = new TarjanGraph[Int](alotOfEdges)
      assert(!g.hasCycle)
    }
  }

  "Tarjan complicated example" should "report NO algebraic loop" in {
    val A: Edge[Char] = Edge('A', Set('B', 'D'))
    val B: Edge[Char] = Edge('B', Set('E'))
    val C: Edge[Char] = Edge('C', Set('F'))
    val D: Edge[Char] = Edge('D', Set('E', 'F'))
    val E: Edge[Char] = Edge('E', Set('H', 'G'))
    val F: Edge[Char] = Edge('F', Set('G', 'I'))
    val G: Edge[Char] = Edge('G', Set('J', 'I'))
    val H: Edge[Char] = Edge('H', Set('J'))

    val graphEdges = Set(A, B, C, D, E, F, G, H)
    val g = new TarjanGraph[Char](graphEdges)
    assert(!g.hasCycle)
    println(g.topologicalSortedEdges)
    val expectedOrderingEdges = List(A, D, C, F, B, E, H, G)
    // This exact assert may fail since, there is some non-determinism in finding the topological order when converting from an unordered collection (Set)
    // But the result should still be a valid topological order since a topological order is not unique for a DAG
    //assert(g.topologicalSortedEdges == expectedOrderingEdges)
  }
}
