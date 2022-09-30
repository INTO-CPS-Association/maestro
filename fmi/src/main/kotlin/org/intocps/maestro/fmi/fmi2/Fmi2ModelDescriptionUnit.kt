package org.intocps.maestro.fmi.fmi2

import org.intocps.maestro.fmi.ModelDescription
import org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi2.Fmi2Unit
import org.intocps.maestro.fmi.xml.NodeIterator
import org.xml.sax.SAXException
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Source
import javax.xml.xpath.XPathExpressionException


abstract class Fmi2ModelDescriptionUnit : ModelDescription {
    @Throws(
        SAXException::class,
        IOException::class,
        ParserConfigurationException::class
    )
    constructor(xmlInputStream: InputStream, schemaModelDescription: Source) : super(
        xmlInputStream, schemaModelDescription
    )

    // Unit definitions attribute
    @Throws(XPathExpressionException::class)
    fun getUnitDefinitions(): Collection<Fmi2Unit> {
        return NodeIterator(lookup(doc, xpath, "fmiModelDescription/UnitDefinitions/Unit")).map { unitNode ->
            Fmi2Unit.Builder().apply {
                setName(unitNode.attributes.getNamedItem("name").nodeValue)
//                val displayUnits = mutableListOf<Fmi3Unit.Fmi3DisplayUnit>()
                NodeIterator(lookup(unitNode, xpath, "*")).forEach { childNode ->
                    when (childNode.nodeName) {
                        "BaseUnit" -> {
                            setBaseUnit(parseBaseUnit(childNode))
                        }
                        "DisplayUnit" -> {
                            // displayUnits.add(parseDisplayUnit(childNode))
                        }
                        "Annotations" -> {
                        }
                    }
                }
//                setDisplayUnits(displayUnits)
            }.build()
        }
    }

}