package org.intocps.multimodelparser.parser

import java.io.File

import argonaut.Parse
import org.intocps.fmi.jnifmuapi.Factory
import org.intocps.multimodelparser.data.{Connection, FMUWithMD, Instance}
import org.intocps.orchestration.coe.modeldefinition.ModelDescription

import scala.io.BufferedSource

object ConfigurationHandler {

  def loadMMCFromFile(file: File): Either[String, RichMultiModelConfiguration] = {
    val source: BufferedSource = scala.io.Source.fromFile(file)
    loadMMCFromSource(source)
  }

  def loadMMCFromSource(mmcSource: BufferedSource): Either[String, RichMultiModelConfiguration] = {
    val lines = try mmcSource.mkString finally mmcSource.close()
    val mmc: Either[String, MultiModelConfiguration] = Parse.decodeEither[MultiModelConfiguration](lines)
    mmc.map { mmc_ =>
      val externalConnections: Set[Connection] = Conversions.MMCConnectionsToMaestroConnections(mmc_.connections)

      // Extract instances from connections and associate these with their respective FMU
      val instances: Set[Instance] = computeInstances(externalConnections)

      // Unpack FMUs and calculate internal dependencies
      val fmus: Map[FMUWithMD, Set[Instance]] = mmc_.fmus.foldRight(Map.empty[FMUWithMD, Set[Instance]]) { case ((key, path), acc) =>
        val md = new ModelDescription(Factory.create(new File(path)).getModelDescription)
        val fmu = FMUWithMD(key, md, Connections.calculateInternalConnections(md))
        val is = instances.collect { case i if i.fmu == fmu.key => i }
        acc.+(fmu -> is)
      }
      RichMultiModelConfiguration(fmus, externalConnections, mmc_);
    }
  }

  // Extracts instances from connections
  def computeInstances(connections: Set[Connection]): Set[Instance] = {
    connections.flatMap { conn =>
      val fromInstance: Instance = conn.from.vInstance
      val toInstances: Set[Instance] = conn.to.map(_.vInstance)
      toInstances.+(fromInstance)
    }
  }

}
