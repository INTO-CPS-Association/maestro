package org.intocps.maestro.webapi

import core.*
import org.intocps.maestro.fmi.ModelDescription
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration
import org.intocps.maestro.webapi.dto.ScenarioDTO
import org.intocps.maestro.webapi.maestro2.dto.MultiModelScenarioVerifier
import scala.Enumeration
import scala.jdk.javaapi.CollectionConverters
import synthesizer.SynthesizerSimple


class ScenarioModelMapper {
    companion object {
        fun scenarioModelToMasterModel(scenario: ScenarioModel, loopStrategy: Enumeration.Value): MasterModel { //
            SynthesizerSimple(scenario, loopStrategy).apply {
                return MasterModel(
                    "masterModel",
                    scenario,
                    null,
                    this.synthesizeInitialization(),
                    this.synthesizeStep(),
                    null
                )
            }
        }

        fun scenarioDTOToScenarioModel(scenarioDTO: ScenarioDTO): ScenarioModel {
            val connectionModelList = scenarioDTO.connections.map { connection ->
                connection.split(" -> ").let {
                    val sourcePortNameSplit = it[0].split(".")
                    val targetPortNameSplit = it[1].split(".")
                    ConnectionModel(
                        PortRef(
                            sourcePortNameSplit[0],
                            sourcePortNameSplit[1]
                        ),
                        PortRef(
                            targetPortNameSplit[0],
                            targetPortNameSplit[1]
                        )
                    )
                }
            }

            val fmuNameToFmuModel = scenarioDTO.fmus.entries.associate { fmuEntry ->
                val inputs =
                    fmuEntry.value.inputs.entries.associate { entry -> entry.key to InputPortModel(if (entry.value.reactive) Reactivity.reactive() else Reactivity.delayed()) }
                val outputs = fmuEntry.value.outputs.entries.associate { entry ->
                    entry.key to OutputPortModel(
                        CollectionConverters.asScala(entry.value.dependenciesInit).toList(),
                        CollectionConverters.asScala(entry.value.dependencies).toList()
                    )
                }


                fmuEntry.key to FmuModel(
                    scala.collection.immutable.Map.from(scala.jdk.CollectionConverters.MapHasAsScala(inputs).asScala()),
                    scala.collection.immutable.Map.from(scala.jdk.CollectionConverters.MapHasAsScala(outputs).asScala()),
                    fmuEntry.value.canRejectStep
                )
            }

            return ScenarioModel(
                scala.collection.immutable.Map.from(scala.jdk.CollectionConverters.MapHasAsScala(fmuNameToFmuModel).asScala()),
                CollectionConverters.asScala(connectionModelList).toList(),
                scenarioDTO.maxPossibleStepSize
            )
        }

        fun multiModelToScenarioModel(multiModel: MultiModelScenarioVerifier, maxPossibleStepSize: Int): ScenarioModel {
            val connectionModelList = multiModel.connections.entries.map { (key, value) ->
                val sourcePortNameSplit = key.split(".")
                value.map { targetPortName ->
                    val targetPortNameSplit = targetPortName.split(".")
                    ConnectionModel(
                        PortRef(
                            sourcePortNameSplit[0],
                            sourcePortNameSplit[2]
                        ),
                        PortRef(
                            targetPortNameSplit[0],
                            targetPortNameSplit[2]
                        )
                    )
                }
            }.flatten()


            val simulationConfiguration = Fmi2SimulationEnvironmentConfiguration().apply {
                this.fmus = multiModel.fmus;
                this.connections = multiModel.connections;
                if (this.fmus == null) throw Exception("Missing FMUs from multi-model")
            }

            val fmuNameToFmuModel = Fmi2SimulationEnvironment.of(
                simulationConfiguration,
                null
            ).fmusWithModelDescriptions.associate { entry ->
                val scalarVariables = entry.value.scalarVariables
                val inputs =
                    scalarVariables.filter { port -> port.causality.equals(ModelDescription.Causality.Input) }
                        .associate { port ->
                            port.getName() to InputPortModel(if (multiModel.reactivity.any { portReactivity ->
                                    portReactivity.key.contains(port.getName()) &&
                                            portReactivity.key.contains(entry.key) &&
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

                entry.key to FmuModel(
                    scala.collection.immutable.Map.from(scala.jdk.CollectionConverters.MapHasAsScala(inputs).asScala()),
                    scala.collection.immutable.Map.from(scala.jdk.CollectionConverters.MapHasAsScala(outputs).asScala()),
                    entry.value.canGetAndSetFmustate
                )
            }

            return ScenarioModel(
                scala.collection.immutable.Map.from(scala.jdk.CollectionConverters.MapHasAsScala(fmuNameToFmuModel).asScala()),
                CollectionConverters.asScala(connectionModelList).toList(),
                maxPossibleStepSize
            )
        }
    }
}