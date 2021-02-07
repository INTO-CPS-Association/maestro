package org.intocps.maestro.plugin.Initializer

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import org.intocps.maestro.ast.AFunctionDeclaration
import org.intocps.maestro.ast.AModuleDeclaration
import org.intocps.maestro.ast.LexIdentifier
import org.intocps.maestro.ast.MableAstFactory.*
import org.intocps.maestro.ast.ToParExp
import org.intocps.maestro.ast.display.PrettyPrinter
import org.intocps.maestro.ast.node.ABlockStm
import org.intocps.maestro.ast.node.AImportedModuleCompilationUnit
import org.intocps.maestro.ast.node.PExp
import org.intocps.maestro.ast.node.PStm
import org.intocps.maestro.core.Framework
import org.intocps.maestro.core.messages.IErrorReporter
import org.intocps.maestro.framework.core.IRelation
import org.intocps.maestro.framework.core.ISimulationEnvironment
import org.intocps.maestro.framework.core.RelationVariable
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder
import org.intocps.maestro.framework.fmi2.api.mabl.*
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api
import org.intocps.maestro.plugin.ExpandException
import org.intocps.maestro.plugin.IMaestroExpansionPlugin
import org.intocps.maestro.plugin.IPluginConfiguration
import org.intocps.maestro.plugin.Initializer.instructions.*
import org.intocps.maestro.plugin.SimulationFramework
import org.intocps.maestro.plugin.verificationsuite.PrologVerifier.InitializationPrologQuery
import org.intocps.orchestration.coe.config.InvalidVariableStringException
import org.intocps.orchestration.coe.config.ModelConnection
import org.intocps.orchestration.coe.config.ModelParameter
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.util.function.Consumer
import java.util.function.Predicate

@SimulationFramework(framework = Framework.FMI2)
class Initializer : IMaestroExpansionPlugin {
    val f1 = newAFunctionDeclaration(LexIdentifier("initialize", null),
            listOf(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newRealType(), newAIdentifier("startTime")), newAFormalParameter(newRealType(), newAIdentifier("endTime"))),
            newAVoidType())

    //private val portsAlreadySet = HashMap<ModelInstance, HashSet<ScalarVariable>>()
    private val topologicalPlugin: TopologicalPlugin
    private val initializationPrologQuery: InitializationPrologQuery
    var config: InitializationConfig? = null
    var modelParameters: List<ModelParameter>? = null
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

    override fun expand(declaredFunction: AFunctionDeclaration, formalArguments: List<PExp>, config: IPluginConfiguration,
                        envIn: ISimulationEnvironment, errorReporter: IErrorReporter): List<PStm> {
        logger.debug("Unfolding: {}", declaredFunction.toString())
        val env = envIn as Fmi2SimulationEnvironment
        verifyArguments(formalArguments, env)
        val startTime = formalArguments[1].clone()
        val endTime = formalArguments[2].clone()

        return try {
            val builder = MablApiBuilder()
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
            val fmuInstances = FromMaBLToMaBLAPI.GetComponentVariablesFrom(builder, formalArguments[0], env)

            // Create bindings
            FromMaBLToMaBLAPI.CreateBindings(fmuInstances, env)

            this.config = config as InitializationConfig

            // Convergence related variables
            absoluteTolerance = dynamicScope.store("absoluteTolerance", this.config!!.absoluteTolerance)
            relativeTolerance = dynamicScope.store("relativeTolerance", this.config!!.relativeTolerance)
            maxConvergeAttempts = dynamicScope.store("maxConvergeAttempts", this.config!!.maxIterations)

            logger.debug("Setup experiment for all components")
            fmuInstances.values.forEach{i -> i.setupExperiment(externalStartTime, externalEndTime, this.config!!.relativeTolerance)};
            val connections = createConnections(env, fmuInstances)



            //Find the right order to instantiate dependentPorts and make sure where doesn't exist any cycles in the connections
            val instantiationOrder = topologicalPlugin.findInstantiationOrderStrongComponents(connections)

            //Set variables for all components in IniPhase
            //setComponentsVariables(env, fmuInstances, PhasePredicates.iniPhase())


            //Enter initialization Mode
            logger.debug("Enter initialization Mode")
            fmuInstances.values.forEach(Consumer { obj: ComponentVariableFmi2Api -> obj.enterInitializationMode() })
            /*
            for ((key, value) in fmuInstances) {
                val instanceByLexName = env.getInstanceByLexName(key)
                var scalarVariables: List<ScalarVariable>? = null
                try {
                    scalarVariables = instanceByLexName.getModelDescription().scalarVariables
                    // I want all the input scalar variables that are not connected to anything

                    /*var inputsScalars =
                        scalarVariables.stream().filter(x -> x.causality == ModelDescription.Causality.Input).collect(Collectors.toList());

                var ports = value.getPorts(inputsScalars.stream().mapToInt(sv -> Math.toIntExact(sv.getValueReference())).toArray());

                var valueL = ports.stream().map(sv -> {
                    var x = FindParameterOrDefault(value.getName(), sv.scalarVariable, modelParameters);
                    return (x;
                }).collect(toList());


                ports.forEach(p -> value.set(p, ));
*/
                    val collect = scalarVariables.filter { x: ScalarVariable -> x.causality == Causality.Input }.groupBy { x: ScalarVariable -> x.type.type }.toMap()
                    for ((svType, svs) in collect) {
                        // Sort the svs according to valuereference
                        svs.sortedBy { a: ScalarVariable -> a.valueReference }
                        when (svType) {
                            Types.Boolean -> value.getPorts(*svs.stream().mapToInt { sv: ScalarVariable -> Math.toIntExact(sv.getValueReference()) }.toArray()).forEach(Consumer { p: PortFmi2Api -> value[p] = FindParameterOrDefault(key, p.scalarVariable, modelParameters) as Fmi2Builder.Value<*> })
                            Types.Real -> value.getPorts(*svs.stream().mapToInt { sv: ScalarVariable -> Math.toIntExact(sv.getValueReference()) }.toArray()).forEach(Consumer { p: PortFmi2Api -> value[p] = FindParameterOrDefault(key, p.scalarVariable, modelParameters) as Fmi2Builder.Value<*> })
                            Types.Integer -> value.getPorts(*svs.stream().mapToInt { sv: ScalarVariable -> Math.toIntExact(sv.getValueReference()) }.toArray()).forEach(Consumer { p: PortFmi2Api -> value[p] = FindParameterOrDefault(key, p.scalarVariable, modelParameters) as Fmi2Builder.Value<*> })
                            Types.String -> value.getPorts(*svs.stream().mapToInt { sv: ScalarVariable -> Math.toIntExact(sv.getValueReference()) }.toArray()).forEach(Consumer { p: PortFmi2Api -> value[p] = FindParameterOrDefault(key, p.scalarVariable, modelParameters) as Fmi2Builder.Value<*> })
                            Types.Enumeration -> throw ExpandException("Enumeration not supported")
                        }
                    }
                } catch (e: Exception) {
                    throw ExpandException("Initializer failed to read scalarvariables", e)
                }
            }

             */


            // // All inputs

            val instructions = instantiationOrder.map { i -> createInitInstructions(i.toList(), dynamicScope, fmuInstances, booleanLogic, math) }

            instructions.forEach{ i -> i.perform()}


            //Exit initialization Mode
            fmuInstances.values.forEach(Consumer { obj: ComponentVariableFmi2Api -> obj.exitInitializationMode() })
            val algorithm = builder.buildRaw() as ABlockStm
            algorithm.apply(ToParExp())
            println(PrettyPrinter.print(algorithm))
            algorithm.body
        } catch (e: Exception) {
            throw ExpandException("Internal error: ", e)
        }
    }

    private fun createInitInstructions(ports: List<Fmi2SimulationEnvironment.Variable>, dynamicScope: DynamicActiveBuilderScope, fmuInstances: Map<String, ComponentVariableFmi2Api>, booleanLogic: BooleanBuilderFmi2Api, mathBuilder: MathBuilderFmi2Api): CoSimInstruction {
        return if (ports.size == 1) {
            val p = ports.last()
            fmuCoSimInstruction(fmuInstances, p)
        } else {
            val actions = ports.map { c -> fmuCoSimInstruction(fmuInstances, c) }
            val outputPorts = ports.filter { p -> p.scalarVariable.scalarVariable.causality == Causality.Output}.map { i -> i.scalarVariable }
            LoopSimInstruction(dynamicScope, maxConvergeAttempts!!, absoluteTolerance!!, relativeTolerance!!, actions, createConvergencePorts(outputPorts, fmuInstances), booleanLogic, mathBuilder)
        }
    }

    private fun fmuCoSimInstruction(fmuInstances: Map<String, ComponentVariableFmi2Api>, p: Fmi2SimulationEnvironment.Variable): FMUCoSimInstruction {
        val fmu = fmuInstances.getValue(p.scalarVariable.instance.text)
        val port = fmu.getPort(p.scalarVariable.scalarVariable.name)
        return when (p.scalarVariable.scalarVariable.causality) {
            Causality.Output -> GetInstruction(fmu, port, false)
            Causality.Input -> SetInstruction(fmu, port)
            else -> throw ExpandException("Internal error")
        }
    }


    private fun createConvergencePorts(ports: List<RelationVariable>, fmuInstances: Map<String, ComponentVariableFmi2Api>): Map<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Any>>> {
        val fmuToPorts = ports.groupBy { i -> i.instance.text }.map { i -> i.key to i.value.map { p -> fmuInstances.getValue(i.key).getPort(p.scalarVariable.getName()) }  }.toMap()
        return fmuToPorts.map { (fmu, ports) -> fmuInstances.getValue(fmu) to ports.map { port -> port to fmuInstances[fmu]?.getSingle(port.name)!!}.toMap()}.toMap()
    }



    private fun createConnections(env: Fmi2SimulationEnvironment,
                                  fmuInstances: Map<String, ComponentVariableFmi2Api>): Set<Fmi2SimulationEnvironment.Relation> {
        return fmuInstances.values
                .flatMap { i: ComponentVariableFmi2Api -> env.getRelations(i.name).filter { rel: Fmi2SimulationEnvironment.Relation -> rel.direction == IRelation.Direction.OutputToInput } }.toSet()
    }


    private fun setComponentsVariables(env: Fmi2SimulationEnvironment, fmuInstances: Map<String, ComponentVariableFmi2Api>,
                                       predicate: Predicate<ScalarVariable>) {
        fmuInstances.entries.forEach(Consumer { comp: Map.Entry<String, ComponentVariableFmi2Api> ->
                val variablesToInitialize = env.getModelDescription(comp.key).scalarVariables.filter { i -> predicate.test(i) }.groupBy { o: ScalarVariable -> o.getType().type }.toMap()
                if (variablesToInitialize.isNotEmpty()) {
                    variablesToInitialize.forEach { (type: Types, variables: List<ScalarVariable>) ->
                        for (variable in variables) {
                            //comp.value.set(variable.)

                        }
                    }
                }

        })
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
        val verify = root["verifyAgainstProlog"]
        val stabilisation = root["stabilisation"]
        val fixedPointIteration = root["fixedPointIteration"]
        val absoluteTolerance = root["absoluteTolerance"]
        val relativeTolerance = root["relativeTolerance"]
        var conf: InitializationConfig? = null
        try {
            conf = InitializationConfig(parameters, verify, stabilisation, fixedPointIteration, absoluteTolerance, relativeTolerance)
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
        compilationUnit!!.imports = listOf("FMI2", "TypeConverter", "Math", "Logger").map { identifier: String? -> newAIdentifier(identifier) }
        val module = AModuleDeclaration()
        module.name = newAIdentifier(name)
        module.setFunctions(ArrayList(declaredUnfoldFunctions))
        compilationUnit!!.module = module
        return compilationUnit as AImportedModuleCompilationUnit
    }

    class InitializationConfig(parameters: JsonNode?, verify: JsonNode?, stabilisation: JsonNode?, fixedPointIteration: JsonNode?, absoluteTolerance: JsonNode?,
                               relativeTolerance: JsonNode?) : IPluginConfiguration {
        var stabilisation = false
        val modelParameters: List<ModelParameter>?
        var verifyAgainstProlog = false
        var maxIterations = 0
        var absoluteTolerance = 0.0
        var relativeTolerance = 0.0


        init {
            val mapper = ObjectMapper()
            val convertParameters: Map<String, Any>? = if(parameters?.isNull == true)null else mapper.convertValue(parameters, Map::class.java) as Map<String, Any>

            modelParameters = convertParameters?.map { (key, value) -> ModelParameter(ModelConnection.Variable.parse(key), value) }
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

        private fun FindParameterOrDefault(compName: String, sv: ScalarVariable, modelParameters: List<ModelParameter>?): Any {
            val parameterValue = modelParameters!!.stream().filter { x: ModelParameter -> x.variable.instance.instanceName == compName && x.variable.variable == sv.name }
                    .findFirst()
            return if (parameterValue.isPresent) {
                parameterValue.get().value
            } else {
                sv.type.start
            }
        }
    }
}
