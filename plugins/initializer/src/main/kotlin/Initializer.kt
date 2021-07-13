package org.intocps.maestro.plugin.initializer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NullNode
import org.intocps.maestro.ast.*
import org.intocps.maestro.ast.display.PrettyPrinter
import org.intocps.maestro.ast.node.AImportedModuleCompilationUnit
import org.intocps.maestro.ast.node.PExp
import org.intocps.maestro.ast.node.PStm
import org.intocps.maestro.ast.node.SBlockStm
import org.intocps.maestro.core.Framework
import org.intocps.maestro.core.messages.IErrorReporter
import org.intocps.maestro.fmi.Fmi2ModelDescription
import org.intocps.maestro.framework.core.IRelation
import org.intocps.maestro.framework.core.ISimulationEnvironment
import org.intocps.maestro.framework.core.RelationVariable
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment
import org.intocps.maestro.framework.fmi2.InvalidVariableStringException
import org.intocps.maestro.framework.fmi2.ModelConnection
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder
import org.intocps.maestro.framework.fmi2.api.mabl.*
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.values.BooleanExpressionValue
import org.intocps.maestro.framework.fmi2.api.mabl.values.DoubleExpressionValue
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue
import org.intocps.maestro.framework.fmi2.api.mabl.values.StringExpressionValue
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.IntVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api
import org.intocps.maestro.plugin.BasicMaestroExpansionPlugin
import org.intocps.maestro.plugin.ExpandException
import org.intocps.maestro.plugin.IPluginConfiguration
import org.intocps.maestro.plugin.SimulationFramework
import org.intocps.maestro.plugin.initializer.instructions.*
import org.intocps.maestro.plugin.verificationsuite.prologverifier.InitializationPrologQuery
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.util.function.Consumer
import java.util.function.Predicate

@SimulationFramework(framework = Framework.FMI2)
class Initializer : BasicMaestroExpansionPlugin {
    val f1 = MableAstFactory.newAFunctionDeclaration(
        LexIdentifier("initialize", null),
        listOf(
            MableAstFactory.newAFormalParameter(
                MableAstFactory.newAArrayType(MableAstFactory.newANameType("FMI2Component")),
                MableAstFactory.newAIdentifier("component")
            ),
            MableAstFactory.newAFormalParameter(
                MableAstFactory.newRealType(),
                MableAstFactory.newAIdentifier("startTime")
            ),
            MableAstFactory.newAFormalParameter(
                MableAstFactory.newRealType(),
                MableAstFactory.newAIdentifier("endTime")
            )
        ),
        MableAstFactory.newAVoidType()
    )

    private val portsAlreadySet = HashMap<ComponentVariableFmi2Api, Set<Fmi2ModelDescription.ScalarVariable>>()
    private val topologicalPlugin: TopologicalPlugin
    private val initializationPrologQuery: InitializationPrologQuery
    var config: InitializationConfig? = null
    var modelParameters: List<ModelParameter>? = null
    var envParameters: List<String>? = null
    var compilationUnit: AImportedModuleCompilationUnit? = null

    // Convergence related variables
    var absoluteTolerance: Fmi2Builder.DoubleVariable<PStm>? = null
    var relativeTolerance: Fmi2Builder.DoubleVariable<PStm>? = null
    var maxConvergeAttempts: Fmi2Builder.IntVariable<PStm>? = null

    constructor() {
        initializationPrologQuery = InitializationPrologQuery()
        topologicalPlugin = TopologicalPlugin()
    }

    constructor(topologicalPlugin: TopologicalPlugin, initializationPrologQuery: InitializationPrologQuery) {
        this.topologicalPlugin = topologicalPlugin
        this.initializationPrologQuery = initializationPrologQuery
    }

    override fun getName(): String {
        return Initializer::class.java.simpleName
    }

    override fun getVersion(): String {
        return "0.0.0"
    }

    val declaredUnfoldFunctions: Set<AFunctionDeclaration>
        get() = setOf(f1)

    override fun expand(
        declaredFunction: AFunctionDeclaration, formalArguments: List<PExp>, config: IPluginConfiguration,
        envIn: ISimulationEnvironment, errorReporter: IErrorReporter
    ): List<PStm> {
        logger.debug("Unfolding: {}", declaredFunction.toString())
        val env = envIn as Fmi2SimulationEnvironment
        verifyArguments(formalArguments, env)
        val startTime = formalArguments[1].clone()
        val endTime = formalArguments[2].clone()

        return try {
            val setting = MablApiBuilder.MablSettings()
            setting.fmiErrorHandlingEnabled = false
            val builder = MablApiBuilder(setting, formalArguments[0])
            val dynamicScope = builder.dynamicScope
            val math = builder.mablToMablAPI.mathBuilder
            val booleanLogic = builder.mablToMablAPI.booleanBuilder

            // Convert raw MaBL to API
            // TODO: Create a reference value type
            val externalStartTime = DoubleVariableFmi2Api(null, null, null, null, startTime)
            // TODO: Create a reference value type
            val externalEndTime = DoubleVariableFmi2Api(null, null, null, null, endTime)
            val endTimeVar = dynamicScope.store("fixed_end_time", 0.0) as DoubleVariableFmi2Api
            endTimeVar.setValue(externalEndTime)

            // Import the external components into Fmi2API
            val fmuInstances = FromMaBLToMaBLAPI.getComponentVariablesFrom(builder, formalArguments[0], env)

            // Create bindings
            FromMaBLToMaBLAPI.createBindings(fmuInstances, env)

            this.config = config as InitializationConfig

            this.modelParameters = config.modelParameters
            this.envParameters = config.envParameters

            // Convergence related variables
            absoluteTolerance = dynamicScope.store("absoluteTolerance", this.config!!.absoluteTolerance)
            relativeTolerance = dynamicScope.store("relativeTolerance", this.config!!.relativeTolerance)
            maxConvergeAttempts = dynamicScope.store("maxConvergeAttempts", this.config!!.maxIterations)

            logger.debug("Setup experiment for all components")
            fmuInstances.values.forEach { i ->
                i.setupExperiment(
                    externalStartTime,
                    externalEndTime,
                    this.config!!.relativeTolerance
                )
            };
            val connections = createConnections(env, fmuInstances)

            //Find the right order to instantiate dependentPorts and make sure where doesn't exist any cycles in the connections
            val instantiationOrder = topologicalPlugin.findInstantiationOrderStrongComponents(connections)

            //Verification against prolog should only be done if it turned on and there is no loops
            if (this.config!!.verifyAgainstProlog && instantiationOrder.all { i -> i.size == 1 })
                initializationPrologQuery.initializationOrderIsValid(instantiationOrder.flatten(), connections)


            //Set variables for all components in IniPhase
            setComponentsVariables(fmuInstances, PhasePredicates.iniPhase(), builder)

            //Enter initialization Mode
            logger.debug("Enter initialization Mode")
            fmuInstances.values.forEach(Consumer { fmu: ComponentVariableFmi2Api -> fmu.enterInitializationMode() })

            val instructions = instantiationOrder.map { i ->
                createInitInstructions(
                    i.toList(),
                    dynamicScope,
                    fmuInstances,
                    booleanLogic,
                    math
                )
            }
            var stabilisationScope: ScopeFmi2Api? = null
            var stabilisationLoop: IntVariableFmi2Api? = null
            if (this.config!!.stabilisation) {
                stabilisationLoop = dynamicScope.store("stabilisation_loop", this.config!!.maxIterations)
                stabilisationScope = dynamicScope.enterWhile(
                    stabilisationLoop!!.toMath().greaterThan(IntExpressionValue.of(0))
                )
            }

            instructions.forEach { i -> i.perform() }

            if (stabilisationScope != null) {
                stabilisationLoop!!.decrement();
                stabilisationScope.activate()
                stabilisationScope.leave();
            }


            setRemainingInputs(fmuInstances, builder)

            //Exit initialization Mode
            fmuInstances.values.forEach(Consumer { obj: ComponentVariableFmi2Api -> obj.exitInitializationMode() })

            val algorithm = builder.buildRaw() as SBlockStm
            algorithm.apply(ToParExp())

            println(PrettyPrinter.print(algorithm))
            algorithm.body
        } catch (e: Exception) {
            throw ExpandException("Internal error: ", e)
        }
    }

    private fun setRemainingInputs(
        fmuInstances: MutableMap<String, ComponentVariableFmi2Api>,
        builder: MablApiBuilder
    ) {
        for (comp in fmuInstances.values) {
            try {
                val scalarVariables = comp.modelDescription.scalarVariables
                val inputsScalars =
                    scalarVariables.filter { x ->
                        PhasePredicates.inPhase().test(x) && !portSet(comp, x)
                    }

                val ports =
                    comp.getPorts(*inputsScalars.stream().mapToInt { sv -> Math.toIntExact(sv.getValueReference()) }
                        .toArray())

                for (port in ports) {
                    setParameterOnPort(port, comp, builder)
                }
            } catch (e: Exception) {
                throw ExpandException("Initializer failed to read scalarvariables", e)
            }
        }
    }

    private fun portSet(comp: ComponentVariableFmi2Api, x: Fmi2ModelDescription.ScalarVariable?): Boolean {
        return if (portsAlreadySet.containsKey(comp)) portsAlreadySet.getValue(comp).contains(x) else false
    }

    private fun setParameterOnPort(
        port: PortFmi2Api,
        comp: ComponentVariableFmi2Api,
        builder: MablApiBuilder
    ) {
        val fmuName = comp.name
//        var value = findUseDefault(fmuName, port.scalarVariable, modelParameters)

        val useEnvForPort = this.envParameters?.contains(port.multiModelScalarVariableName)
        if (useEnvForPort == null || !useEnvForPort) {

            var staticValue = findParameterOrDefault(fmuName, port.scalarVariable, modelParameters)
            when (port.scalarVariable.type.type!!) {
                Fmi2ModelDescription.Types.Boolean -> comp.set(port, BooleanExpressionValue.of(staticValue as Boolean))
                Fmi2ModelDescription.Types.Real -> {
                    if (staticValue is Int) {
                        staticValue = staticValue.toDouble()
                    }
                    val b: Double = staticValue as Double
                    comp.set(port, DoubleExpressionValue.of(b))
                }
                Fmi2ModelDescription.Types.Integer -> comp.set(port, IntExpressionValue.of(staticValue as Int))
                Fmi2ModelDescription.Types.String -> comp.set(port, StringExpressionValue.of(staticValue as String))
                Fmi2ModelDescription.Types.Enumeration -> throw ExpandException("Enumeration not supported")
                else -> throw ExpandException("Not known type")
            }
        } else {
            when (port.scalarVariable.type.type!!) {
                Fmi2ModelDescription.Types.Boolean -> {
                    val v = builder.executionEnvironment.getBool(port.multiModelScalarVariableName)
                    comp.set(port, v)
                }
                Fmi2ModelDescription.Types.Real -> {
                    val v = builder.executionEnvironment.getReal(port.multiModelScalarVariableName)
                    comp.set(port, v)
                }
                Fmi2ModelDescription.Types.Integer -> {
                    val v = builder.executionEnvironment.getInt(port.multiModelScalarVariableName)
                    comp.set(port, v)
                }
                Fmi2ModelDescription.Types.String -> {
                    val v = builder.executionEnvironment.getString(port.multiModelScalarVariableName)
                    comp.set(port, v)
                }
                Fmi2ModelDescription.Types.Enumeration -> throw ExpandException("Enumeration not supported")
                else -> throw ExpandException("Not known type")
            }
        }


        addToPortsAlreadySet(comp, port.scalarVariable)
    }

    private fun addToPortsAlreadySet(comp: ComponentVariableFmi2Api, port: Fmi2ModelDescription.ScalarVariable) {
        if (portsAlreadySet.containsKey(comp)) {
            portsAlreadySet.replace(comp, portsAlreadySet.getValue(comp).plus(port))
        } else {
            portsAlreadySet[comp] = setOf(port)
        }
    }

    private fun createInitInstructions(
        ports: List<Fmi2SimulationEnvironment.Variable>,
        dynamicScope: DynamicActiveBuilderScope,
        fmuInstances: Map<String, ComponentVariableFmi2Api>,
        booleanLogic: BooleanBuilderFmi2Api,
        mathBuilder: MathBuilderFmi2Api
    ): CoSimInstruction {
        return if (ports.size == 1) {
            val p = ports.last()
            fmuCoSimInstruction(fmuInstances, p)
        } else {
            val actions = ports.map { c -> fmuCoSimInstruction(fmuInstances, c) }
            val outputPorts =
                ports.filter { p -> p.scalarVariable.scalarVariable.causality == Fmi2ModelDescription.Causality.Output }
                    .map { i -> i.scalarVariable }
            LoopSimInstruction(
                dynamicScope,
                maxConvergeAttempts!!,
                absoluteTolerance!!,
                relativeTolerance!!,
                actions,
                createConvergencePorts(outputPorts, fmuInstances),
                booleanLogic,
                mathBuilder
            )
        }
    }

    private fun fmuCoSimInstruction(
        fmuInstances: Map<String, ComponentVariableFmi2Api>,
        p: Fmi2SimulationEnvironment.Variable
    ): FMUCoSimInstruction {
        val fmu = fmuInstances.getValue(p.scalarVariable.instance.text)
        val port = fmu.getPort(p.scalarVariable.scalarVariable.name)
        return when (p.scalarVariable.scalarVariable.causality) {
            Fmi2ModelDescription.Causality.Output -> GetInstruction(fmu, port, false)
            Fmi2ModelDescription.Causality.Input -> {
                addToPortsAlreadySet(fmu, port.scalarVariable)
                SetInstruction(fmu, port)
            }
            else -> throw ExpandException("Internal error")
        }
    }


    private fun createConvergencePorts(
        ports: List<RelationVariable>,
        fmuInstances: Map<String, ComponentVariableFmi2Api>
    ): Map<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Any>>> {
        val fmuToPorts = ports.groupBy { i -> i.instance.text }
            .map { i ->
                i.key to i.value.map { p ->
                    fmuInstances.getValue(i.key).getPort(p.scalarVariable.getName())
                }
            }
            .toMap()
        return fmuToPorts.map { (fmu, ports) ->
            fmuInstances.getValue(fmu) to ports.map { port ->
                port to fmuInstances[fmu]?.getSingle(
                    port.name
                )!!
            }.toMap()
        }.toMap()
    }


    private fun createConnections(
        env: Fmi2SimulationEnvironment,
        fmuInstances: Map<String, ComponentVariableFmi2Api>
    ): Set<Fmi2SimulationEnvironment.Relation> {
        return fmuInstances.values
            .flatMap { i: ComponentVariableFmi2Api ->
                env.getRelations(i.name)
                    .filter { rel: Fmi2SimulationEnvironment.Relation -> rel.direction == IRelation.Direction.OutputToInput }
            }.toSet()
    }


    private fun setComponentsVariables(
        fmuInstances: Map<String, ComponentVariableFmi2Api>,
        predicate: Predicate<Fmi2ModelDescription.ScalarVariable>,
        builder: MablApiBuilder
    ) {
        fmuInstances.entries.forEach { (fmuName, comp) ->
            for (sv in comp.modelDescription.scalarVariables.filter { i -> predicate.test(i) }) {
                val port = comp.getPort(sv.name)
                setParameterOnPort(port, comp, builder)
            }
        }
    }


    @Throws(ExpandException::class)
    private fun verifyArguments(formalArguments: List<PExp>?, env: ISimulationEnvironment?) {
        //maybe some of these tests are not necessary - but they are in my unit test
        if (formalArguments == null || formalArguments.size != f1.formals.size) {
            throw ExpandException("Invalid args")
        }
        if (env == null) {
            throw ExpandException("Simulation environment must not be null")
        }
    }

    override fun requireConfig(): Boolean {
        return true
    }

    @Throws(IOException::class)
    override fun parseConfig(`is`: InputStream): IPluginConfiguration {
        var root = ObjectMapper().readTree(`is`)
        //We are only interested in one configuration, so in case it is an array we take the first one.
        if (root is ArrayNode) {
            root = root[0]
        }
        val parameters = root["parameters"]
        val envParameters = root["environmentParameters"]
        val verify = root["verifyAgainstProlog"]
        val stabilisation = root["stabilisation"]
        val fixedPointIteration = root["fixedPointIteration"]
        val absoluteTolerance = root["absoluteTolerance"]
        val relativeTolerance = root["relativeTolerance"]
        var conf: InitializationConfig? = null
        try {
            conf = InitializationConfig(
                parameters,
                if (envParameters is NullNode) null else envParameters,
                verify,
                stabilisation,
                fixedPointIteration,
                absoluteTolerance,
                relativeTolerance
            )
        } catch (e: InvalidVariableStringException) {
            e.printStackTrace()
        }
        return conf!!
    }


    override fun getDeclaredImportUnit(): AImportedModuleCompilationUnit {
        if (compilationUnit != null) {
            return compilationUnit as AImportedModuleCompilationUnit
        }
        compilationUnit = AImportedModuleCompilationUnit()
        compilationUnit!!.imports =
            listOf("FMI2", "TypeConverter", "Math", "Logger", "MEnv").map { identifier: String? ->
                MableAstFactory.newAIdentifier(
                    identifier
                )
            }
        val module = AModuleDeclaration()
        module.name = MableAstFactory.newAIdentifier(name)
        module.setFunctions(ArrayList(declaredUnfoldFunctions))
        compilationUnit!!.module = module
        return compilationUnit as AImportedModuleCompilationUnit
    }

    class InitializationConfig(
        parameters: JsonNode?,
        envParameters: JsonNode?,
        verify: JsonNode?,
        stabilisation: JsonNode?,
        fixedPointIteration: JsonNode?,
        absoluteTolerance: JsonNode?,
        relativeTolerance: JsonNode?
    ) : IPluginConfiguration {
        var stabilisation = false
        val modelParameters: List<ModelParameter>?
        val envParameters: List<String>?
        var verifyAgainstProlog = false
        var maxIterations = 0
        var absoluteTolerance = 0.0
        var relativeTolerance = 0.0


        init {
            val mapper = ObjectMapper()
            val convertParameters: Map<String, Any>? =
                if (parameters == null) null else mapper.convertValue(
                    parameters,
                    Map::class.java
                ) as Map<String, Any>

            modelParameters =
                convertParameters?.map { (key, value) ->
                    ModelParameter(
                        ModelConnection.Variable.parse(key),
                        value
                    )
                }

            this.envParameters = if (envParameters == null) null else mapper.convertValue(
                envParameters,
                List::class.java
            ) as List<String>

            verifyAgainstProlog = verify?.asBoolean(false) ?: false
            this.stabilisation = stabilisation?.asBoolean(false) ?: false
            maxIterations = fixedPointIteration?.asInt(5) ?: 5
            if (absoluteTolerance == null) {
                this.absoluteTolerance = 0.2
            } else {
                this.absoluteTolerance = absoluteTolerance.asDouble(0.2)
            }
            if (relativeTolerance == null) {
                this.relativeTolerance = 0.1
            } else {
                this.relativeTolerance = relativeTolerance.asDouble(0.1)
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(Initializer::class.java)

        private fun findParameterOrDefault(
            compName: String,
            sv: Fmi2ModelDescription.ScalarVariable,
            modelParameters: List<ModelParameter>?
        ): Any {
            val parameterValue =
                modelParameters?.firstOrNull { x: ModelParameter -> x.variable.instance.instanceName == compName && x.variable.variable == sv.name }
            return if (parameterValue != null) parameterValue.value else sv.type.start
        }

//        /**
//         * This functions either returns null if the parameter has a value or it returns the model description start value
//         */
//        private fun findUseDefault(
//            compName: String,
//            sv: ModelDescription.ScalarVariable,
//            modelParameters: List<ModelParameter>?
//        ): Any? {
//            val parameterValue =
//                modelParameters?.firstOrNull { x: ModelParameter -> x.variable.instance.instanceName == compName && x.variable.variable == sv.name }
//            return if (parameterValue != null) null else sv.type.start
//        }
    }
}