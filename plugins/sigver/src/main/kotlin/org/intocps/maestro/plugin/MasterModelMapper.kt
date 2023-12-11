package org.intocps.maestro.plugin

import org.intocps.maestro.core.dto.ExtendedMultiModel
import org.intocps.maestro.fmi.Fmi2ModelDescription
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration
import scala.jdk.javaapi.CollectionConverters
import org.intocps.verification.scenarioverifier.api.GenerationAPI
import org.intocps.verification.scenarioverifier.core.*

class MasterModelMapper {
    companion object {

        private fun getFmuNameFromFmuInstanceName(name: String): String {
            return name.split(Sigver.MASTER_MODEL_FMU_INSTANCE_DELIMITER)[0]
        }

        private fun getInstanceNameFromFmuInstanceName(name: String): String {
            return name.split(Sigver.MASTER_MODEL_FMU_INSTANCE_DELIMITER)[1]
        }

        private fun multiModelConnectionNameToMasterModelInstanceName(multiModelName: String): String {
            val multiModelNameSplit = multiModelName.split(Sigver.MULTI_MODEL_FMU_INSTANCE_DELIMITER)
            return multiModelNameSplit[0].replace("[{}]".toRegex(), "").plus(Sigver.MASTER_MODEL_FMU_INSTANCE_DELIMITER)
                .plus(multiModelNameSplit[1])
        }

        private fun multiModelConnectionNameToPortName(name: String): String {
            return name.split(Sigver.MULTI_MODEL_FMU_INSTANCE_DELIMITER).let { it.subList(2, it.size).joinToString(Sigver.MULTI_MODEL_FMU_INSTANCE_DELIMITER) }
        }

        fun scenarioToMasterModel(scenario: String): MasterModel {
            // Load master model without algorithm
            val masterModel = ScenarioLoader.load(scenario.byteInputStream())
            return GenerationAPI.synthesizeAlgorithm(masterModel.name(), masterModel.scenario())
        }

        fun masterModelConnectionsToMultiModelConnections(masterModel: MasterModel): HashMap<String, MutableList<String>> {
            // Setup connections as defined in the scenario (These should be identical to the multi-model)
            return CollectionConverters.asJava(masterModel.scenario().connections())
                .fold(HashMap()) { consMap, connection ->
                    val targetFmuAndInstance =
                        connection.trgPort().fmu().split(Sigver.MASTER_MODEL_FMU_INSTANCE_DELIMITER).toTypedArray()
                    val targetFmuName = targetFmuAndInstance[0]
                    val targetInstanceName = targetFmuAndInstance[1]
                    val sourceFmuAndInstance =
                        connection.srcPort().fmu().split(Sigver.MASTER_MODEL_FMU_INSTANCE_DELIMITER).toTypedArray()
                    val sourceFmuName = sourceFmuAndInstance[0]
                    val sourceInstanceName = sourceFmuAndInstance[1]
                    val muModelTrgName =
                        "{" + targetFmuName + "}" + Sigver.MULTI_MODEL_FMU_INSTANCE_DELIMITER + targetInstanceName +
                                Sigver.MULTI_MODEL_FMU_INSTANCE_DELIMITER + connection.trgPort().port()
                    val muModelSrcName =
                        "{" + sourceFmuName + "}" + Sigver.MULTI_MODEL_FMU_INSTANCE_DELIMITER + sourceInstanceName +
                                Sigver.MULTI_MODEL_FMU_INSTANCE_DELIMITER + connection.srcPort().port()
                    consMap.containsKey(muModelSrcName).also { isPresent ->
                        if (isPresent) consMap[muModelSrcName]!!.add(muModelTrgName) else consMap[muModelSrcName] =
                            ArrayList(listOf(muModelTrgName))
                    }
                    return@fold consMap
                }
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
            val simulationConfiguration = Fmi2SimulationEnvironmentConfiguration(extendedMultiModel.connections, extendedMultiModel.fmus)

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
                                                    portReactivity.key.contains(
                                                        getFmuNameFromFmuInstanceName(
                                                            fmuInstanceName
                                                        )
                                                    ) &&
                                                    portReactivity.key.contains(
                                                        getInstanceNameFromFmuInstanceName(
                                                            fmuInstanceName
                                                        )
                                                    ) &&
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

            print("Hello from multiModelToMasterModel!")
            print("Scenario model: ${scenarioModel.toConf(0)}")

            // Generate the master model from the scenario
            return GenerationAPI.synthesizeAlgorithm("generatedFromMultiModel", scenarioModel)
        }
    }
}