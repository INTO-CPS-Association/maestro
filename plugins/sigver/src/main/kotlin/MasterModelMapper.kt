package org.intocps.maestro.plugin

import api.GenerationAPI
import core.*
import org.intocps.maestro.core.dto.ExtendedMultiModel
import org.intocps.maestro.fmi.Fmi2ModelDescription
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration
import scala.jdk.javaapi.CollectionConverters

class MasterModelMapper {
    companion object {

        private const val MASTER_MODEL_INSTANCE_DELIMITER = "_"

        private fun getFmuNameFromFmuInstanceName(name: String): String {
            return name.split(MASTER_MODEL_INSTANCE_DELIMITER)[0]
        }

        private fun getInstanceNameFromFmuInstanceName(name: String): String {
            return name.split(MASTER_MODEL_INSTANCE_DELIMITER)[1]
        }

        private fun multiModelConnectionNameToMasterModelInstanceName(multiModelName: String): String {
            val multiModelNameSplit = multiModelName.split(".")
            return multiModelNameSplit[0].replace("[{}]".toRegex(), "").plus(MASTER_MODEL_INSTANCE_DELIMITER)
                .plus(multiModelNameSplit[1])
        }

        private fun multiModelConnectionNameToPortName(name: String): String {
            return name.split(".")[2]
        }

        fun scenarioToMasterModel(scenario: String): MasterModel {
            // Load master model without algorithm
            val masterModel = ScenarioLoader.load(scenario.byteInputStream())
            return GenerationAPI.generateAlgorithm(masterModel.name(), masterModel.scenario())
        }

        fun multiModelToMasterModel(extendedMultiModel: ExtendedMultiModel, maxPossibleStepSize: Int): MasterModel {
            // Map multi model connections type to scenario connections type
            val connectionModelList = extendedMultiModel.connections.entries.map { (key, value) ->
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
                this.fmus = extendedMultiModel.fmus;
                this.connections = extendedMultiModel.connections;
                if (this.fmus == null) throw Exception("Missing FMUs from multi-model")
            }

            // Map fmus to fmu models
            val simulationEnvironment = Fmi2SimulationEnvironment.of(simulationConfiguration, null)
            val fmusWithModelDescriptions = simulationEnvironment.fmusWithModelDescriptions
            val fmuInstanceNames = connectionModelList.map { connectionModel ->
                listOf(
                    connectionModel.srcPort().fmu(),
                    connectionModel.trgPort().fmu()
                )
            }.flatten().distinct()
            val fmuNameToFmuModel = fmuInstanceNames.mapNotNull { fmuInstanceName ->
                fmusWithModelDescriptions.find { it.key.contains(getFmuNameFromFmuInstanceName(fmuInstanceName)) }
                    ?.let { fmuWithMD ->
                        val scalarVariables = fmuWithMD.value.scalarVariables
                        val inputs =
                            scalarVariables.filter { port -> port.causality.equals(Fmi2ModelDescription.Causality.Input) }
                                .associate { inputPort ->
                                    inputPort.getName() to InputPortModel(if (extendedMultiModel.sigver.reactivity.any { portReactivity ->
                                            portReactivity.key.contains(inputPort.getName()) &&
                                            portReactivity.key.contains(getFmuNameFromFmuInstanceName(fmuInstanceName)) &&
                                            portReactivity.key.contains(getInstanceNameFromFmuInstanceName(fmuInstanceName)) &&
                                            portReactivity.value == ExtendedMultiModel.Sigver.Reactivity.Reactive
                                        }) Reactivity.reactive() else Reactivity.delayed())
                                }

                        val outputs =
                            scalarVariables.filter { port ->
                                port.causality.equals(Fmi2ModelDescription.Causality.Output) && connectionModelList.find { connectionModel ->
                                    connectionModel.srcPort().fmu().equals(fmuInstanceName) && connectionModel.srcPort()
                                        .port().equals(port.getName())
                                } != null
                            }.associate { outputPort ->
                                val dependencies =
                                    outputPort.outputDependencies.map { entry -> entry.key.getName() }
                                // This might need to change later
                                outputPort.getName() to OutputPortModel(
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
                            fmuWithMD.value.getCanGetAndSetFmustate(),
                            simulationEnvironment.getUriFromFMUName(fmuWithMD.key).path
                        )
                    }
            }.toMap()

            val scenarioModel = ScenarioModel(
                scala.collection.immutable.Map.from(
                    scala.jdk.CollectionConverters.MapHasAsScala(fmuNameToFmuModel).asScala()
                ),
                AdaptiveModel(
                    CollectionConverters.asScala(listOf<PortRef>()).toList(), scala.collection.immutable.Map.from(
                        scala.jdk.CollectionConverters.MapHasAsScala(mapOf<String, ConfigurationModel>()).asScala()
                    )
                ),
                CollectionConverters.asScala(connectionModelList).toList(),
                maxPossibleStepSize
            )

            // Generate the master model from the scenario
            return GenerationAPI.generateAlgorithm("generatedFromMultiModel", scenarioModel)
        }
    }
}