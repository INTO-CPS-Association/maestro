package org.intocps.multimodelparser.parser

import argonaut.DecodeJson
import org.intocps.multimodelparser.data.{BooleanVal, IntegerVal, ParameterValue, RealVal, StringVal}


object ParameterValueDecDecoder
{
  implicit val parameterValueDec: DecodeJson[ParameterValue] = DecodeJson(c =>
    c.as[Int].map[ParameterValue](IntegerVal(_))
      ||| c.as[Boolean].map[ParameterValue](BooleanVal(_))
      ||| c.as[String].map[ParameterValue](StringVal(_))
      ||| c.as[Double].map[ParameterValue](RealVal(_)))
}