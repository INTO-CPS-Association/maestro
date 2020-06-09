import org.intocps.topologicalsorting.TarjanGraph
import org.intocps.topologicalsorting.data.{AcyclicDependencyResult, CyclicDependencyResult, Edge11}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.annotation.tailrec

class TarjanAlgorithmSimpleTest extends AnyFlatSpec with Matchers {
  val Edge111: Edge11[Int,String] = Edge11(1, 2, "A")
  val Edge112: Edge11[Int, String] = Edge11(2, 3, "A")
  val Edge113: Edge11[Int, String] = Edge11(3, 4, "A")
  val Edge114: Edge11[Int, String] = Edge11(4, 1, "A")
  val allEdge11s: Set[Edge11[Int, String]] = Set(Edge111, Edge112, Edge113, Edge114)

  @tailrec
  private def generateEdge11s(n: Int, Edge11s: List[Edge11[Int, String]] = List[Edge11[Int, String]]()): List[Edge11[Int, String]] = {
    if (n == 0) Edge11s
    else generateEdge11s(n - 1, Edge11s.:+(Edge11(n - 1, n, "A")))
  }

  "Tarjan " should "report an algebraic loop" in {
    val tests: Iterator[Set[Edge11[Int, String]]] = allEdge11s.toList.combinations(4).map(_.toSet)
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
    val Edge115: Edge11[Int, String] = Edge11(4, 5, "A")
    val Edge116: Edge11[Int, String] = Edge11(5, 6, "A")
    val Edge117: Edge11[Int, String] = Edge11(6, 7, "A")
    val Edge118: Edge11[Int, String] = Edge11(7, 5, "A")

    val Edge11s = allEdge11s.toSeq ++ (Set(Edge115, Edge116, Edge117, Edge118)).toList
    val g = new TarjanGraph[Int, String](Edge11s)
    assert(g.hasCycle)
    assert(g.tarjanCycle.size == 2)
    val sort = g.topologicalSort
    sort match {
      case CyclicDependencyResult(cycle) => println(cycle); assert(true)
      case AcyclicDependencyResult(totalOrder) => assert(false)
    }
  }

  "Tarjan Graph with three loops" should "report three algebraic loops" in {
    val Edge115: Edge11[Int, String] = Edge11(4, 5, "A")
    val Edge116: Edge11[Int, String] = Edge11(5, 6, "A")
    val Edge117: Edge11[Int, String] = Edge11(6, 7, "A")
    val Edge118: Edge11[Int, String] = Edge11(6, 5, "A")
    val Edge119: Edge11[Int, String] = Edge11(8, 7, "A")
    val Edge1110: Edge11[Int, String] = Edge11(7, 8, "A")

    val Edge11s = allEdge11s.toSeq ++ (Set(Edge115, Edge116, Edge117, Edge118, Edge119, Edge1110)).toList
    val g = new TarjanGraph[Int, String](Edge11s)
    assert(g.hasCycle)
    assert(g.tarjanCycle.size == 3)
    val sort = g.topologicalSort
    sort match {
      case CyclicDependencyResult(cycle) => println(cycle); assert(true)
      case AcyclicDependencyResult(totalOrder) => assert(false)
    }
  }

  "Tarjan BigGrahp 1000+ Edge11s" should "report AN algebraic loop" in {
    val alotOfEdge11s = generateEdge11s(1000).:+(Edge11(1000, 1, "A"))
    val g = new TarjanGraph[Int, String](alotOfEdge11s)
    assert(g.hasCycle)
    val sort = g.topologicalSort
    sort match {
      case CyclicDependencyResult(cycle) => println(cycle); assert(true)
      case AcyclicDependencyResult(totalOrder) => assert(false)
    }
  }

  "Tarjan" should "report NO algebraic loop" in {
    val tests: Iterator[Set[Edge11[Int, String]]] = allEdge11s.toList.combinations(3).map(_.toSet)
    assert(tests.forall((f: Set[Edge11[Int, String]]) => {
      val g = new TarjanGraph[Int, String](f)
      val actual = g.hasCycle
      !actual
    }))
  }

  "Tarjan BigGrahp 1000+ Edge11s" should "report NO algebraic loop" in {
    val alotOfEdge11s = generateEdge11s(1001)
    val g = new TarjanGraph[Int, String](alotOfEdge11s)
    assert(!g.hasCycle)
  }

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) + "ns")
    result
  }

  "Tarjan BigGrahp 100000+ Edge11s" should "report NO algebraic loop" ignore  {
    val alotOfEdge11s = generateEdge11s(100000)
    time {
      val g = new TarjanGraph[Int, String](alotOfEdge11s)
      assert(!g.hasCycle)
    }
  }

  "Tarjan complicated example" should "report NO algebraic loop" in {
    val A1: Edge11[Char, String] = Edge11('A', 'B', "A")
    val A2: Edge11[Char, String] = Edge11('A', 'D', "A")
    val B: Edge11[Char, String] = Edge11('B', 'E', "A")
    val C: Edge11[Char, String] = Edge11('C', 'F', "A")
    val D1: Edge11[Char, String] = Edge11('D', 'E', "A")
    val D2: Edge11[Char, String] = Edge11('D', 'F', "A")
    val E1: Edge11[Char, String] = Edge11('E', 'H', "A")
    val E2: Edge11[Char, String] = Edge11('E', 'G', "A")
    val F1: Edge11[Char, String] = Edge11('F', 'G', "A")
    val F2: Edge11[Char, String] = Edge11('F', 'I', "A")
    val G1: Edge11[Char, String] = Edge11('G', 'J', "A")
    val G2: Edge11[Char, String] = Edge11('G', 'I', "A")
    val H: Edge11[Char, String] = Edge11('H', 'J', "A")

    val graphEdge11s = Set(A1, A2, B, C, D1, D2, E1, E2, F1, F2, G1, G2, H)
    val g = new TarjanGraph[Char, String](graphEdge11s)
    assert(!g.hasCycle)
    println(g.topologicalSortedEdges)
    val expectedOrderingEdge11s = List(A1, A2, D1, D2, C, F1, F2, B, E1, E2, H, G1, G2)
    // This exact assert may fail since, there is some non-determinism in finding the topological order when converting from an unordered collection (Set)
    // But the result should still be a valid topological order since a topological order is not unique for a DAG
    //assert(g.topologicalSortedEdge11s == expectedOrderingEdge11s)
  }
}
