package org.intocps.initializer

import java.net.URI

import argonaut.DecodeJson
import org.intocps.maestro.plugin.IContext

object InitializerContext{
  implicit val dec: DecodeJson[InitializerContext] = DecodeJson(r => for {
    multiModelFilePath <- (r --\ "multimodelfilepath").as[String]
  } yield new InitializerContext(new URI(multiModelFilePath)))
}

class InitializerContext(initializerContext: URI) extends IContext {
  def getContext : URI = initializerContext
}