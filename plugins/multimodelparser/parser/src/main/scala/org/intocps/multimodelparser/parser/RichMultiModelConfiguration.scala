package org.intocps.multimodelparser.parser

import org.intocps.multimodelparser.data.{Connection, FMUWithMD, Instance}

case class RichMultiModelConfiguration(fmus: Map[FMUWithMD, Set[Instance]], externalConnections: Set[Connection], multiModelConfiguration: MultiModelConfiguration)
