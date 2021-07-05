package org.intocps.maestro.fmi

import org.apache.commons.io.IOUtils
import org.intocps.maestro.fmi.xml.NodeIterator
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import javax.xml.transform.stream.StreamSource
import javax.xml.xpath.XPathExpressionException

class Fmi3ModelDescription : BaseModelDescription {
    constructor(file: File) : super(
        ByteArrayInputStream(IOUtils.toByteArray(FileInputStream(file))), StreamSource(
            ModelDescription::class.java.classLoader.getResourceAsStream(
                "fmi2ModelDescription.xsd"
            )
        )
    )

    constructor(file: InputStream) : super(
        file,
        StreamSource(ModelDescription::class.java.classLoader.getResourceAsStream("fmi2ModelDescription.xsd"))
    )

    // Top level attributes
    @Throws(XPathExpressionException::class)
    fun getInstantiationToken(): String? {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@instantiationToken")
    }

    // Capabilities common between the interfaces for CoSimulation, ModelExchange and ScheduledExecution
    @Throws(XPathExpressionException::class)
    fun getCanSerializeFmustate(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@canSerializeFMUstate")
        return name.nodeValue.toBoolean()
    }

    @Throws(XPathExpressionException::class)
    fun getProvidesDirectionalDerivatives(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@providesDirectionalDerivatives")
        return name.nodeValue.toBoolean()
    }

    @Throws(XPathExpressionException::class)
    fun getProvidesAdjointDerivatives(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@providesAdjointDerivatives")
        return name.nodeValue.toBoolean()
    }

    @Throws(XPathExpressionException::class)
    fun getProvidesPerElementDependencies(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@providesPerElementDependencies")
        return name.nodeValue.toBoolean()
    }

    @Throws(XPathExpressionException::class)
    fun getProvidesIntermediateUpdate(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@providesIntermediateUpdate")
        return name.nodeValue.toBoolean()
    }

    @Throws(XPathExpressionException::class)
    fun getRecommendedIntermediateInputSmoothness(): Int {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@recommendedIntermediateInputSmoothness")
        return name.nodeValue.toInt()
    }

    // Specific CoSimulation attributes
    @Throws(XPathExpressionException::class)
    fun getModelIdentifier(): String {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/CoSimulation/@modelIdentifier")
    }

    @Throws(XPathExpressionException::class)
    fun getCanReturnEarlyAfterIntermediateUpdate(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@canReturnEarlyAfterIntermediateUpdate")
        return name.nodeValue.toBoolean()
    }

    @Throws(XPathExpressionException::class)
    fun getFixedInternalStepSize(): Double {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@fixedInternalStepSize")
        return name.nodeValue.toDouble()
    }

    @Throws(XPathExpressionException::class)
    fun getHasEventMode(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@hasEventMode")
        return name.nodeValue.toBoolean()
    }

    // Unit definitions element
    @Throws(XPathExpressionException::class)
    fun getUnitDefinitions(): List<Fmi3Unit> {
        return NodeIterator(lookup(doc, xpath, "fmiModelDescription/UnitDefinitions/Unit")).map { unitNode ->
            Fmi3Unit.Builder().apply {
                setName(unitNode.attributes.getNamedItem("name").nodeValue)
                val displayUnits = mutableListOf<Fmi3DisplayUnit>()
                NodeIterator(unitNode.childNodes).forEach { childNode ->
                    when (childNode.localName) {
                        "BaseUnit" -> {
                            setBaseUnit(mapNodeToBaseUnit(childNode))
                        }
                        "DisplayUnit" -> {
                            displayUnits.add(mapNodeToDisplayUnit(childNode))
                        }
                        "Annotations" -> {
                        }
                    }
                }
                setDisplayUnits(displayUnits)
            }.build()
        }
    }

    // Type definitions element
//    @Throws(XPathExpressionException::class)
//    fun getTypeDefinitions(): List<Fmi3TypeDefinition> {
//        lookup(doc, xpath, "fmiModelDescription/TypeDefinitions").let { typeDefNode ->
//            val numberOfTypeDefinitions = typeDefNode.length
//            val typeDefinitions = mutableListOf<Fmi3TypeDefinition>()
//            for(i in 0..numberOfTypeDefinitions){
//                val typeDefinition = Fmi3TypeDefinition
//                typeDefinitions.add()
//            }
//        }
//    }

    private fun mapNodeToDisplayUnit(node: Node): Fmi3DisplayUnit {
        val name = node.attributes.getNamedItem("name").nodeValue
        val inverse = node.attributes.getNamedItem("inverse")?.nodeValue?.toBoolean()
        val factor = node.attributes.getNamedItem("factor")?.nodeValue?.toDouble()
        val offset = node.attributes.getNamedItem("offset")?.nodeValue?.toDouble()

        return Fmi3DisplayUnit(inverse, name, factor, offset)
    }

//    fun mapNodeToTypeDefinition(node: Node): Fmi3TypeDefinition{
//
//    }

    @Synchronized
    @Throws(
        XPathExpressionException::class,
        InvocationTargetException::class,
        IllegalAccessException::class
    )
    override fun parse() {
    }

    class Fmi3TypeDefinition(val typeIdentifier: Fmi3TypeIdentifiers, val type: Fmi3Type) {

        data class Float32(
            override val name: String,
            var description: String,
            var quantity: String,
            var unit: String,
            var displayUnit: String,
            var relativeQuantity: Boolean,
            var unbounded: Boolean,
            var min: Double,
            var max: Double,
            var nominal: Double
        ): Fmi3Type

        data class Float64(
            override val name: String,
            var description: String,
            var quantity: String,
            var unit: String,
            var displayUnit: String,
            var relativeQuantity: Boolean,
            var unbounded: Boolean,
            var min: Double,
            var max: Double,
            var nominal: Double
        ): Fmi3Type

        data class Int8(
            override val name: String,
            var description: String,
            var quantity: String,
            var min: Double,
            var max: Double
        ): Fmi3Type

        data class UInt8(
            override val name: String,
            var description: String,
            var quantity: String,
            var min: Double,
            var max: Double
        ): Fmi3Type

        data class Int16(
            override val name: String,
            var description: String,
            var quantity: String,
            var min: Double,
            var max: Double
        ): Fmi3Type

        data class UInt16(
            override val name: String,
            var description: String,
            var quantity: String,
            var min: Double,
            var max: Double
        ): Fmi3Type

        data class Int32(
            override val name: String,
            var description: String,
            var quantity: String,
            var min: Double,
            var max: Double
        ): Fmi3Type

        data class UInt32(
            override val name: String,
            var description: String,
            var quantity: String,
            var min: Double,
            var max: Double
        ): Fmi3Type

        data class Int64(
            override val name: String,
            var description: String,
            var quantity: String,
            var min: Double,
            var max: Double
        ): Fmi3Type

        data class UInt64(
            override val name: String,
            var description: String,
            var quantity: String,
            var min: Double,
            var max: Double
        ): Fmi3Type

        data class Boolean(
            override val name: String,
            var description: String,
        ): Fmi3Type

        data class Binary(
            override val name: String,
            var description: String,
            var mimeType: String,
            var maxSize: Int
        ): Fmi3Type

        data class Enumeration(
            override val name: String,
            var description: String,
            var quantity: String
        ): Fmi3Type

        interface Fmi3Type{
            val name: String
        }

        enum class Fmi3TypeIdentifiers {
            Float32, Float64, Int8, UInt8, Int16, UInt16, Int32, UInt32, Int64, UInt64, Boolean, String, Binary, Enumeration, Clock
        }
    }


    class Fmi3DisplayUnit internal constructor(
        val inverse: Boolean?,
        name: String,
        factor: Double?,
        offset: Double?
    ) : Fmi2DisplayUnit(name, factor, offset)

    class Fmi3Unit internal constructor(
        val name: String,
        val baseUnit: BaseUnit?,
        val displayUnits: Collection<Fmi3DisplayUnit>?
    ) {
        data class Builder(
            var name: String = "",
            var baseUnit: BaseUnit? = null,
            var displayUnits: Collection<Fmi3DisplayUnit>? = null
        ) {
            fun setName(name: String) = apply { this.name = name }
            fun setBaseUnit(baseUnit: BaseUnit) = apply { this.baseUnit = baseUnit }
            fun setDisplayUnits(displayUnits: Collection<Fmi3DisplayUnit>) = apply { this.displayUnits = displayUnits }
            fun build() = Fmi3Unit(name, baseUnit, displayUnits)
        }
    }
}