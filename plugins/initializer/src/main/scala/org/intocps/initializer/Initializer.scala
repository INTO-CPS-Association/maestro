import java.io.{File, InputStream}
import java.util

import argonaut.Parse
import org.intocps.initializer.{FMIASTFactory, InitializerContext}
import org.intocps.maestro.ast.{AFunctionDeclaration, PExp, PStm}
import org.intocps.maestro.plugin.{IMaestroPlugin, IPluginConfiguration}
import org.intocps.multimodelparser.data._
import org.intocps.multimodelparser.parser.{ConfigurationHandler, RichMultiModelConfiguration}
import org.intocps.topologicalsorting.{Edge, TarjanGraph}

import scala.jdk.CollectionConverters._

object Initializer {
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

//    println(topSortResult match {
//      case Left(cycle: String)                              => "Topological sorting failed with the cycle: " + cycle;
//      case Right(totalOrder: Seq[ConnectionScalarVariable]) => "Topological sorting succeeded with the order: " +
//        totalOrder.map(x => x.toString).mkString(" -> ");
//    })

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
  private val uniqueInitializeFunction = FMIASTFactory.functionDeclaration("initialize")
  private val uniqueFunctionDeclaration: util.Set[AFunctionDeclaration] = Set(uniqueInitializeFunction).asJava

}

class Initializer extends IMaestroPlugin {

  override def getName: String = "Initializer"

  override def getVersion: String = "0.0.1"

  // TODO: Create initialize token

  override def getDeclaredUnfoldFunctions: util.Set[AFunctionDeclaration] = Initializer.uniqueFunctionDeclaration

  override def unfold(declaredFunction: AFunctionDeclaration, formalArguments: util.List[PExp], ctxt: IPluginConfiguration): PStm = {
    val localFunctionDeclaration = Initializer.uniqueInitializeFunction
    assert(declaredFunction.eq(localFunctionDeclaration), "The declared function passed to unfolding does not match the declared function of the plugin.")

    val context : InitializerContext = ctxt.asInstanceOf[InitializerContext]

    Initializer.calculateInitialize(new File(context.getContext)) match {
      case Left(value)  => throw new Exception("Could not create initialize: " + value)
      case Right(value) => value
    }
  }

  override def requireConfig(): Boolean = true

  override def parseConfig(is: InputStream): IPluginConfiguration = {
    // The input stream is expected to contain a link to the multimodel file
    val parsed = Parse.decodeEither[InitializerContext](scala.io.Source.fromInputStream(is).mkString)
    parsed match {
      case Left(value) => throw new Exception("Failed to parse plugin data: " + value)
      case Right(value) => value
    }
}}
