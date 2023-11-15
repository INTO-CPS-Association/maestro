package org.intocps.maestro.plugin.initializer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NullNode
import org.intocps.maestro.ast.*
import org.intocps.maestro.ast.node.*
import org.intocps.maestro.core.Framework
import org.intocps.maestro.core.messages.IErrorReporter
import org.intocps.maestro.fmi.Fmi2ModelDescription
import org.intocps.maestro.fmi.fmi3.*
import org.intocps.maestro.framework.core.IRelation
import org.intocps.maestro.framework.core.ISimulationEnvironment
import org.intocps.maestro.framework.core.RelationVariable
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment
import org.intocps.maestro.framework.fmi2.InvalidVariableStringException
import org.intocps.maestro.framework.fmi2.ModelConnection
import org.intocps.maestro.framework.fmi2.api.FmiBuilder
import org.intocps.maestro.framework.fmi2.api.FmiBuilder.ArrayVariable
import org.intocps.maestro.framework.fmi2.api.FmiBuilder.SimulationInstance
import org.intocps.maestro.framework.fmi2.api.mabl.*
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.values.BooleanExpressionValue
import org.intocps.maestro.framework.fmi2.api.mabl.values.DoubleExpressionValue
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue
import org.intocps.maestro.framework.fmi2.api.mabl.values.StringExpressionValue
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*
import org.intocps.maestro.plugin.*
import org.intocps.maestro.plugin.IMaestroExpansionPlugin.EmptyRuntimeConfig
import org.intocps.maestro.plugin.initializer.instructions.*
import org.intocps.maestro.plugin.verificationsuite.prologverifier.InitializationPrologQuery
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors

@Suppress("UNCHECKED_CAST")
@SimulationFramework(framework = Framework.FMI2)
class Initializer : BasicMaestroExpansionPlugin {
    val f1_transfer = MableAstFactory.newAFunctionDeclaration(
        LexIdentifier("initialize_transfer", null), listOf(
            MableAstFactory.newAFormalParameter(
                MableAstFactory.newAArrayType(MableAstFactory.newANameType("FMI2Component")),
                MableAstFactory.newAIdentifier("component")
            ), MableAstFactory.newAFormalParameter(
                MableAstFactory.newAArrayType(MableAstFactory.newANameType("FMI2Component")),
                MableAstFactory.newAIdentifier("component")
            ), MableAstFactory.newAFormalParameter(
                MableAstFactory.newRealType(), MableAstFactory.newAIdentifier("startTime")
            ), MableAstFactory.newAFormalParameter(
                MableAstFactory.newRealType(), MableAstFactory.newAIdentifier("endTime")
            ), MableAstFactory.newAFormalParameter(
                MableAstFactory.newABoleanPrimitiveType(), MableAstFactory.newAIdentifier("endTimeDefined")
            )
        ), MableAstFactory.newAVoidType()
    )

    val f1 = MableAstFactory.newAFunctionDeclaration(
        LexIdentifier("initialize", null), listOf(
            MableAstFactory.newAFormalParameter(
                MableAstFactory.newAArrayType(MableAstFactory.newANameType("FMI2Component")),
                MableAstFactory.newAIdentifier("component")
            ),

            MableAstFactory.newAFormalParameter(
                MableAstFactory.newRealType(), MableAstFactory.newAIdentifier("startTime")
            ), MableAstFactory.newAFormalParameter(
                MableAstFactory.newRealType(), MableAstFactory.newAIdentifier("endTime")
            ), MableAstFactory.newAFormalParameter(
                MableAstFactory.newABoleanPrimitiveType(), MableAstFactory.newAIdentifier("endTimeDefined")
            )
        ), MableAstFactory.newAVoidType()
    )

    val f2_3 = MableAstFactory.newAFunctionDeclaration(
        LexIdentifier("initialize23", null), listOf(
            MableAstFactory.newAFormalParameter(
                MableAstFactory.newAArrayType(MableAstFactory.newANameType("FMI2Component")),
                MableAstFactory.newAIdentifier("component")
            ), MableAstFactory.newAFormalParameter(
                MableAstFactory.newAArrayType(MableAstFactory.newANameType("FMI3Instance")),
                MableAstFactory.newAIdentifier("instances")
            ),

            MableAstFactory.newAFormalParameter(
                MableAstFactory.newRealType(), MableAstFactory.newAIdentifier("startTime")
            ), MableAstFactory.newAFormalParameter(
                MableAstFactory.newRealType(), MableAstFactory.newAIdentifier("endTime")
            ), MableAstFactory.newAFormalParameter(
                MableAstFactory.newABoleanPrimitiveType(), MableAstFactory.newAIdentifier("endTimeDefined")
            )
        ), MableAstFactory.newAVoidType()
    )

    private val portsAlreadySet = HashMap<FmiBuilder.SimulationInstance<PStm>, Set<Any?>>()
    private val topologicalPlugin: TopologicalPlugin
    private val initializationPrologQuery: InitializationPrologQuery
    var config: InitializationConfig? = null
    var modelParameters: List<ModelParameter>? = null
    var envParameters: List<String>? = null
    var compilationUnit: AImportedModuleCompilationUnit? = null

    // Convergence related variables
    var absoluteTolerance: FmiBuilder.DoubleVariable<PStm>? = null
    var relativeTolerance: FmiBuilder.DoubleVariable<PStm>? = null
    var maxConvergeAttempts: FmiBuilder.IntVariable<PStm>? = null

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
        get() = setOf(f1, f1_transfer, f2_3)

    fun getActiveDeclaration(declaredFunction: AFunctionDeclaration?): AFunctionDeclaration {
        return if (declaredFunction == f1) f1 else if (declaredFunction == f1_transfer) f1_transfer else f2_3
    }

    enum class ARG_INDEX {
        FMI2_INSTANCES, FMI3_INSTANCES, TRANSFER_INSTANCES, START_TIME, END_TIME, END_TIME_DEFINED
    }

    /**
     * Mapping function from arg to value when called with the builder api
     */
    fun getArg(
        declaredFunction: AFunctionDeclaration?,
        formalArguments: MutableList<FmiBuilder.Variable<PStm, *>>?,
        index: ARG_INDEX
    ): FmiBuilder.Variable<PStm, *>? {
        when (declaredFunction) {
            f1 -> {
                return when (index) {
                    ARG_INDEX.FMI2_INSTANCES -> formalArguments?.get(0);
                    ARG_INDEX.START_TIME -> formalArguments?.get(1);
                    ARG_INDEX.END_TIME -> formalArguments?.get(2);
                    ARG_INDEX.END_TIME_DEFINED -> formalArguments?.get(3);
                    else -> null;
                }
            }

            f1_transfer -> {
                return when (index) {
                    ARG_INDEX.FMI2_INSTANCES -> formalArguments?.get(0);
                    ARG_INDEX.TRANSFER_INSTANCES -> formalArguments?.get(1);
                    ARG_INDEX.START_TIME -> formalArguments?.get(2);
                    ARG_INDEX.END_TIME -> formalArguments?.get(3);
                    ARG_INDEX.END_TIME_DEFINED -> formalArguments?.get(4);
                    else -> null;
                }
            }

            f2_3 -> {
                return when (index) {
                    ARG_INDEX.FMI2_INSTANCES -> formalArguments?.get(0);
                    ARG_INDEX.FMI3_INSTANCES -> formalArguments?.get(1);
                    ARG_INDEX.START_TIME -> formalArguments?.get(2);
                    ARG_INDEX.END_TIME -> formalArguments?.get(3);
                    ARG_INDEX.END_TIME_DEFINED -> formalArguments?.get(4);
                    else -> null;
                }
            }

            else -> return null
        }
    }

    /**
     * Mapping function from arg to value when called as plain with exp
     */
    fun getArg(
        declaredFunction: AFunctionDeclaration?,
        builder: MablApiBuilder,
        env: Fmi2SimulationEnvironment,
        formalArguments: List<PExp>,
        index: ARG_INDEX
    ): FmiBuilder.Variable<PStm, *>? {
        when (declaredFunction) {
            f1 -> {
                return when (index) {
                    ARG_INDEX.FMI2_INSTANCES -> ArrayVariableFmi2Api(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        FromMaBLToMaBLAPI.getComponentVariablesFrom(builder, formalArguments[0], env).values.stream()
                            .collect(Collectors.toList()) as List<VariableFmi2Api<FmiBuilder.NamedVariable<PStm>>>?
                    );
                    ARG_INDEX.START_TIME -> DoubleVariableFmi2Api(null, null, null, null, formalArguments[1].clone());
                    ARG_INDEX.END_TIME -> DoubleVariableFmi2Api(null, null, null, null, formalArguments[2].clone());
                    ARG_INDEX.END_TIME_DEFINED -> BooleanVariableFmi2Api(
                        null, null, null, null, formalArguments[3].clone()
                    );
                    else -> null;
                }
            }

            f1_transfer -> {
                return when (index) {
                    ARG_INDEX.FMI2_INSTANCES -> ArrayVariableFmi2Api(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        FromMaBLToMaBLAPI.getComponentVariablesFrom(builder, formalArguments[0], env).values.stream()
                            .collect(Collectors.toList()) as List<VariableFmi2Api<FmiBuilder.NamedVariable<PStm>>>?
                    );
                    ARG_INDEX.TRANSFER_INSTANCES -> ArrayVariableFmi2Api(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        FromMaBLToMaBLAPI.getComponentVariablesFrom(builder, formalArguments[1], env).values.stream()
                            .collect(Collectors.toList()) as List<VariableFmi2Api<FmiBuilder.NamedVariable<PStm>>>?
                    );
                    ARG_INDEX.START_TIME -> DoubleVariableFmi2Api(null, null, null, null, formalArguments[2].clone());
                    ARG_INDEX.END_TIME -> DoubleVariableFmi2Api(null, null, null, null, formalArguments[3].clone());
                    ARG_INDEX.END_TIME_DEFINED -> BooleanVariableFmi2Api(
                        null, null, null, null, formalArguments[4].clone()
                    );
                    else -> null;
                }
            }

            f2_3 -> {
                return when (index) {
                    ARG_INDEX.FMI2_INSTANCES -> ArrayVariableFmi2Api(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        FromMaBLToMaBLAPI.getComponentVariablesFrom(builder, formalArguments[0], env).values.stream()
                            .collect(Collectors.toList()) as List<VariableFmi2Api<FmiBuilder.NamedVariable<PStm>>>?
                    );
                    ARG_INDEX.FMI3_INSTANCES -> null;
                    ARG_INDEX.START_TIME -> DoubleVariableFmi2Api(null, null, null, null, formalArguments[2].clone());
                    ARG_INDEX.END_TIME -> DoubleVariableFmi2Api(null, null, null, null, formalArguments[3].clone());
                    ARG_INDEX.END_TIME_DEFINED -> BooleanVariableFmi2Api(
                        null, null, null, null, formalArguments[4].clone()
                    );
                    else -> null;
                }
            }

            else -> return null
        }
    }

    private fun toCompMap(map: List<ComponentVariableFmi2Api>?): Map<String, ComponentVariableFmi2Api> {
        return if (map == null) emptyMap() else map.stream()
            .collect(Collectors.toMap({ v: ComponentVariableFmi2Api -> v.name },
                Function.identity(),
                { u: ComponentVariableFmi2Api?, v: ComponentVariableFmi2Api? -> u }) { LinkedHashMap() })
    }

    private fun toInstanceMap(map: List<InstanceVariableFmi3Api>?): Map<String, InstanceVariableFmi3Api> {
        return if (map == null) emptyMap() else map.stream()
            .collect(Collectors.toMap({ v: InstanceVariableFmi3Api -> v.name },
                Function.identity(),
                { u: InstanceVariableFmi3Api?, v: InstanceVariableFmi3Api? -> u }) { LinkedHashMap() })
    }

    override fun <R : Any?> expandWithRuntimeAddition(
        declaredFunction: AFunctionDeclaration?,
        builder: FmiBuilder<PStm, ASimulationSpecificationCompilationUnit, PExp, *>?,
        formalArguments: MutableList<FmiBuilder.Variable<PStm, *>>?,
        config: IPluginConfiguration?,
        envIn: ISimulationEnvironment?,
        errorReporter: IErrorReporter?
    ): IMaestroExpansionPlugin.RuntimeConfigAddition<R> {
        if (!declaredUnfoldFunctions.contains(declaredFunction)) {
            throw ExpandException("Unknown function declaration")
        }

        if (envIn == null) {
            throw ExpandException("Simulation environment must not be null")
        }

        if (formalArguments == null || formalArguments.size != getActiveDeclaration(declaredFunction).getFormals().size) {
            throw ExpandException("Invalid args")
        }

        val env = envIn as Fmi2SimulationEnvironment
        try {
            if (builder !is MablApiBuilder && builder != null) {
                throw ExpandException(
                    "Not supporting the given builder type. Expecting " + MablApiBuilder::class.java.simpleName + " got " + builder.javaClass.simpleName
                )
            }

            val builder = builder as MablApiBuilder

            val dynamicScope = builder.dynamicScope
            val math = builder.mablToMablAPI.mathBuilder
            val booleanLogic = builder.booleanBuilder


            val fmuInstancesTransfer: List<ComponentVariableFmi2Api>? = ((getArg(
                declaredFunction, formalArguments, ARG_INDEX.TRANSFER_INSTANCES
            ) as? ArrayVariable<*, *>)?.items()) as? List<ComponentVariableFmi2Api>;

            val externalStartTime =
                getArg(declaredFunction, formalArguments, ARG_INDEX.START_TIME) as DoubleVariableFmi2Api
            val externalEndTime = getArg(declaredFunction, formalArguments, ARG_INDEX.END_TIME) as DoubleVariableFmi2Api
            val externalEndTimeDefined =
                getArg(declaredFunction, formalArguments, ARG_INDEX.END_TIME_DEFINED) as BooleanVariableFmi2Api

            val endTimeVar = dynamicScope.store("fixed_end_time", 0.0) as DoubleVariableFmi2Api
            endTimeVar.setValue(externalEndTime)

            // use LinkedHashMap to preserve added order
            val fmuInstances: List<ComponentVariableFmi2Api>? = ((getArg(
                declaredFunction, formalArguments, ARG_INDEX.FMI2_INSTANCES
            ) as? ArrayVariable<*, *>)?.items() as? List<ComponentVariableFmi2Api>)

            val fmu3Instances: List<InstanceVariableFmi3Api>? = ((getArg(
                declaredFunction, formalArguments, ARG_INDEX.FMI3_INSTANCES
            ) as? ArrayVariable<*, *>)?.items() as? List<InstanceVariableFmi3Api>)

            expansionLogic(
                config,
                dynamicScope,
                toCompMap(fmuInstances),
                toInstanceMap(fmu3Instances),
                toCompMap(fmuInstancesTransfer),
                externalStartTime,
                externalEndTime,
                externalEndTimeDefined,
                env,
                builder,
                booleanLogic,
                math
            )


        } catch (e: Exception) {
            throw ExpandException("Internal error: ", e)
        }

        return EmptyRuntimeConfig()
    }

    private fun expansionLogic(
        config: IPluginConfiguration?,
        dynamicScope: DynamicActiveBuilderScope,
        fmuInstancesIn: Map<String, ComponentVariableFmi2Api>,
        fmu3InstancesIn: Map<String, InstanceVariableFmi3Api>,
        fmuInstancesTransfer: Map<String, ComponentVariableFmi2Api>,
        externalStartTime: DoubleVariableFmi2Api,
        externalEndTime: DoubleVariableFmi2Api,
        externalEndTimeDefined: BooleanVariableFmi2Api,
        env: Fmi2SimulationEnvironment,
        builder: MablApiBuilder,
        booleanLogic: BooleanBuilderFmi2Api,
        math: MathBuilderFmi2Api
    ) {
        this.config = config as InitializationConfig

        this.modelParameters = config.modelParameters
        this.envParameters = config.envParameters

        // Convergence related variables
        absoluteTolerance = dynamicScope.store("absoluteTolerance", this.config!!.absoluteTolerance)
        relativeTolerance = dynamicScope.store("relativeTolerance", this.config!!.relativeTolerance)
        maxConvergeAttempts = dynamicScope.store("maxConvergeAttempts", this.config!!.maxIterations)

        val fmuInstances = fmuInstancesIn.filterKeys { !fmuInstancesTransfer.keys.contains(it) };
        val fmu3Instances = fmu3InstancesIn.filterKeys { !fmuInstancesTransfer.keys.contains(it) };

        logger.debug("Setup experiment for all components")
        fmuInstances.values.forEach { i ->
            i.setupExperiment(
                externalStartTime, externalEndTime, externalEndTimeDefined, this.config!!.relativeTolerance
            )
        };


        val connections = createConnections(env, fmuInstances).plus(createConnections3(env, fmu3Instances))

        val filterTargets = createTargetFilter(fmuInstancesTransfer, connections)

        //Find the right order to instantiate dependentPorts and make sure where doesn't exist any cycles in the connections
        val instantiationOrder = topologicalPlugin.findInstantiationOrderStrongComponents(connections, filterTargets)

        //Verification against prolog should only be done if it turned on and there is no loops
        if (this.config!!.verifyAgainstProlog && instantiationOrder.all { i -> i.size == 1 }) initializationPrologQuery.initializationOrderIsValid(
            instantiationOrder.flatten(),
            connections
        )


        //Set variables for all components in IniPhase
        setComponentsVariables(fmuInstances, PhasePredicates.iniPhase(), builder)
        setInstanceVariables(fmu3Instances, PhasePredicates3.iniPhase(), builder)

        //Enter initialization Mode
        logger.debug("Enter initialization Mode")
        fmuInstances.values.forEach(Consumer { fmu: ComponentVariableFmi2Api -> fmu.enterInitializationMode() })

        val fmi3ToloranceDefined = dynamicScope.store("absoluteTolerance", this.config!!.absoluteTolerance)
        fmu3Instances.values.forEach(Consumer { fmu: InstanceVariableFmi3Api ->
            fmu.enterInitializationMode(
                VarWrap.wrap(false),
                VarWrap.wrap(0.0),
                externalStartTime,
                externalEndTimeDefined,
                externalEndTime
            )
        })


        //TODO not sure what this is - stabilization maybe
        val instructions = instantiationOrder.map { i ->
            createInitInstructions(
                i.toList(), dynamicScope, fmuInstances, fmu3Instances, booleanLogic, math
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
        //TODO end


        setRemainingInputs(fmuInstances, builder)
        setRemainingInputs3(fmu3Instances, builder)

        //Exit initialization Mode
        fmuInstances.values.forEach(Consumer { obj: ComponentVariableFmi2Api -> obj.exitInitializationMode() })
        fmu3Instances.values.forEach(Consumer { obj: InstanceVariableFmi3Api -> obj.exitInitializationMode() })
    }


    override fun expand(
        declaredFunction: AFunctionDeclaration,
        formalArguments: List<PExp>,
        config: IPluginConfiguration,
        envIn: ISimulationEnvironment,
        errorReporter: IErrorReporter
    ): List<PStm> {
        logger.debug("Unfolding: {}", declaredFunction.toString())
        val env = envIn as Fmi2SimulationEnvironment
        verifyArguments(declaredFunction, formalArguments, env)

        return try {
            val setting = MablApiBuilder.MablSettings()
            setting.fmiErrorHandlingEnabled = false
            val builder = MablApiBuilder(setting, formalArguments[0])
            val dynamicScope = builder.dynamicScope
            val math = builder.mablToMablAPI.mathBuilder
            val booleanLogic = builder.mablToMablAPI.booleanBuilder

            val fmuInstancesTransfer: List<ComponentVariableFmi2Api>? = (((getArg(
                declaredFunction, builder, env, formalArguments, ARG_INDEX.TRANSFER_INSTANCES
            ) as? ArrayVariable<*, *>)?.items()) as? List<ComponentVariableFmi2Api>)

            val externalStartTime =
                getArg(declaredFunction, builder, env, formalArguments, ARG_INDEX.START_TIME) as DoubleVariableFmi2Api
            val externalEndTime =
                getArg(declaredFunction, builder, env, formalArguments, ARG_INDEX.END_TIME) as DoubleVariableFmi2Api
            val externalEndTimeDefined = getArg(
                declaredFunction, builder, env, formalArguments, ARG_INDEX.END_TIME_DEFINED
            ) as BooleanVariableFmi2Api

            val endTimeVar = dynamicScope.store("fixed_end_time", 0.0) as DoubleVariableFmi2Api
            endTimeVar.setValue(externalEndTime)

            // use LinkedHashMap to preserve added order
            val fmuInstances: List<ComponentVariableFmi2Api>? = (((getArg(
                declaredFunction, builder, env, formalArguments, ARG_INDEX.FMI2_INSTANCES
            ) as? ArrayVariable<*, *>)?.items()) as? List<ComponentVariableFmi2Api>)

            val fmu3Instances: List<InstanceVariableFmi3Api>? = (((getArg(
                declaredFunction, builder, env, formalArguments, ARG_INDEX.FMI3_INSTANCES
            ) as? ArrayVariable<*, *>)?.items()) as? List<InstanceVariableFmi3Api>)


            expansionLogic(
                config,
                builder.dynamicScope,
                toCompMap(fmuInstances),
                toInstanceMap(fmu3Instances),
                toCompMap(fmuInstancesTransfer),
                externalStartTime,
                externalEndTime,
                externalEndTimeDefined,
                env,
                builder,
                booleanLogic,
                math
            );


            val algorithm = builder.buildRaw() as SBlockStm
            algorithm.apply(ToParExp())


            algorithm.body
        } catch (e: Exception) {
            throw ExpandException("Internal error: ", e)
        }
    }

    private fun setRemainingInputs(
        fmuInstances: Map<String, ComponentVariableFmi2Api>, builder: MablApiBuilder
    ) {
        for (comp in fmuInstances.values) {
            try {
                val scalarVariables = comp.modelDescription.scalarVariables
                val inputsScalars = scalarVariables.filter { x ->
                    PhasePredicates.inPhase().test(x) && !isPortSet(comp, x)
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

    private fun setRemainingInputs3(
        fmuInstances: Map<String, InstanceVariableFmi3Api>, builder: MablApiBuilder
    ) {
        for (comp in fmuInstances.values) {
            try {
                val scalarVariables = comp.modelDescription.getScalarVariables()
                val inputsScalars = scalarVariables.filter { x ->
                    PhasePredicates3.inPhase().test(x.variable) && !isPortSet(comp, x.variable)
                }

                val ports =
                    comp.getPorts(
                        *inputsScalars.stream()
                            .mapToInt { sv -> Math.toIntExact(sv.variable.getValueReferenceAsLong()) }
                            .toArray())

                for (port in ports) {
                    setParameterOnPort(port, comp, builder)
                }
            } catch (e: Exception) {
                throw ExpandException("Initializer failed to read scalarvariables", e)
            }
        }
    }

    private fun isPortSet(comp: SimulationInstance<PStm>, x: Any?): Boolean {
        return if (portsAlreadySet.containsKey(comp)) portsAlreadySet.getValue(comp).contains(x) else false
    }


    private fun setParameterOnPort(
        port: PortFmi2Api, comp: ComponentVariableFmi2Api, builder: MablApiBuilder
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

    private fun setParameterOnPort(
        port: PortFmi3Api, comp: InstanceVariableFmi3Api, builder: MablApiBuilder
    ) {
        val fmuName = comp.name
//        var value = findUseDefault(fmuName, port.scalarVariable, modelParameters)

        val useEnvForPort = this.envParameters?.contains(port.multiModelScalarVariableName)
        if (useEnvForPort == null || !useEnvForPort) {

            var staticValue = findParameterOrDefault(fmuName, port.scalarVariable, modelParameters)
            when (port.scalarVariable.variable.typeIdentifier!!) {
                Fmi3TypeEnum.BooleanType -> comp.set(port, BooleanExpressionValue.of(staticValue as Boolean))
                Fmi3TypeEnum.Float64Type -> {

                    if (port.scalarVariable.variable.isScalar()) {
                        staticValue = (staticValue as List<Any>).get(0)


                        if (staticValue is Int) {
                            staticValue = staticValue.toDouble()
                        }
                        val b: Double = staticValue as Double
                        comp.set(port, DoubleExpressionValue.of(b))
                    }
                }

                Fmi3TypeEnum.Int32Type -> comp.set(port, IntExpressionValue.of(staticValue as Int))
                Fmi3TypeEnum.StringType -> comp.set(port, StringExpressionValue.of(staticValue as String))
                Fmi3TypeEnum.EnumerationType -> throw ExpandException("Enumeration not supported")
                else -> throw ExpandException("Not known type")
            }
        } else {
            when (port.scalarVariable.variable.typeIdentifier!!) {
                Fmi3TypeEnum.BooleanType -> {
                    val v = builder.executionEnvironment.getBool(port.multiModelScalarVariableName)
                    comp.set(port, v)
                }

                Fmi3TypeEnum.Float64Type -> {
                    val v = builder.executionEnvironment.getReal(port.multiModelScalarVariableName)
                    comp.set(port, v)
                }

                Fmi3TypeEnum.Int32Type -> {
                    val v = builder.executionEnvironment.getInt(port.multiModelScalarVariableName)
                    comp.set(port, v)
                }

                Fmi3TypeEnum.StringType -> {
                    val v = builder.executionEnvironment.getString(port.multiModelScalarVariableName)
                    comp.set(port, v)
                }

                Fmi3TypeEnum.EnumerationType -> throw ExpandException("Enumeration not supported")
                else -> throw ExpandException("Not known type")
            }
        }


        addToPortsAlreadySet(comp, port.scalarVariable)
    }

    private fun addToPortsAlreadySet(comp: FmiBuilder.SimulationInstance<PStm>, port: Any?) {
        if (portsAlreadySet.containsKey(comp)) {
            portsAlreadySet.replace(comp, portsAlreadySet.getValue(comp).plus(port))
        } else {
            portsAlreadySet[comp] = setOf(port)
        }
    }

    private fun createInitInstructions(
        ports: List<org.intocps.maestro.framework.fmi2.RelationVariable<Any>>,
        dynamicScope: DynamicActiveBuilderScope,
        fmuInstances: Map<String, ComponentVariableFmi2Api>,
        fmu3Instances: Map<String, InstanceVariableFmi3Api>,
        booleanLogic: BooleanBuilderFmi2Api,
        mathBuilder: MathBuilderFmi2Api
    ): CoSimInstruction {
        return if (ports.size == 1) {
            val p = ports.last()
            fmuCoSimInstruction(fmuInstances, fmu3Instances, p)
        } else {
            val actions = ports.map { c -> fmuCoSimInstruction(fmuInstances, fmu3Instances, c) }
            val outputPorts =
                ports.filter { p -> p.has(Fmi2ModelDescription.Causality.Output) || p.has(Fmi3Causality.Output) }
                    .map { i -> i }
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
        fmu3Instances: Map<String, InstanceVariableFmi3Api>,
        p: org.intocps.maestro.framework.fmi2.RelationVariable<Any>
    ): CoSimInstruction {
        val instance = fmuInstances.values.find { x -> x.environmentName.equals(p.getInstance().text) }
//        val fmu = fmuInstances.getValue(p.scalarVariable.instance.text)
        if (instance != null) {
            val port = instance.getPort(p.name)
            return when {
                p.has(Fmi2ModelDescription.Causality.Output) -> GetInstruction(instance, port, false)
                p.has(Fmi2ModelDescription.Causality.Input) -> {
                    addToPortsAlreadySet(instance, port.scalarVariable)
                    SetInstruction(instance, port)
                }

                else -> throw ExpandException("Internal error")
            }
        } else {
            val instance3 = fmu3Instances.values.find { x -> x.environmentName.equals(p.getInstance().text) }
//        val fmu = fmuInstances.getValue(p.scalarVariable.instance.text)
            if (instance3 != null) {
                val port = instance3.getPort(p.name)
                return when {
                    p.has(Fmi3Causality.Output) -> GetInstruction3(instance3, port, false)
                    p.has(Fmi3Causality.Input) -> {
                        addToPortsAlreadySet(instance3, port.scalarVariable)
                        SetInstruction3(instance3, port)
                    }

                    else -> throw ExpandException("Internal error")
                }

            } else
                throw ExpandException("Failed to retrieve FMUInstance by name: " + p.name)
        }
    }


    private fun createConvergencePorts(
        ports: List<RelationVariable>, fmuInstances: Map<String, ComponentVariableFmi2Api>
    ): Map<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Any>>> {
        val fmuToPorts = ports.groupBy { i -> i.instance.text }.map { i ->
            i.key to i.value.map { p ->
                fmuInstances.getValue(i.key).getPort(p.name)
            }
        }.toMap()
        return fmuToPorts.map { (fmu, ports) ->
            fmuInstances.getValue(fmu) to ports.map { port ->
                port to fmuInstances[fmu]?.getSingle(
                    port.name
                )!!
            }.toMap()
        }.toMap()
    }


    private fun createConnections(
        env: Fmi2SimulationEnvironment, fmuInstances: Map<String, ComponentVariableFmi2Api>
    ): Set<Fmi2SimulationEnvironment.Relation> {
        return fmuInstances.values.flatMap { i: ComponentVariableFmi2Api ->
            env.getRelations(i.environmentName)
                .filter { rel: Fmi2SimulationEnvironment.Relation -> rel.direction == IRelation.Direction.OutputToInput }
        }.toSet()
    }

    private fun createConnections3(
        env: Fmi2SimulationEnvironment, fmuInstances: Map<String, InstanceVariableFmi3Api>
    ): Set<Fmi2SimulationEnvironment.Relation> {
        return fmuInstances.values.flatMap { i: InstanceVariableFmi3Api ->
            env.getRelations(i.environmentName)
                .filter { rel: Fmi2SimulationEnvironment.Relation -> rel.direction == IRelation.Direction.OutputToInput }
        }.toSet()
    }

    private fun createTargetFilter(
        fmuInstancesTransfer: Map<String, ComponentVariableFmi2Api>,
        connections: Set<Fmi2SimulationEnvironment.Relation>
    ): Set<LexIdentifier> {
        val filter = mutableListOf<LexIdentifier>()
        connections.forEach { c ->
            for (t in c.targets) {
                if (fmuInstancesTransfer.containsKey(t.key.text)) {
                    filter.add(t.key)
                }
            }
        }
        return filter.toSet()
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

    private fun setInstanceVariables(
        fmuInstances: Map<String, InstanceVariableFmi3Api>,
        predicate: Predicate<Fmi3Variable>,
        builder: MablApiBuilder
    ) {
        fmuInstances.entries.forEach { (fmuName, comp) ->
            for (sv in comp.modelDescription.getModelVariables().filter { i -> predicate.test(i) }) {
                val port = comp.getPort(sv.name)
                setParameterOnPort(port, comp, builder)
            }
        }
    }


    @Throws(ExpandException::class)
    private fun verifyArguments(
        declaredFunction: AFunctionDeclaration, formalArguments: List<PExp>?, env: ISimulationEnvironment?
    ) {
        //maybe some of these tests are not necessary - but they are in my unit test
        if (formalArguments == null || formalArguments.size != getActiveDeclaration(declaredFunction).formals.size) {
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
                if (parameters is NullNode) null else parameters,
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
        compilationUnit!!.imports = listOf(
            "FMI2", "FMI3", "TypeConverter", "Math", "Logger", "MEnv", "BooleanLogic"
        ).map { identifier: String? ->
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
            val convertParameters: Map<String, Any>? = if (parameters == null) null else mapper.convertValue(
                parameters, Map::class.java
            ) as Map<String, Any>

            modelParameters = convertParameters?.map { (key, value) ->
                ModelParameter(
                    ModelConnection.Variable.parse(key), value
                )
            }

            this.envParameters = if (envParameters == null) null else mapper.convertValue(
                envParameters, List::class.java
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
            compName: String, sv: Fmi2ModelDescription.ScalarVariable, modelParameters: List<ModelParameter>?
        ): Any {
            val parameterValue =
                modelParameters?.firstOrNull { x: ModelParameter -> x.variable.instance.instanceName == compName && x.variable.variable == sv.name }
            return if (parameterValue != null) parameterValue.value else sv.type.start
        }

        private fun findParameterOrDefault(
            compName: String, sv: Fmi3ModelDescription.Fmi3ScalarVariable, modelParameters: List<ModelParameter>?
        ): Any? {
            val parameterValue =
                modelParameters?.firstOrNull { x: ModelParameter -> x.variable.instance.instanceName == compName && x.variable.variable == sv.variable.name }
            return if (parameterValue != null) parameterValue.value else getStartValue(sv.variable)
        }

        private fun getStartValue(variable: Fmi3Variable): Any? {
            return when (variable) {
                is FloatVariable -> variable.start
                is Int64Variable -> variable.start
                is IntVariable -> variable.start
                is BooleanVariable -> variable.start
                is StringVariable -> variable.start
                is BinaryVariable -> variable.start
                is EnumerationVariable -> variable.start
                is ClockVariable -> null
                else -> null

            }
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