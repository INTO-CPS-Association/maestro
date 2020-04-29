package org.intocps.multimodelparser.parser

import java.io.File

import scala.io.BufferedSource

class MultiModelParserPlugin{
  def parseMultiModel(file: File) = ConfigurationHandler.loadMMCFromFile(file)

  def parseMultiModel(stream: BufferedSource) = ConfigurationHandler.loadMMCFromSource(stream)
}
