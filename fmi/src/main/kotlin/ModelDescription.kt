package org.intocps.maestro.fmi

import org.intocps.fmi.jnifmuapi.xml.SchemaProvider
import org.intocps.fmi.jnifmuapi.xml.SchemaResourceResolver
import org.intocps.maestro.fmi.xml.NamedNodeMapIterator
import org.intocps.maestro.fmi.xml.NodeIterator
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException
import java.io.IOException
import java.io.InputStream
import java.util.*
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

abstract class ModelDescription
@Throws(
    SAXException::class,
    IOException::class,
    ParserConfigurationException::class
) constructor(xmlInputStream: InputStream, schemaModelDescription: Source, provider: SchemaProvider) {
    private val DEBUG = false

    @JvmField
    protected val doc: Document

    @JvmField
    protected val xpath: XPath

    init {
        val docBuilderFactory = DocumentBuilderFactory.newInstance()
        validateAgainstXSD(StreamSource(xmlInputStream), schemaModelDescription, provider)
        xmlInputStream.reset()
        doc = docBuilderFactory.newDocumentBuilder().parse(xmlInputStream)
        val xPathfactory = XPathFactory.newInstance()
        xpath = xPathfactory.newXPath()
    }

    // Top level attributes
    @Throws(XPathExpressionException::class)
    fun getFmiVersion(): String {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@fmiVersion") ?: ""
    }

    @Throws(XPathExpressionException::class)
    fun getModelName(): String {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@modelName") ?: ""
    }

    @Throws(XPathExpressionException::class)
    fun getModelDescription(): String {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@description") ?: ""
    }

    @Throws(XPathExpressionException::class)
    fun getAuthor(): String {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@author") ?: ""
    }

    @Throws(XPathExpressionException::class)
    fun getVersion(): String {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@version") ?: ""
    }

    @Throws(XPathExpressionException::class)
    fun getCopyright(): String {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@copyright") ?: ""
    }

    @Throws(XPathExpressionException::class)
    fun getLicense(): String {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@license") ?: ""
    }

    @Throws(XPathExpressionException::class)
    fun getGenerationTool(): String {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@generationTool") ?: ""
    }

    @Throws(XPathExpressionException::class)
    fun getGenerationDateAndTime(): String {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@generationDateAndTime") ?: ""
    }

    @Throws(XPathExpressionException::class)
    fun getVariableNamingConvention(): String {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@variableNamingConvention") ?: ""
    }

    // Attributes common between the interfaces for CoSimulation, ModelExchange and ScheduledExecution (FMI3)
    @Throws(XPathExpressionException::class)
    fun getNeedsExecutionTool(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@needsExecutionTool")
        return name?.nodeValue?.toBoolean() ?: false
    }

    @Throws(XPathExpressionException::class)
    fun getCanBeInstantiatedOnlyOncePerProcess(): Boolean {
        val name = lookupSingle(
            doc,
            xpath,
            "fmiModelDescription/CoSimulation/@canBeInstantiatedOnlyOncePerProcess"
        )
        return name?.nodeValue?.toBoolean() ?: false
    }

    @Throws(XPathExpressionException::class)
    fun getCanGetAndSetFmustate(): Boolean {
        val name =
            lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@canGetAndSetFMUstate")
        return name?.nodeValue?.toBoolean() ?: false
    }

    @Throws(XPathExpressionException::class)
    fun getMaxOutputDerivativeOrder(): Int {
        val name =
            lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@maxOutputDerivativeOrder")
        return name?.nodeValue?.toInt() ?: 0
    }

    // Attributes specific to the CoSimulation element
    @Throws(XPathExpressionException::class)
    fun getCanHandleVariableCommunicationStepSize(): Boolean {
        val name = lookupSingle(
            doc, xpath, "fmiModelDescription/CoSimulation/@canHandleVariableCommunicationStepSize"
        )
        return name?.nodeValue?.toBoolean() ?: false
    }

    // Log categories attribute
    @Throws(XPathExpressionException::class)
    fun getLogCategories(): List<LogCategory> {
        val categories: MutableList<LogCategory> = Vector()
        NodeIterator(lookup(doc, xpath, "fmiModelDescription/LogCategories/Category")).forEach { node ->
            categories.add(
                LogCategory(
                    node.attributes.getNamedItem("name").nodeValue,
                    node.attributes.getNamedItem("description")?.nodeValue ?: ""
                )
            )
        }
        return categories
    }


    // Default experiment attribute
    fun getDefaultExperiment(): DefaultExperiment? {
        try {
            return lookupSingle(doc, xpath, "fmiModelDescription/DefaultExperiment").let { defaultExperimentNode ->
                if (defaultExperimentNode == null) {
                    return@let null
                }
                return@let DefaultExperiment(
                    defaultExperimentNode.attributes.getNamedItem("startTime")?.nodeValue?.toDouble(),
                    defaultExperimentNode.attributes.getNamedItem("stopTime")?.nodeValue?.toDouble(),
                    defaultExperimentNode.attributes.getNamedItem("tolerance")?.nodeValue?.toDouble(),
                    defaultExperimentNode.attributes.getNamedItem("stepSize")?.nodeValue?.toDouble()
                )
            }
        } catch (e: Exception) {
            throw Exception("Default experiment cannot be parsed during model description parsing: $e")
        }
    }

    companion object {
        @Throws(SAXException::class, IOException::class)
        fun validateAgainstXSD(document: Source, schemaSource: Source, provider: SchemaProvider) {
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).run {
                this.resourceResolver = SchemaResourceResolver(provider)
                this.newSchema(schemaSource).newValidator().validate(document)
            }
        }
    }

    @Synchronized
    @Throws(Exception::class)
    open fun parse() {
    }

    protected fun parseBaseUnit(node: Node): BaseUnit {
        val baseUnitBuilder = BaseUnit.Builder()
        val attributesMapLength = node.attributes.length
        for (i in 0 until attributesMapLength) {
            val nodeName = node.attributes.item(i).nodeName
            val nodeValue = node.attributes.item(i).nodeValue

            when (nodeName) {
                "kg" -> baseUnitBuilder.setKg(nodeValue.toInt())
                "m" -> baseUnitBuilder.setM(nodeValue.toInt())
                "s" -> baseUnitBuilder.setS(nodeValue.toInt())
                "A" -> baseUnitBuilder.setA(nodeValue.toInt())
                "K" -> baseUnitBuilder.setK(nodeValue.toInt())
                "mol" -> baseUnitBuilder.setMol(nodeValue.toInt())
                "cd" -> baseUnitBuilder.setCd(nodeValue.toInt())
                "rad" -> baseUnitBuilder.setRad(nodeValue.toInt())
                "factor" -> baseUnitBuilder.setFactor(nodeValue.toDouble())
                "offset" -> baseUnitBuilder.setOffset(nodeValue.toDouble())
                else -> throw Exception("Unknown base unit being parsed: $nodeName")
            }
        }
        return baseUnitBuilder.build()
    }

    @Throws(XPathExpressionException::class)
    protected fun lookupSingle(doc: Any, xpath: XPath, expression: String): Node? {
        return lookup(doc, xpath, expression).item(0)
    }

    @Throws(XPathExpressionException::class)
    protected fun lookupSingleNodeValue(doc: Any, xpath: XPath, expression: String): String? {
        return lookupSingle(doc, xpath, expression)?.nodeValue
    }

    @Throws(XPathExpressionException::class)
    protected fun lookup(doc: Any, xpath: XPath, expression: String): NodeList {
        val expr = xpath.compile(expression)
        if (DEBUG) {
            println("Starting from: " + formatNodeWithAtt(doc))
        }
        val list = expr.evaluate(doc, XPathConstants.NODESET) as NodeList
        if (DEBUG) {
            print("\tFound: ")
        }
        var first = true
        for (n in NodeIterator(list)) {
            if (DEBUG) {
                println((if (!first) "\t       " else "") + formatNodeWithAtt(n))
            }
            first = false
        }
        if (first) {
            if (DEBUG) {
                println("none")
            }
        }
        return list
    }

    inline fun <reified T : Enum<T>> valueOf(type: String): T {
        return java.lang.Enum.valueOf(T::class.java,
            type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
    }

    private fun formatNodeWithAtt(node: Any): String {
        if (node is Document) {
            return "Root document"
        } else if (node is Node) {
            val tmp = StringBuilder(node.localName)
            if (node.hasAttributes()) {
                for (att in NamedNodeMapIterator(node.attributes)) {
                    tmp.append(" ").append(att).append(", ")
                }
            }
            return tmp.toString()
        }
        return node.toString()
    }

    enum class Initial {
        Exact,
        Approx,
        Calculated
    }

    enum class Variability {
        Constant,
        Fixed,
        Tunable,
        Discrete,
        Continuous
    }

    data class DefaultExperiment(
        val startTime: Double?,
        val stopTime: Double?,
        val tolerance: Double?,
        val stepSize: Double?
    )

    class BaseUnit private constructor(
        val kg: Int = 0,
        val m: Int = 0,
        val s: Int = 0,
        val A: Int = 0,
        val K: Int = 0,
        val mol: Int = 0,
        val cd: Int = 0,
        val rad: Int = 0,
        val factor: Double = 1.0,
        val offset: Double = 0.0
    ) {
        data class Builder(
            var kg: Int = 0,
            var m: Int = 0,
            var s: Int = 0,
            var A: Int = 0,
            var K: Int = 0,
            var mol: Int = 0,
            var cd: Int = 0,
            var rad: Int = 0,
            var factor: Double = 1.0,
            var offset: Double = 0.0
        ) {
            fun setKg(kg: Int) = apply { this.kg = kg }
            fun setM(m: Int) = apply { this.m = m }
            fun setS(s: Int) = apply { this.s = s }
            fun setA(A: Int) = apply { this.A = A }
            fun setK(K: Int) = apply { this.K = K }
            fun setMol(mol: Int) = apply { this.mol = mol }
            fun setCd(cd: Int) = apply { this.cd = cd }
            fun setRad(rad: Int) = apply { this.rad = rad }
            fun setFactor(factor: Double) = apply { this.factor = factor }
            fun setOffset(offset: Double) = apply { this.offset = offset }
            fun build() = BaseUnit(kg, m, s, A, K, mol, cd, rad, factor, offset)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BaseUnit

            if (kg != other.kg) return false
            if (m != other.m) return false
            if (s != other.s) return false
            if (A != other.A) return false
            if (K != other.K) return false
            if (mol != other.mol) return false
            if (cd != other.cd) return false
            if (rad != other.rad) return false
            if (factor != other.factor) return false
            if (offset != other.offset) return false

            return true
        }

        fun equalsAutoConvert(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BaseUnit

            if (kg != other.kg) return false
            if (m != other.m) return false
            if (s != other.s) return false
            if (A != other.A) return false
            if (K != other.K) return false
            if (mol != other.mol) return false
            if (cd != other.cd) return false
            if (rad != other.rad) return false

            return true
        }

        override fun hashCode(): Int {
            var result = kg
            result = 31 * result + m
            result = 31 * result + s
            result = 31 * result + A
            result = 31 * result + K
            result = 31 * result + mol
            result = 31 * result + cd
            result = 31 * result + rad
            result = 31 * result + factor.hashCode()
            result = 31 * result + offset.hashCode()
            return result
        }


    }


    class LogCategory(val name: String, val description: String) {
        override fun toString(): String {
            return name
        }
    }


}