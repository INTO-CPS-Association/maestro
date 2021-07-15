package org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3

import org.apache.commons.io.IOUtils
import org.intocps.maestro.fmi.ModelDescription
import org.intocps.maestro.fmi.xml.NodeIterator
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import javax.xml.transform.stream.StreamSource
import javax.xml.xpath.XPathExpressionException

class Fmi3ModelDescription : ModelDescription {
    private var variables: Collection<Fmi3Variable>? = null
    private var typeDefinitions: Collection<IFmi3TypeDefinition>? = null
    private var modelStructureElements: Collection<Fmi3ModelStructureElement>? = null
    private var scalarVariables: List<Fmi3ScalarVariable>? = null
    private var outputs: List<Fmi3ScalarVariable>? = null
    private var derivatives: List<Fmi3ScalarVariable>? = null
    private var derivativeToDerivativeSource: Map<Fmi3ScalarVariable, Fmi3ScalarVariable>? = null

    constructor(file: File) : super(
        ByteArrayInputStream(IOUtils.toByteArray(FileInputStream(file))), StreamSource(
            Fmi3ModelDescription::class.java.classLoader.getResourceAsStream(
                "fmi3ModelDescription.xsd"
            )
        )
    )

    constructor(file: InputStream) : super(
        file,
        StreamSource(Fmi3ModelDescription::class.java.classLoader.getResourceAsStream("fmi3ModelDescription.xsd"))
    )

    fun getScalarVariables(): List<Fmi3ScalarVariable> {
        return scalarVariables ?: run {
            parse()
            return scalarVariables ?: listOf()
        }
    }

    fun getOutputs(): List<Fmi3ScalarVariable> {
        return outputs ?: run {
            parse()
            return outputs ?: listOf()
        }
    }

    fun getDerivatives(): List<Fmi3ScalarVariable> {
        return derivatives ?: run {
            parse()
            return derivatives ?: listOf()
        }
    }

    fun getDerivativeToDerivativeSourceMap(): Map<Fmi3ScalarVariable, Fmi3ScalarVariable> {
        return derivativeToDerivativeSource ?: run {
            parse()
            return derivativeToDerivativeSource ?: mapOf()
        }
    }

    @Synchronized
    @Throws(
        XPathExpressionException::class,
        InvocationTargetException::class,
        IllegalAccessException::class
    )


    override fun parse() {
        // Create local mutable model structure list so that elements can be removed when they have been visited
        val modelStructure: MutableList<Fmi3ModelStructureElement> = getModelStructure().toMutableList()
        // Create scalar variables from a combination of model variables and model structure
        getModelVariables().map { modelVariable ->
            Fmi3ScalarVariable(modelVariable)
        }.also { interMediateVariableList ->
            // Update each element in the list according to the model structure, i.e. outputDependencies, derivativesDependencies etc...
            interMediateVariableList.forEach { variable ->
                modelStructure.filter { modelElement ->
                    modelElement.valueReference == variable.variable.valueReference
                }.also { modelStructure.removeAll(it) }.forEach { modelElement ->
                    // As per the specification the dependencies list matches the dependenciesKind list and if dependenciesKind is present so is dependencies.
                    // Therefore they can be zipped and mapped. If dependencies is not present an empty map is created.
                    val dependencyScalarVariableToDependencyKinds: Map<Fmi3ScalarVariable, Fmi3DependencyKind>
                    try {
                        dependencyScalarVariableToDependencyKinds =
                            modelElement.dependencies?.map { valueRef -> interMediateVariableList.find { valueRef == it.variable.valueReference }!! }
                                ?.zip(modelElement.dependenciesKind ?: listOf())
                                ?.associate { pair -> pair.first to pair.second } ?: mapOf()
                    } catch (e: Exception) {
                        throw Exception("Unable to associate dependency with dependency kind: $e")
                    }


                    // Add mapped model structure element to scalar variable
                    when (modelElement.elementType) {
                        Fmi3ModelStructureElementEnum.Output -> variable.outputDependencies.putAll(
                            dependencyScalarVariableToDependencyKinds
                        )
                        Fmi3ModelStructureElementEnum.ContinuousStateDerivative -> variable.derivativesDependencies.putAll(
                            dependencyScalarVariableToDependencyKinds
                        )
                        Fmi3ModelStructureElementEnum.ClockedState -> {
                            //TODO: Implement
                        }
                        Fmi3ModelStructureElementEnum.InitialUnknown -> variable.initialUnknownsDependencies.putAll(
                            dependencyScalarVariableToDependencyKinds
                        )

                        Fmi3ModelStructureElementEnum.EventIndicator -> {
                            variable.eventIndicators.putAll(dependencyScalarVariableToDependencyKinds)
                        }
                    }
                }
            }
        }.apply {
            // Set scalar variables, outputs, derivatives and derivative map
            scalarVariables = this
            outputs = this.filter { sc -> sc.variable.causality == Fmi3Causality.Output }
            derivatives = this.filter { sc ->
                (sc.variable.typeIdentifier == Fmi3TypeEnum.Float64Type ||
                        sc.variable.typeIdentifier == Fmi3TypeEnum.Float32Type) &&
                        (sc.variable as FloatVariable).derivative != null
            }.also {
                try {
                    derivativeToDerivativeSource =
                        it.associateBy {
                            this.find { sc ->
                                sc.variable.valueReference == (it.variable as FloatVariable).derivative
                            }!!
                        }
                } catch (e: Exception) {
                    throw Exception("Unable to associate derivative with derivative source: $e")
                }
            }
        }
    }

    // Top level attributes
    @Throws(XPathExpressionException::class)
    fun getInstantiationToken(): String? {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@instantiationToken")
    }

    // Attributes common between the interfaces for CoSimulation, ModelExchange and ScheduledExecution
    @Throws(XPathExpressionException::class)
    fun getCanSerializeFmustate(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@canSerializeFMUstate")
        return name?.nodeValue?.toBoolean() ?: false
    }

    @Throws(XPathExpressionException::class)
    fun getProvidesDirectionalDerivatives(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@providesDirectionalDerivatives")
        return name?.nodeValue?.toBoolean() ?: false
    }

    @Throws(XPathExpressionException::class)
    fun getProvidesAdjointDerivatives(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@providesAdjointDerivatives")
        return name?.nodeValue?.toBoolean() ?: false
    }

    @Throws(XPathExpressionException::class)
    fun getProvidesPerElementDependencies(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@providesPerElementDependencies")
        return name?.nodeValue?.toBoolean() ?: false
    }

    @Throws(XPathExpressionException::class)
    fun getProvidesIntermediateUpdate(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@providesIntermediateUpdate")
        return name?.nodeValue?.toBoolean() ?: false
    }

    @Throws(XPathExpressionException::class)
    fun getRecommendedIntermediateInputSmoothness(): Int? {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@recommendedIntermediateInputSmoothness")
        return name?.nodeValue?.toInt()
    }

    // Specific CoSimulation attributes
    @Throws(XPathExpressionException::class)
    fun getModelIdentifier(): String {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/CoSimulation/@modelIdentifier") ?: ""
    }

    @Throws(XPathExpressionException::class)
    fun getCanReturnEarlyAfterIntermediateUpdate(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@canReturnEarlyAfterIntermediateUpdate")
        return name?.nodeValue?.toBoolean() ?: false
    }

    @Throws(XPathExpressionException::class)
    fun getFixedInternalStepSize(): Double? {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@fixedInternalStepSize")
        return name?.nodeValue?.toDouble()
    }

    @Throws(XPathExpressionException::class)
    fun getHasEventMode(): Boolean {
        val name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@hasEventMode")
        return name?.nodeValue?.toBoolean() ?: false
    }

    // Unit definitions attribute
    @Throws(XPathExpressionException::class)
    fun getUnitDefinitions(): Collection<Fmi3Unit> {
        return NodeIterator(lookup(doc, xpath, "fmiModelDescription/UnitDefinitions/Unit")).map { unitNode ->
            Fmi3Unit.Builder().apply {
                setName(unitNode.attributes.getNamedItem("name").nodeValue)
                val displayUnits = mutableListOf<Fmi3Unit.Fmi3DisplayUnit>()
                NodeIterator(lookup(unitNode, xpath, "*")).forEach { childNode ->
                    when (childNode.nodeName) {
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
    fun getTypeDefinitions(): Collection<IFmi3TypeDefinition> {
        return typeDefinitions ?: lookupSingle(doc, xpath, "fmiModelDescription/TypeDefinitions").let { typeDefNodes ->
            val typeDefinitions = mutableListOf<IFmi3TypeDefinition>()
            NodeIterator(lookup(typeDefNodes as Node, xpath, "*")).forEach {
                it.apply { typeDefinitions.add(parseTypeDefinition(this)) }
            }
            this.typeDefinitions = typeDefinitions
            return typeDefinitions
        }
    }

    // Model variables attribute
    @Throws(XPathExpressionException::class)
    fun getModelVariables(): Collection<Fmi3Variable> {
        return variables ?: lookupSingle(doc, xpath, "fmiModelDescription/ModelVariables").let { modelVariablesNode ->
            val modelVariables = mutableListOf<Fmi3Variable>()
            NodeIterator(lookup(modelVariablesNode as Node, xpath, "*")).forEach {
                it.apply { modelVariables.add(parseModelVariable(this)) }
            }
            variables = modelVariables
            return modelVariables
        }
    }

    // Model structure attribute
    @Throws(XPathExpressionException::class)
    fun getModelStructure(): Collection<Fmi3ModelStructureElement> {
        return modelStructureElements ?: lookupSingle(
            doc,
            xpath,
            "fmiModelDescription/ModelStructure"
        ).let { modelStructureNode ->
            val modelStructureElements = mutableListOf<Fmi3ModelStructureElement>()
            NodeIterator(lookup(modelStructureNode as Node, xpath, "*")).forEach {
                it.apply { modelStructureElements.add(parseModelStructureElement(this)) }
            }
            this.modelStructureElements = modelStructureElements
            return modelStructureElements
        }
    }

    //****************** Parsing logic ****************** //
    private fun parseModelStructureElement(node: Node): Fmi3ModelStructureElement {
        try {
            return Fmi3ModelStructureElement(
                valueOf(node.nodeName),
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("dependencies")?.nodeValue?.split(" ")
                    ?.map { value -> value.toUInt() },
                (node.attributes.getNamedItem("dependenciesKind")?.nodeValue ?: "").let { dependencyKinds ->
                    if (dependencyKinds.isEmpty()) null else dependencyKinds.split(" ")
                        .map { dependencyKind -> valueOf(dependencyKind) }
                }
            )
        } catch (e: Exception) {
            throw Exception("Cannot parse model structure element ${node.nodeName}: $e")
        }
    }

    private fun parseModelVariable(node: Node): Fmi3Variable {
        try {
            return Fmi3TypeEnum.fromVariableTypeAsString(node.nodeName).let {
                when (it) {
                    Fmi3TypeEnum.Float32Type -> parseFloatVariable(node, Fmi3TypeEnum.Float32Type)
                    Fmi3TypeEnum.Float64Type -> parseFloatVariable(node, Fmi3TypeEnum.Float64Type)
                    Fmi3TypeEnum.Int8Type -> parseIntVariable(node, Fmi3TypeEnum.Int8Type)
                    Fmi3TypeEnum.UInt8Type -> parseIntVariable(node, Fmi3TypeEnum.UInt8Type)
                    Fmi3TypeEnum.Int16Type -> parseIntVariable(node, Fmi3TypeEnum.Int16Type)
                    Fmi3TypeEnum.UInt16Type -> parseIntVariable(node, Fmi3TypeEnum.UInt16Type)
                    Fmi3TypeEnum.Int32Type -> parseIntVariable(node, Fmi3TypeEnum.Int32Type)
                    Fmi3TypeEnum.UInt32Type -> parseIntVariable(node, Fmi3TypeEnum.UInt32Type)
                    Fmi3TypeEnum.Int64Type -> parseInt64Variable(node, Fmi3TypeEnum.Int64Type)
                    Fmi3TypeEnum.UInt64Type -> parseInt64Variable(node, Fmi3TypeEnum.UInt64Type)
                    Fmi3TypeEnum.BooleanType -> parseBooleanVariable(node)
                    Fmi3TypeEnum.StringType -> parseStringVariable(node)
                    Fmi3TypeEnum.BinaryType -> parseBinaryVariable(node)
                    Fmi3TypeEnum.EnumerationType -> parseEnumerationVariable(node)
                    Fmi3TypeEnum.ClockType -> parseClockVariable(node)
                }
            }
        } catch (e: Exception) {
            throw Exception("Cannot parse model variable ${node.nodeName}: $e")
        }
    }

    private fun parseFloatVariable(node: Node, typeIdentifier: Fmi3TypeEnum): FloatVariable {
        try {
            // If a type is declared and it exists in the type definitions then if no value is declared for a variable
            // the value of the type definition is used if present.
            val declaredType = node.attributes.getNamedItem("declaredType")?.nodeValue
            val typeDefinition: FloatTypeDefinition? =
                getTypeDefinitionFromDeclaredType(declaredType ?: "") as FloatTypeDefinition?

            return FloatVariable(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("description")?.nodeValue,
                (node.attributes.getNamedItem("causality")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Fmi3Causality.Local else valueOf(it) // Default causality is local
                },
                (node.attributes.getNamedItem("variability")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Variability.Continuous else valueOf(it) // Default variability for float is continuous unless its clocked then its discrete!
                },
                node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("clocks")?.nodeValue?.split(" ")?.map { value -> value.toUInt() },
                typeIdentifier,
                declaredType,
                (node.attributes.getNamedItem("initial")?.nodeValue ?: "").let {
                    if (it.isEmpty()) null else valueOf<Initial>(it)
                },
                node.attributes.getNamedItem("quantity")?.nodeValue ?: typeDefinition?.quantity,
                node.attributes.getNamedItem("unit")?.nodeValue ?: typeDefinition?.unit,
                node.attributes.getNamedItem("displayUnit")?.nodeValue ?: typeDefinition?.displayUnit,
                node.attributes.getNamedItem("relativeQuantity")?.nodeValue?.toBoolean()
                    ?: typeDefinition?.relativeQuantity,
                node.attributes.getNamedItem("unbounded")?.nodeValue?.toBoolean() ?: typeDefinition?.unbounded,
                node.attributes.getNamedItem("min")?.nodeValue?.toDouble() ?: typeDefinition?.min,
                node.attributes.getNamedItem("max")?.nodeValue?.toDouble() ?: typeDefinition?.max,
                node.attributes.getNamedItem("nominal")?.nodeValue?.toDouble() ?: typeDefinition?.nominal,
                node.attributes.getNamedItem("start")?.nodeValue?.split(" ")?.map { value -> value.toDouble() },
                node.attributes.getNamedItem("derivative")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("reinit")?.nodeValue?.toBoolean(),
                getDimensionsFromVariableNode(node)
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse variable ${node.nodeName}: $e")
        }
    }

    private fun parseIntVariable(node: Node, typeIdentifier: Fmi3TypeEnum): IntVariable {
        try {
            // If a type is declared and it exists in the type definitions then if no value is declared for a variable
            // that has its value declared in the type definition, the type definition value is used.
            val declaredType = node.attributes.getNamedItem("declaredType")?.nodeValue
            val typeDefinition: IntTypeDefinition? =
                getTypeDefinitionFromDeclaredType(declaredType ?: "") as IntTypeDefinition?

            return IntVariable(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("description")?.nodeValue,
                (node.attributes.getNamedItem("causality")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Fmi3Causality.Local else valueOf(it) // Default causality is local
                },
                (node.attributes.getNamedItem("variability")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Variability.Discrete else valueOf(it) //Default variability for int is discrete
                },
                node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean() ?: false,
                node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("clocks")?.nodeValue?.split(" ")?.map { value -> value.toUInt() },
                typeIdentifier,
                declaredType,
                (node.attributes.getNamedItem("initial")?.nodeValue ?: "").let {
                    if (it.isEmpty()) null else valueOf<Initial>(it)
                },
                node.attributes.getNamedItem("quantity")?.nodeValue ?: typeDefinition?.quantity,
                node.attributes.getNamedItem("min")?.nodeValue?.toInt() ?: typeDefinition?.min,
                node.attributes.getNamedItem("max")?.nodeValue?.toInt() ?: typeDefinition?.max,
                node.attributes.getNamedItem("start")?.nodeValue?.split(" ")?.map { value -> value.toInt() },
                getDimensionsFromVariableNode(node)
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse variable ${node.nodeName}: $e")
        }
    }

    private fun parseInt64Variable(node: Node, typeIdentifier: Fmi3TypeEnum): Int64Variable {
        try {
            // If a type is declared and it exists in the type definitions then if no value is declared for a variable
            // that has its value declared in the type definition, the type definition value is used.
            val declaredType = node.attributes.getNamedItem("declaredType")?.nodeValue
            val typeDefinition: Int64TypeDefinition? =
                getTypeDefinitionFromDeclaredType(declaredType ?: "") as Int64TypeDefinition?

            return Int64Variable(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("description")?.nodeValue,
                (node.attributes.getNamedItem("causality")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Fmi3Causality.Local else valueOf(it) // Default causality is local
                },
                (node.attributes.getNamedItem("variability")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Variability.Discrete else valueOf(it) //Default variability for int is discrete
                },
                node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean() ?: false,
                node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("clocks")?.nodeValue?.split(" ")?.map { value -> value.toUInt() },
                typeIdentifier,
                declaredType,
                (node.attributes.getNamedItem("initial")?.nodeValue ?: "").let {
                    if (it.isEmpty()) null else valueOf<Initial>(it)
                },
                node.attributes.getNamedItem("quantity")?.nodeValue ?: typeDefinition?.quantity,
                node.attributes.getNamedItem("min")?.nodeValue?.toLong() ?: typeDefinition?.min,
                node.attributes.getNamedItem("max")?.nodeValue?.toLong() ?: typeDefinition?.max,
                node.attributes.getNamedItem("start")?.nodeValue?.split(" ")?.map { value -> value.toLong() },
                getDimensionsFromVariableNode(node)
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse variable ${node.nodeName} : $e")
        }
    }

    private fun parseBooleanVariable(node: Node): BooleanVariable {
        try {
            return BooleanVariable(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("description")?.nodeValue,
                (node.attributes.getNamedItem("causality")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Fmi3Causality.Local else valueOf(it) // Default causality is local
                },
                (node.attributes.getNamedItem("variability")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Variability.Discrete else valueOf(it) //Default variability for boolean is discrete
                },
                node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean() ?: false,
                node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("clocks")?.nodeValue?.split(" ")?.map { value -> value.toUInt() },
                Fmi3TypeEnum.BooleanType,
                node.attributes.getNamedItem("declaredType")?.nodeValue,
                (node.attributes.getNamedItem("initial")?.nodeValue ?: "").let {
                    if (it.isEmpty()) null else valueOf<Initial>(it)
                },
                node.attributes.getNamedItem("start")?.nodeValue?.split(" ")?.map { value -> value.toBoolean() },
                getDimensionsFromVariableNode(node)
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse variable ${node.nodeName}: $e")
        }
    }

    private fun parseStringVariable(node: Node): StringVariable {
        try {
            return StringVariable(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("description")?.nodeValue,
                (node.attributes.getNamedItem("causality")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Fmi3Causality.Local else valueOf(it) // Default causality is local
                },
                (node.attributes.getNamedItem("variability")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Variability.Discrete else valueOf(it) //Default variability for string is discrete
                },
                node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean() ?: false,
                node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("clocks")?.nodeValue?.split(" ")?.map { value -> value.toUInt() },
                Fmi3TypeEnum.StringType,
                node.attributes.let { att ->
                    val startValues: MutableList<String> = mutableListOf()
                    for (i in 0 until att.length) {
                        att.item(i).takeIf { node -> node.nodeName.equals("start") }
                            .apply { startValues.add(this?.nodeValue ?: "") }
                    }
                    return@let startValues
                },
                getDimensionsFromVariableNode(node)
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse variable ${node.nodeName}: $e")
        }
    }

    private fun parseBinaryVariable(node: Node): BinaryVariable {
        try {
            // If a type is declared and it exists in the type definitions then if no value is declared for a variable
            // that has its value declared in the type definition, the type definition value is used.
            val declaredType = node.attributes.getNamedItem("declaredType")?.nodeValue
            val typeDefinition: BinaryTypeDefinition? =
                getTypeDefinitionFromDeclaredType(declaredType ?: "") as BinaryTypeDefinition?

            return BinaryVariable(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("description")?.nodeValue,
                (node.attributes.getNamedItem("causality")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Fmi3Causality.Local else valueOf(it) // Default causality is local
                },
                (node.attributes.getNamedItem("variability")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Variability.Discrete else valueOf(it) //Default variability for binary is discrete
                },
                node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean() ?: false,
                node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("clocks")?.nodeValue?.split(" ")?.map { value -> value.toUInt() },
                Fmi3TypeEnum.BinaryType,
                declaredType,
                (node.attributes.getNamedItem("initial")?.nodeValue ?: "").let {
                    if (it.isEmpty()) null else valueOf<Initial>(it)
                },
                node.attributes.getNamedItem("mimeType")?.nodeValue ?: typeDefinition?.mimeType,
                node.attributes.getNamedItem("maxSize")?.nodeValue?.toUInt() ?: typeDefinition?.maxSize,
                node.attributes.getNamedItem("start")?.nodeValue?.split(" ")
                    ?.map { value -> value.toByteArray() },
                getDimensionsFromVariableNode(node)
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse variable ${node.nodeName}: $e")
        }
    }

    private fun parseEnumerationVariable(node: Node): EnumerationVariable {
        try {
            // If a type is declared and it exists in the type definitions then if no value is declared for a variable
            // that has its value declared in the type definition, the type definition value is used.
            val declaredType = node.attributes.getNamedItem("declaredType")?.nodeValue
            val typeDefinition: EnumerationTypeDefinition? =
                getTypeDefinitionFromDeclaredType(declaredType ?: "") as EnumerationTypeDefinition?

            return EnumerationVariable(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("description")?.nodeValue,
                (node.attributes.getNamedItem("causality")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Fmi3Causality.Local else valueOf(it) // Default causality is local
                },
                (node.attributes.getNamedItem("variability")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Variability.Discrete else valueOf(it) //Default variability for enumeration is discrete
                },
                node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean() ?: false,
                node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("clocks")?.nodeValue?.split(" ")?.map { value -> value.toUInt() },
                Fmi3TypeEnum.EnumerationType,
                declaredType,
                node.attributes.getNamedItem("quantity")?.nodeValue ?: typeDefinition?.quantity,
                node.attributes.getNamedItem("min")?.nodeValue?.toLong(),
                node.attributes.getNamedItem("max")?.nodeValue?.toLong(),
                node.attributes.getNamedItem("start")?.nodeValue?.split(" ")?.map { value -> value.toLong() },
                getDimensionsFromVariableNode(node)
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse variable ${node.nodeName}: $e")
        }
    }

    private fun parseClockVariable(node: Node): ClockVariable {
        try {
            // If a type is declared and it exists in the type definitions then if no value is declared for a variable
            // that has its value declared in the type definition, the type definition value is used.
            val declaredType = node.attributes.getNamedItem("declaredType")?.nodeValue
            val typeDefinition: ClockTypeDefinition? =
                getTypeDefinitionFromDeclaredType(declaredType ?: "") as ClockTypeDefinition?

            val interval = node.attributes.getNamedItem("interval")?.nodeValue

            return ClockVariable(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("valueReference").nodeValue.toUInt(),
                node.attributes.getNamedItem("description")?.nodeValue,
                (node.attributes.getNamedItem("causality")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Fmi3Causality.Local else valueOf(it) // Default causality is local
                },
                (node.attributes.getNamedItem("variability")?.nodeValue ?: "").let {
                    if (it.isEmpty()) Variability.Discrete else valueOf(it) //Variability for clock is always discrete
                },
                node.attributes.getNamedItem("canHandleMultipleSetPerTimeInstant")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("intermediateUpdate")?.nodeValue?.toBoolean() ?: false,
                node.attributes.getNamedItem("previous")?.nodeValue?.toUInt(),
                node.attributes.getNamedItem("clocks")?.nodeValue?.split(" ")?.map { value -> value.toUInt() },
                Fmi3TypeEnum.ClockType,
                declaredType,
                node.attributes.getNamedItem("canBeDeactivated").nodeValue?.toBoolean()
                    ?: typeDefinition?.canBeDeactivated,
                node.attributes.getNamedItem("priority")?.nodeValue?.toUInt() ?: typeDefinition?.priority,
                if (interval == null) typeDefinition!!.interval else valueOf(interval),
                node.attributes.getNamedItem("intervalDecimal")?.nodeValue?.toFloat()
                    ?: typeDefinition?.intervalDecimal,
                node.attributes.getNamedItem("shiftDecimal")?.nodeValue?.toFloat() ?: typeDefinition?.shiftDecimal
                ?: (0).toFloat(),
                node.attributes.getNamedItem("supportsFraction").nodeValue?.toBoolean()
                    ?: typeDefinition?.supportsFraction ?: false,
                node.attributes.getNamedItem("resolution")?.nodeValue?.toULong() ?: typeDefinition?.resolution,
                node.attributes.getNamedItem("intervalCounter")?.nodeValue?.toULong()
                    ?: typeDefinition?.intervalCounter,
                node.attributes.getNamedItem("shiftCounter")?.nodeValue?.toULong() ?: typeDefinition?.shiftCounter
                ?: (0).toULong(),
                getDimensionsFromVariableNode(node)
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse variable ${node.nodeName}: $e")
        }
    }

    private fun parseTypeDefinition(node: Node): IFmi3TypeDefinition {
        try {
            return when (valueOf<Fmi3TypeEnum>(node.nodeName)) {
                Fmi3TypeEnum.Float32Type -> parseFloatType(node, Fmi3TypeEnum.Float32Type)
                Fmi3TypeEnum.Float64Type -> parseFloatType(node, Fmi3TypeEnum.Float64Type)
                Fmi3TypeEnum.Int8Type -> parseIntType(node, Fmi3TypeEnum.Int8Type)
                Fmi3TypeEnum.UInt8Type -> parseIntType(node, Fmi3TypeEnum.UInt8Type)
                Fmi3TypeEnum.Int16Type -> parseIntType(node, Fmi3TypeEnum.Int16Type)
                Fmi3TypeEnum.UInt16Type -> parseIntType(node, Fmi3TypeEnum.UInt16Type)
                Fmi3TypeEnum.Int32Type -> parseIntType(node, Fmi3TypeEnum.Int32Type)
                Fmi3TypeEnum.UInt32Type -> parseIntType(node, Fmi3TypeEnum.UInt32Type)
                Fmi3TypeEnum.Int64Type -> parseInt64Type(node, Fmi3TypeEnum.Int64Type)
                Fmi3TypeEnum.UInt64Type -> parseInt64Type(node, Fmi3TypeEnum.UInt64Type)
                Fmi3TypeEnum.BooleanType -> parseBooleanType(node)
                Fmi3TypeEnum.StringType -> parseStringType(node)
                Fmi3TypeEnum.BinaryType -> parseBinaryType(node)
                Fmi3TypeEnum.EnumerationType -> parseEnumerationType(node)
                Fmi3TypeEnum.ClockType -> parseClockType(node)
            }
        } catch (e: Exception) {
            throw Exception("Unknown type definition during model description parsing: ${node.nodeName}")
        }
    }

    private fun parseClockType(node: Node): ClockTypeDefinition {
        try {
            return ClockTypeDefinition(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue,
                Fmi3TypeEnum.ClockType,
                node.attributes.getNamedItem("canBeDeactivated")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("priority")?.nodeValue?.toUInt(),
                valueOf(node.attributes.getNamedItem("interval")!!.nodeValue),
                node.attributes.getNamedItem("intervalDecimal")?.nodeValue?.toFloat(),
                node.attributes.getNamedItem("shiftDecimal")?.nodeValue?.toFloat(),
                node.attributes.getNamedItem("supportsFraction")?.nodeValue?.toBoolean(),
                node.attributes.getNamedItem("resolution")?.nodeValue?.toULong(),
                node.attributes.getNamedItem("intervalCounter")?.nodeValue?.toULong(),
                node.attributes.getNamedItem("shiftCounter")?.nodeValue?.toULong()
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName}: $e")
        }
    }

    private fun parseEnumerationType(node: Node): EnumerationTypeDefinition {
        try {
            return EnumerationTypeDefinition(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue,
                Fmi3TypeEnum.EnumerationType,
                node.attributes.getNamedItem("quantity")?.nodeValue
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName}: $e")
        }
    }

    private fun parseBinaryType(node: Node): BinaryTypeDefinition {
        try {
            return BinaryTypeDefinition(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue,
                Fmi3TypeEnum.BinaryType,
                node.attributes.getNamedItem("mimeType")?.nodeValue ?: "application/octet-stream",
                node.attributes.getNamedItem("maxSize")?.nodeValue?.toUInt()
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName}: $e")
        }
    }

    private fun parseStringType(node: Node): StringTypeDefinition {
        try {
            return StringTypeDefinition(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue,
                Fmi3TypeEnum.StringType
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName}: $e")
        }
    }

    private fun parseBooleanType(node: Node): BooleanTypeDefinition {
        try {
            return BooleanTypeDefinition(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue,
                Fmi3TypeEnum.BooleanType
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName} to: $e")
        }
    }

    private fun parseIntType(node: Node, typeIdentifier: Fmi3TypeEnum): IntTypeDefinition {
        try {
            return IntTypeDefinition(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue,
                typeIdentifier,
                node.attributes.getNamedItem("quantity")?.nodeValue,
                node.attributes.getNamedItem("min")?.nodeValue?.toInt(),
                node.attributes.getNamedItem("max")?.nodeValue?.toInt()
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName}: $e")
        }
    }

    private fun parseInt64Type(node: Node, typeIdentifier: Fmi3TypeEnum): Int64TypeDefinition {
        try {
            return Int64TypeDefinition(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue,
                typeIdentifier,
                node.attributes.getNamedItem("quantity")?.nodeValue,
                node.attributes.getNamedItem("min")?.nodeValue?.toLong(),
                node.attributes.getNamedItem("max")?.nodeValue?.toLong()
            )
        } catch (e: Exception) {
            throw Exception("Unable to parse type ${node.nodeName}: $e")
        }
    }

    private fun parseFloatType(node: Node, typeIdentifier: Fmi3TypeEnum): FloatTypeDefinition {
        try {
            return FloatTypeDefinition(
                node.attributes.getNamedItem("name").nodeValue,
                node.attributes.getNamedItem("description")?.nodeValue,
                typeIdentifier,
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
            throw Exception("Unable to parse type ${node.nodeName}: $e")
        }
    }

    private fun parseDisplayUnit(node: Node): Fmi3Unit.Fmi3DisplayUnit {
        val name = node.attributes.getNamedItem("name").nodeValue
        val inverse = node.attributes.getNamedItem("inverse")?.nodeValue?.toBoolean()
        val factor = node.attributes.getNamedItem("factor")?.nodeValue?.toDouble()
        val offset = node.attributes.getNamedItem("offset")?.nodeValue?.toDouble()
        return Fmi3Unit.Fmi3DisplayUnit(inverse, name, factor, offset)
    }

    private fun getDimensionsFromVariableNode(node: Node): List<Dimension> {
        return NodeIterator(lookup(node, xpath, "Dimension")).map { dimensionNode ->
            Dimension(
                dimensionNode.attributes.getNamedItem("valueReference")?.nodeValue?.toUInt(),
                dimensionNode.attributes.getNamedItem("start")?.nodeValue?.split(" ")
                    ?.map { value -> value.toLong() })
        }
    }

    private fun getTypeDefinitionFromDeclaredType(declaredType: String): IFmi3TypeDefinition? {
        return if (declaredType.isNotEmpty()) getTypeDefinitions().find { typeDef ->
            typeDef.name == declaredType
        } else null
    }

    data class Fmi3ScalarVariable(
        val variable: Fmi3Variable,
        val outputDependencies: MutableMap<Fmi3ScalarVariable, Fmi3DependencyKind> = mutableMapOf(),
        val derivativesDependencies: MutableMap<Fmi3ScalarVariable, Fmi3DependencyKind> = mutableMapOf(),
        val initialUnknownsDependencies: MutableMap<Fmi3ScalarVariable, Fmi3DependencyKind> = mutableMapOf(),
        val eventIndicators: MutableMap<Fmi3ScalarVariable, Fmi3DependencyKind> = mutableMapOf()
    )

}