package org.intocps.multimodelparser.parser

import argonaut.DecodeJson
import org.intocps.multimodelparser.data.ParameterValue

// Beware of this issue: https://github.com/argonaut-io/argonaut/issues/314
object MultiModelConfiguration
{
  implicit val dec: DecodeJson[MultiModelConfiguration] = DecodeJson(r => for {
    fmus <- (r --\ "fmus").as[Map[String, String]]
    connections <- (r --\ "connections").as[Map[String, List[String]]]
    parameters <- (r --\ "parameters").as[Map[String, ParameterValue]](argonaut.DecodeJson.MapDecodeJson(ParameterValueDecDecoder.parameterValueDec))
  } yield MultiModelConfiguration(fmus, connections, parameters))
}
case class MultiModelConfiguration(fmus: Map[String,String], connections: Map[String, List[String]], parameters: Map[String, ParameterValue]);
