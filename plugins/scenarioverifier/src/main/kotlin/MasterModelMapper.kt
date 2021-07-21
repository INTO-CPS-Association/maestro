package org.intocps.maestro.plugin

import core.*
import org.intocps.maestro.fmi.ModelDescription
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration
import org.intocps.maestro.core.dto.MultiModelScenarioVerifier
import scala.jdk.javaapi.CollectionConverters
import synthesizer.LoopStrategy
import synthesizer.SynthesizerSimple

class MasterModelMapper {
    companion object {

        private val loopStrategy = LoopStrategy.maximum()

        private const val INSTANCE_DELIMITER = "_"

        private fun getFmuNameFromFmuInstanceName(name: String): String {
            return name.split(INSTANCE_DELIMITER)[0]
        }

        private fun getInstanceNameFromFmuInstanceName(name: String): String {
            return name.split(INSTANCE_DELIMITER)[1]
        }

        private fun multiModelConnectionNameToMasterModelInstanceName(multiModelName: String): String {
            val multiModelNameSplit = multiModelName.split(".")
            return multiModelNameSplit[0].replace("[{}]".toRegex(), "").plus(INSTANCE_DELIMITER).plus(multiModelNameSplit[1])
        }

        private fun multiModelConnectionNameToPortName(name: String): String {
            return name.split(".")[2]
        }

        fun scenarioToMasterModel(scenario: String): MasterModel {
            // Load master model without algorithm
            val masterModel = ScenarioLoader.load(scenario.byteInputStream())

            // Generate algorithm part of the master model
            val synthesizer = SynthesizerSimple(masterModel.scenario(), loopStrategy)
            val initialization = synthesizer.synthesizeInitialization()
            val coSimStep = synthesizer.synthesizeStep()

            return MasterModel(
                masterModel.name(),
                masterModel.scenario(),
                masterModel.instantiation(),
                initialization,
                coSimStep,
                masterModel.terminate()
            )

        }

        fun multiModelToMasterModel(multiModel: MultiModelScenarioVerifier, maxPossibleStepSize: Int): MasterModel {
            // Map multi model connections type to scenario connections type
            val connectionModelList = multiModel.connections.entries.map { (key, value) ->
                value.map { targetPortName ->
                    ConnectionModel(
                        PortRef(
                            multiModelConnectionNameToMasterModelInstanceName(key), // Fully qualify the fmu instance with <fmu-name>_<instance-name>
                            multiModelConnectionNameToPortName(key)
                        ),
                        PortRef(
                            multiModelConnectionNameToMasterModelInstanceName(targetPortName), // Fully qualify the fmu instance with <fmu-name>_<instance-name>
                            multiModelConnectionNameToPortName(targetPortName)
                        )
                    )
                }
            }.flatten()

            // Instantiate a simulationConfiguration to be able to access fmu model descriptions
            val simulationConfiguration = Fmi2SimulationEnvironmentConfiguration().apply {
                this.fmus = multiModel.fmus;
                this.connections = multiModel.connections;
                if (this.fmus == null) throw Exception("Missing FMUs from multi-model")
            }

            val fmuInstanceNamesFromConnections = connectionModelList.map { connectionModel ->
                listOf(
                    connectionModel.srcPort().fmu(),
                    connectionModel.trgPort().fmu()
                )
            }.flatten().distinct()


            // Map fmus to fmu models
            val fmusWithModelDescriptions =
                Fmi2SimulationEnvironment.of(simulationConfiguration, null).fmusWithModelDescriptions
            val fmuNameToFmuModel = fmuInstanceNamesFromConnections.mapNotNull { fmuInstanceName ->
                fmusWithModelDescriptions.find { it.key.contains(getFmuNameFromFmuInstanceName(fmuInstanceName)) }?.let { fmuWithMD ->
                    val scalarVariables = fmuWithMD.value.scalarVariables
                    val inputs =
                        scalarVariables.filter { port -> port.causality.equals(ModelDescription.Causality.Input) }
                            .associate { port ->
                                port.getName() to InputPortModel(if (multiModel.scenarioVerifier.reactivity.any { portReactivity ->
                                        portReactivity.key.contains(port.getName()) &&
                                                portReactivity.key.contains(getFmuNameFromFmuInstanceName(fmuInstanceName)) &&
                                                portReactivity.key.contains(getInstanceNameFromFmuInstanceName(fmuInstanceName)) &&
                                                portReactivity.value
                                    }) Reactivity.reactive() else Reactivity.delayed())
                            }

                    val outputs =
                        scalarVariables.filter { port -> port.causality.equals(ModelDescription.Causality.Output) }
                            .associate { port ->
                                val dependencies = port.outputDependencies.map { entry -> entry.key.getName() }
                                port.getName() to OutputPortModel(
                                    CollectionConverters.asScala(dependencies).toList(),
                                    CollectionConverters.asScala(dependencies).toList()
                                )
                            }

                    fmuInstanceName to FmuModel(
                        scala.collection.immutable.Map.from(
                            scala.jdk.CollectionConverters.MapHasAsScala(inputs).asScala()
                        ),
                        scala.collection.immutable.Map.from(
                            scala.jdk.CollectionConverters.MapHasAsScala(outputs).asScala()
                        ),
                        fmuWithMD.value.canGetAndSetFmustate
                    )
                }
            }.toMap()

            val scenario = ScenarioModel(
                scala.collection.immutable.Map.from(
                    scala.jdk.CollectionConverters.MapHasAsScala(fmuNameToFmuModel).asScala()
                ),
                CollectionConverters.asScala(connectionModelList).toList(),
                maxPossibleStepSize
            )

            // Generate the master model from the scenario
            val synthesizer = SynthesizerSimple(scenario, loopStrategy)
            return MasterModel(
                "fromMultiModel",
                scenario,
                CollectionConverters.asScala(listOf<InstantiationInstruction>()).toList(),
                synthesizer.synthesizeInitialization(),
                synthesizer.synthesizeStep(),
                CollectionConverters.asScala(listOf<TerminationInstruction>()).toList()
            )
        }
    }
}