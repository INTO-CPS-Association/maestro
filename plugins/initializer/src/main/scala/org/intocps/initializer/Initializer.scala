import java.io.{File, InputStream}
import java.util

import org.intocps.initializer.FMIASTFactory
import org.intocps.maestro.ast.{AFunctionDeclaration, PExp, PStm}
import org.intocps.maestro.plugin.{IMaestroPlugin, IPluginConfiguration}
import org.intocps.multimodelparser.data._
import org.intocps.multimodelparser.parser.{ConfigurationHandler, RichMultiModelConfiguration}
import org.intocps.topologicalsorting.{Edge, TarjanGraph}

import scala.jdk.CollectionConverters._

class Initializer extends IMaestroPlugin {

  override def getName: String = "Initializer"

  override def getVersion: String = "0.0.1"

  // TODO: Create initialize token

  override def getDeclaredUnfoldFunctions: util.Set[AFunctionDeclaration] = Set(FMIASTFactory.functionDeclaration("initialize")).asJava

  override def unfold(declaredFunction: AFunctionDeclaration, formalArguments: util.List[PExp], ctxt: IPluginConfiguration): PStm = {
    calculateInitialize(new File("FIXME")) match {
      case Left(value)  => throw new Exception("Could not create initialize: " + value)
      case Right(value) => value
    }
  }

  def calculateInitialize(f: File): Either[String, PStm] = {
    val rmmcE: Either[String, RichMultiModelConfiguration] = ConfigurationHandler.loadMMCFromFile(f)

    val topSortResult = rmmcE.flatMap(data => {
      // All connections
      val allConnections: Set[Connection] = data.externalConnections.union(
        Connections.FMUInternalConnectionsToConnections(data.fmus));
      val edgeConvertedConnections = allConnections.map(x => Edge(x.from, x.to))
      // Perform topological sorting
      val tg = new TarjanGraph[ConnectionScalarVariable](edgeConvertedConnections);
      tg.topologicalSort match {
        case IODependencyCyclic(cycle)       => Left(cycle)
        case IODependencyAcyclic(totalOrder) => Right(totalOrder)
      }
    })

    println(topSortResult match {
      case Left(cycle: String)                              => "Topological sorting failed with the cycle: " + cycle;
      case Right(totalOrder: Seq[ConnectionScalarVariable]) => "Topological sorting succeeded with the order: " +
        totalOrder.map(x => x.toString).mkString(" -> ");
    })

    val program: Either[String, PStm] = for {
      rmmc <- rmmcE
      topSorted <- topSortResult
    } yield MaBLSpec.createMaBLSpec(rmmc.fmus, topSorted, rmmc.multiModelConfiguration, rmmc.externalConnections)
    program
  }

  // Extracts instances from connections
  def computeInstances(connections: Set[Connection]): Set[Instance] = {
    connections.flatMap { conn =>
      val fromInstance: Instance = conn.from.vInstance
      val toInstances: Set[Instance] = conn.to.map(_.vInstance)
      toInstances.+(fromInstance)
    }
  }

  //  def main(args: Array[String]) : Unit =
  //    {
  //      // Load the multi model configuration file
  //      val string = args[0]
  //      val f = new File(string).getAbsolutePath
  ////
  ////      ConfigurationHandler.loadMMCFromFile(Source.fromFile(args[0]))
  //      // Parse it
  //      // Get the graph
  //      // Create the code
  //    }


  override def requireConfig(): Boolean = false

  override def parseConfig(is: InputStream): IPluginConfiguration = ???
}
