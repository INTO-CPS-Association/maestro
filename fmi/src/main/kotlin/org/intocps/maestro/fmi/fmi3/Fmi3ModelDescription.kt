package org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3

import org.apache.commons.io.IOUtils
import org.intocps.maestro.fmi.BaseModelDescription
import org.intocps.maestro.fmi.ModelDescription
import org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3.Fmi3TypeDefinition.Fmi3TypeIdentifiers
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
    private var variables: Collection<Fmi3Variable>? = null
    private var typeDefinitions: Collection<Fmi3TypeDefinition>? = null

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

    // Attributes common between the interfaces for CoSimulation, ModelExchange and ScheduledExecution
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

    // Unit definitions attribute
    @Throws(XPathExpressionException::class)
    fun getUnitDefinitions(): Collection<Fmi3Unit> {
        return NodeIterator(lookup(doc, xpath, "fmiModelDescription/UnitDefinitions/@Unit")).map { unitNode ->
            Fmi3Unit.Builder().apply {
                setName(unitNode.attributes.getNamedItem("name").nodeValue)
                val displayUnits = mutableListOf<Fmi3Unit.Fmi3DisplayUnit>()
                NodeIterator(unitNode.childNodes).forEach { childNode ->
                    when (childNode.localName) {
                        "BaseUnit" -> {
                            setBaseUnit(parseBaseUnit(childNode))
                        }
                        "DisplayUnit" -> {
                            displayUnits.add(parseDisplayUnit(childNode))
                        }
                        "Annotations" -> {
                        }
                    }
                }
                setDisplayUnits(displayUnits)
            }.build()
        }
    }

    // Type definitions attribute
    @Throws(XPathExpressionException::class)
    fun getTypeDefinitions(): Collection<Fmi3TypeDefinition> {
        return typeDefinitions ?: lookup(doc, xpath, "fmiModelDescription/@TypeDefinitions").let { typeDefNodes ->
            val typeDefinitions = mutableListOf<Fmi3TypeDefinition>()
            for (i in 0..typeDefNodes.length) {
                typeDefNodes.item(i).apply { typeDefinitions.add(parseTypeDefinition(this)) }
            }
            this.typeDefinitions = typeDefinitions
            typeDefinitions
        }
    }

    // Model variables attribute
    @Throws(XPathExpressionException::class)
    fun getModelVariables(): Collection<Fmi3Variable> {
        return variables ?: lookup(doc, xpath, "fmiModelDescription/@ModelVariables").let { modelVariablesNode ->
            val modelVariables = mutableListOf<Fmi3Variable>()
            for (i in 0..modelVariablesNode.length) {
                modelVariablesNode.item(i).apply { modelVariables.add(parseModelVariable(this)) }
            }
            //TODO: if(!validateVariabilityCausality(modelVariables))
            variables = modelVariables
            modelVariables
        }
    }

    // Model structure attribute
    @Throws(XPathExpressionException::class)
    fun getModelStructure(): Collection<Fmi3Variable> {
        return lookup(doc, xpath, "fmiModelDescription/@ModelStructure").let { modelVariablesNode ->
            val modelVariables = mutableListOf<Fmi3Variable>()
            for (i in 0..modelVariablesNode.length) {
                modelVariablesNode.item(i).apply { modelVariables.add(parseModelVariable(this)) }
            }
            modelVariables
        }
    }

    @Synchronized
    @Throws(
        XPathExpressionException::class,
        InvocationTargetException::class,
        IllegalAccessException::class
    )
    override fun parse() {
    }

    //TODO: Validate that variable declaration conforms to the spec?
    // i.e: valid combination of variability and causality, each variable has a unique name etc.?
    private fun validateSpecConformity(variables: Collection<Fmi3Variable>): Boolean {

        return true
    }

    //** Parsing logic **//
    private fun parseModelVariable(node: Node): Fmi3Variable {
        try {
            Fmi3TypeIdentifiers.fromVariableTypeAsString(node.nodeName).let {
                return when (it) {
                    Fmi3TypeIdentifiers.Float32Type -> parseFloatVariable(node, Fmi3TypeIdentifiers.Float32Type)
                    Fmi3TypeIdentifiers.Float64Type -> parseFloatVariable(node, Fmi3TypeIdentifiers.Float64Type)
                    Fmi3TypeIdentifiers.Int8Type -> parseIntVariable(node, Fmi3TypeIdentifiers.Int8Type)
                    Fmi3TypeIdentifiers.UInt8Type -> parseIntVariable(node, Fmi3TypeIdentifiers.UInt8Type)
                    Fmi3TypeIdentifiers.Int16Type -> parseIntVariable(node, Fmi3TypeIdentifiers.Int16Type)
                    Fmi3TypeIdentifiers.UInt16Type -> parseIntVariable(node, Fmi3TypeIdentifiers.UInt16Type)
                    Fmi3TypeIdentifiers.Int32Type -> parseIntVariable(node, Fmi3TypeIdentifiers.Int32Type)
                    Fmi3TypeIdentifiers.UInt32Type -> parseIntVariable(node, Fmi3TypeIdentifiers.UInt32Type)
                    Fmi3TypeIdentifiers.Int64Type -> parseIntVariable(node, Fmi3TypeIdentifiers.Int64Type)
                    Fmi3TypeIdentifiers.UInt64Type -> parseIntVariable(node, Fmi3TypeIdentifiers.UInt64Type)
                    Fmi3TypeIdentifiers.BooleanType -> parseBooleanVariable(node)
                    Fmi3TypeIdentifiers.StringType -> parseStringVariable(node)
                    Fmi3TypeIdentifiers.BinaryType -> parseBinaryVariable(node)
                    Fmi3TypeIdentifiers.EnumerationType -> parseEnumerationVariable(node)
                    Fmi3TypeIdentifiers.ClockType -> parseClockVariable(node)
                }
            }
        } catch (e: Exception) {
            throw Exception("Cannot parse model variable ${node.nodeName}: $e")
        }
    }

    private fun parseFloatVariable(node: Node, typeIdentifier: Fmi3TypeIdentifiers): FloatVariable {
        try {
            val initial = (node.attributes.getNamedItem("initial")?.nodeValue ?: "").let {
                if (it.isEmpty()) null else valueOf<Initial>(it)
            }
            val causality = (node.attributes.getNamedItem("causality")?.nodeValue ?: "").let {
                if (it.isEmpty()) Fmi3Causality.Local else valueOf(it) // Default causality is local
            }
            val variability = (node.attributes.getNamedItem("variability")?.nodeValue ?: "").let {
                if (it.isEmpty()) Variability.Continuous else valueOf(it) // Default variability for float is continuous
            }
            val declaredType = node.attributes.getNamedItem("declaredType")?.nodeValue ?: ""
            val typeDefinition = getTypeDefinitions().find { typeDef ->
                declaredType.isNotEmpty() && typeDef.type.equals(
                    valueOf<Fmi3TypeIdentifiers>(declaredType)
                )
            }
            val quantity = node.attributes.getNamedItem("quantity")?.nodeValue
                ?: (typeDefinition?.type as Fmi3TypeDefinition.FloatType).quantity
            return FloatVariable(
                    node.attributes.getNamedItem("name").nodeValue,
                    node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                    node.attributes.getNamedItem("description")?.nodeValue,
                    causality,
                    variability,
                    node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                    node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean(),
                    node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                    node.attributes.getNamedItem("clocks")?.nodeValue, //TODO: Implement parsing of clocks
                    typeIdentifier,
                    declaredType,
                    initial,
                    node.attributes.getNamedItem("quantity")?.nodeValue,
                    node.attributes.getNamedItem("unit")?.nodeValue,
                    node.attributes.getNamedItem("displayUnit")?.nodeValue,
                    node.attributes.getNamedItem("relativeQuantity")?.nodeValue?.toBoolean(),
                    node.attributes.getNamedItem("unbounded")?.nodeValue?.toBoolean(),
                    node.attributes.getNamedItem("min")?.nodeValue?.toDouble(),
                    node.attributes.getNamedItem("max")?.nodeValue?.toDouble(),
                    node.attributes.getNamedItem("nominal")?.nodeValue?.toDouble(),
                    node.attributes.getNamedItem("start")?.nodeValue, //TODO: Implement parsing of start
                    node.attributes.getNamedItem("derivative")?.nodeValue?.toUInt(),
                    node.attributes.getNamedItem("reinit")?.nodeValue?.toBoolean()
                )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to Fmi3FloatVariable: $e")
        }
    }

    private fun parseIntVariable(node: Node, typeIdentifier: Fmi3TypeIdentifiers): IntVariable {
        try {
            val causality = node.attributes.getNamedItem("causality")?.nodeValue ?: ""
            val variability = node.attributes.getNamedItem("variability")?.nodeValue ?: ""
            return IntVariable(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("description")?.nodeValue,
                if (causality.isEmpty()) Fmi3Causality.Local else valueOf(causality), // Default causality is local
                if (variability.isEmpty()) Variability.Discrete else valueOf(variability), //Default variability for float is discrete
                node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean() ?: false,
                node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("clocks")?.nodeValue, //TODO: Implement parsing of clocks
                typeIdentifier,
                node.attributes.getNamedItem("declaredType")?.nodeValue,
                valueOf<Initial>(node.attributes.getNamedItem("initial")?.nodeValue ?: ""),
                node.attributes.getNamedItem("quantity")?.nodeValue,
                node.attributes.getNamedItem("min")?.nodeValue?.toDouble(),
                node.attributes.getNamedItem("max")?.nodeValue?.toDouble(),
                node.attributes.getNamedItem("start")?.nodeValue //TODO: Implement parsing of start
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to Fmi3IntVariable: $e")
        }
    }

    private fun parseBooleanVariable(node: Node): BooleanVariable {
        try {
            val causality = node.attributes.getNamedItem("causality")?.nodeValue ?: ""
            val variability = node.attributes.getNamedItem("variability")?.nodeValue ?: ""
            return BooleanVariable(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("description")?.nodeValue,
                if (causality.isEmpty()) Fmi3Causality.Local else valueOf(causality), // Default causality is local
                if (variability.isEmpty()) Variability.Discrete else valueOf(variability), //Default variability for float is discrete
                node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean() ?: false,
                node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("clocks")?.nodeValue, //TODO: Implement parsing of clocks
                Fmi3TypeIdentifiers.BooleanType,
                node.attributes.getNamedItem("declaredType")?.nodeValue,
                valueOf<Initial>(node.attributes.getNamedItem("initial")?.nodeValue ?: ""),
                node.attributes.getNamedItem("start")?.nodeValue //TODO: Implement parsing of start
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to Fmi3BoolVariable: $e")
        }
    }

    private fun parseStringVariable(node: Node): StringVariable {
        try {
            val causality = node.attributes.getNamedItem("causality")?.nodeValue ?: ""
            val variability = node.attributes.getNamedItem("variability")?.nodeValue ?: ""
            return StringVariable(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("description")?.nodeValue,
                if (causality.isEmpty()) Fmi3Causality.Local else valueOf(causality), // Default causality is local
                if (variability.isEmpty()) Variability.Discrete else valueOf(variability), //Default variability for float is discrete
                node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean() ?: false,
                node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("clocks")?.nodeValue, //TODO: Implement parsing of clocks
                Fmi3TypeIdentifiers.StringType
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to Fmi3StringVariable: $e")
        }
    }

    private fun parseBinaryVariable(node: Node): BinaryVariable {
        try {
            val causality = node.attributes.getNamedItem("causality")?.nodeValue ?: ""
            val variability = node.attributes.getNamedItem("variability")?.nodeValue ?: ""
            return BinaryVariable(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("description")?.nodeValue,
                if (causality.isEmpty()) Fmi3Causality.Local else valueOf(causality), // Default causality is local
                if (variability.isEmpty()) Variability.Discrete else valueOf(variability), //Default variability for float is discrete
                node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean() ?: false,
                node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("clocks")?.nodeValue, //TODO: Implement parsing of clocks
                Fmi3TypeIdentifiers.BinaryType,
                node.attributes.getNamedItem("declaredType")?.nodeValue,
                valueOf<Initial>(node.attributes.getNamedItem("initial")?.nodeValue ?: ""),
                node.attributes.getNamedItem("mimeType")?.nodeValue,
                node.attributes.getNamedItem("maxSize")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("start")?.nodeValue //TODO: Implement parsing of start
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to Fmi3BinaryVariable: $e")
        }
    }

    private fun parseEnumerationVariable(node: Node): EnumerationVariable {
        try {
            val causality = node.attributes.getNamedItem("causality")?.nodeValue ?: ""
            val variability = node.attributes.getNamedItem("variability")?.nodeValue ?: ""
            return EnumerationVariable(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("description")?.nodeValue,
                if (causality.isEmpty()) Fmi3Causality.Local else valueOf(causality), // Default causality is local
                if (variability.isEmpty()) Variability.Discrete else valueOf(variability), //Default variability for float is discrete
                node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean() ?: false,
                node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("clocks")?.nodeValue, //TODO: Implement parsing of clocks
                Fmi3TypeIdentifiers.EnumerationType,
                node.attributes.getNamedItem("declaredType")?.nodeValue,
                node.attributes.getNamedItem("quantity")?.nodeValue,
                node.attributes.getNamedItem("min")?.nodeValue?.toLong(),
                node.attributes.getNamedItem("max")?.nodeValue?.toLong(),
                node.attributes.getNamedItem("start")?.nodeValue //TODO: Implement parsing of start
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to Fmi3EnumerationVariable: $e")
        }
    }

    private fun parseClockVariable(node: Node): ClockVariable {
        try {
            val causality = node.attributes.getNamedItem("causality")?.nodeValue ?: ""
            val variability = node.attributes.getNamedItem("variability")?.nodeValue ?: ""
            return ClockVariable(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("description")?.nodeValue,
                if (causality.isEmpty()) Fmi3Causality.Local else valueOf(causality), // Default causality is local
                if (variability.isEmpty()) Variability.Discrete else valueOf(variability), //Default variability for float is discrete
                node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean() ?: false,
                node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("clocks")?.nodeValue, //TODO: Implement parsing of clocks
                Fmi3TypeIdentifiers.ClockType,
                node.attributes.getNamedItem("declaredType")?.nodeValue,
                node.attributes.getNamedItem("canBeDeactivated").nodeValue?.toBoolean(),
                node.attributes.getNamedItem("priority")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("interval")?.nodeValue, //TODO: Implement parsing of interval
                node.attributes.getNamedItem("intervalDecimal")?.nodeValue?.toFloat(),
                node.attributes.getNamedItem("shiftDecimal")?.nodeValue?.toFloat() ?: (0).toFloat(),
                node.attributes.getNamedItem("supportsFraction").nodeValue?.toBoolean() ?: false,
                node.attributes.getNamedItem("resolution")?.nodeValue?.toULong(),
                node.attributes.getNamedItem("intervalCounter")?.nodeValue?.toULong(),
                node.attributes.getNamedItem("shiftCounter")?.nodeValue?.toULong() ?: (0).toULong()
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to Fmi3ClockVariable: $e")
        }
    }

    private fun parseDisplayUnit(node: Node): Fmi3Unit.Fmi3DisplayUnit {
        val name = node.attributes.getNamedItem("name").nodeValue
        val inverse = node.attributes.getNamedItem("inverse")?.nodeValue?.toBoolean()
        val factor = node.attributes.getNamedItem("factor")?.nodeValue?.toDouble()
        val offset = node.attributes.getNamedItem("offset")?.nodeValue?.toDouble()

        return Fmi3Unit.Fmi3DisplayUnit(inverse, name, factor, offset)
    }

    private fun parseTypeDefinition(node: Node): Fmi3TypeDefinition {
        return when (node.nodeName) {
            Fmi3TypeIdentifiers.Float32Type.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.Float32Type,
                parseFloatType(node)
            )
            Fmi3TypeIdentifiers.Float64Type.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.Float64Type,
                parseFloatType(node)
            )
            Fmi3TypeIdentifiers.Int8Type.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.Int8Type,
                parseIntType(node)
            )
            Fmi3TypeIdentifiers.UInt8Type.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.UInt8Type,
                parseIntType(node)
            )
            Fmi3TypeIdentifiers.Int16Type.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.Int16Type,
                parseIntType(node)
            )
            Fmi3TypeIdentifiers.UInt16Type.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.UInt16Type,
                parseIntType(node)
            )
            Fmi3TypeIdentifiers.Int32Type.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.Int32Type,
                parseIntType(node)
            )
            Fmi3TypeIdentifiers.UInt32Type.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.UInt32Type,
                parseIntType(node)
            )
            Fmi3TypeIdentifiers.Int64Type.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.Int64Type,
                parseIntType(node)
            )
            Fmi3TypeIdentifiers.UInt64Type.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.UInt64Type,
                parseIntType(node)
            )
            Fmi3TypeIdentifiers.BooleanType.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.BooleanType,
                parseBooleanType(node)
            )
            Fmi3TypeIdentifiers.StringType.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.StringType,
                parseStringType(node)
            )
            Fmi3TypeIdentifiers.BinaryType.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.BinaryType,
                parseBinaryType(node)
            )
            Fmi3TypeIdentifiers.EnumerationType.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.EnumerationType,
                parseEnumerationType(node)
            )
            Fmi3TypeIdentifiers.ClockType.toString() -> Fmi3TypeDefinition(
                Fmi3TypeIdentifiers.ClockType,
                parseClockType(node)
            )
            else -> throw Exception("Unknown type definition during model description parsing: ${node.nodeName}")
        }
    }

    private fun parseClockType(node: Node): Fmi3TypeDefinition.ClockType {
        val clockType: Fmi3TypeDefinition.ClockType
        try {
            clockType = Fmi3TypeDefinition.ClockType(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue,
                node.attributes.getNamedItem("canBeDeactivated")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("priority")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("interval")?.nodeValue,
                node.attributes.getNamedItem("intervalDecimal")?.nodeValue?.toFloat(),
                node.attributes.getNamedItem("shiftDecimal")?.nodeValue?.toFloat(),
                node.attributes.getNamedItem("supportsFraction")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("resolution")?.nodeValue?.toULong(),
                node.attributes.getNamedItem("intervalCounter")?.nodeValue?.toULong(),
                node.attributes.getNamedItem("shiftCounter")?.nodeValue?.toULong()
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to Fmi3ClockType: $e")
        }
        return clockType
    }

    private fun parseEnumerationType(node: Node): Fmi3TypeDefinition.EnumerationType {
        val enumerationType: Fmi3TypeDefinition.EnumerationType
        try {
            enumerationType = Fmi3TypeDefinition.EnumerationType(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue,
                node.attributes.getNamedItem("quantity")?.nodeValue
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to Fmi3EnumerationType: $e")
        }
        return enumerationType
    }

    private fun parseBinaryType(node: Node): Fmi3TypeDefinition.BinaryType {
        val binaryType: Fmi3TypeDefinition.BinaryType
        try {
            binaryType = Fmi3TypeDefinition.BinaryType(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue,
                node.attributes.getNamedItem("mimeType")?.nodeValue ?: "application/octet-stream",
                node.attributes.getNamedItem("maxSize")?.nodeValue?.toUInt()
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to Fmi3BinaryType: $e")
        }
        return binaryType
    }

    private fun parseStringType(node: Node): Fmi3TypeDefinition.StringType {
        val stringType: Fmi3TypeDefinition.StringType
        try {
            stringType = Fmi3TypeDefinition.StringType(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to Fmi3StringType: $e")
        }
        return stringType
    }

    private fun parseBooleanType(node: Node): Fmi3TypeDefinition.BooleanType {
        val booleanType: Fmi3TypeDefinition.BooleanType
        try {
            booleanType = Fmi3TypeDefinition.BooleanType(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to: $e")
        }
        return booleanType
    }

    private fun parseIntType(node: Node): Fmi3TypeDefinition.IntType {
        val intType: Fmi3TypeDefinition.IntType
        try {
            intType = Fmi3TypeDefinition.IntType(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue,
                node.attributes.getNamedItem("quantity")?.nodeValue,
                node.attributes.getNamedItem("min")?.nodeValue?.toDouble(),
                node.attributes.getNamedItem("max")?.nodeValue?.toDouble()
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to Fmi3IntType: $e")
        }
        return intType
    }

    private fun parseFloatType(node: Node): Fmi3TypeDefinition.FloatType {
        val floatType: Fmi3TypeDefinition.FloatType
        try {
            floatType = Fmi3TypeDefinition.FloatType(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue,
                node.attributes.getNamedItem("quantity")?.nodeValue,
                node.attributes.getNamedItem("unit")?.nodeValue,
                node.attributes.getNamedItem("displayUnit")?.nodeValue,
                node.attributes.getNamedItem("relativeQuantity")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("unbounded")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("min")?.nodeValue?.toDouble(),
                node.attributes.getNamedItem("max")?.nodeValue?.toDouble(),
                node.attributes.getNamedItem("nominal")?.nodeValue?.toDouble()
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to Fmi3FloatType: $e")
        }
        return floatType
    }
}