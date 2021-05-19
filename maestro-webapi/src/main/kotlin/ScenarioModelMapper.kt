package org.intocps.maestro.webapi

import core.*
import org.intocps.maestro.fmi.ModelDescription
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration
import org.intocps.maestro.webapi.maestro2.dto.MultiModelScenarioVerifier
import scala.jdk.javaapi.CollectionConverters
import synthesizer.LoopStrategy
import synthesizer.SynthesizerSimple

class ScenarioModelMapper {
    companion object {
        private fun removeBraces(value: String): String {
            return value.replace("[{}]".toRegex(), "")
        }

        private val loopStrategy = LoopStrategy.maximum()

        fun scenarioToMasterModel(scenario: String): MasterModel {
            val masterModel = ScenarioLoader.load(scenario.byteInputStream())
            val synthesizer = SynthesizerSimple(masterModel.scenario(), loopStrategy)
            val initialization = synthesizer.synthesizeInitialization()
            val cosimStep = synthesizer.synthesizeStep()

            return MasterModel(
                masterModel.name(),
                masterModel.scenario(),
                masterModel.instantiation(),
                initialization,
                cosimStep,
                masterModel.terminate()
            )

        }

        fun multiModelToMasterModel(multiModel: MultiModelScenarioVerifier, maxPossibleStepSize: Int): MasterModel {
            val connectionModelList = multiModel.connections.entries.map { (key, value) ->
                val sourcePortNameSplit = key.split(".")
                value.map { targetPortName ->
                    val targetPortNameSplit = targetPortName.split(".")
                    ConnectionModel(
                        PortRef(
                            removeBraces(sourcePortNameSplit[0]),
                            sourcePortNameSplit[2]
                        ),
                        PortRef(
                            removeBraces(targetPortNameSplit[0]),
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
                            port.getName() to InputPortModel(if (multiModel.scenarioVerifier.reactivity.any { portReactivity ->
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

                removeBraces(entry.key) to FmuModel(
                    scala.collection.immutable.Map.from(scala.jdk.CollectionConverters.MapHasAsScala(inputs).asScala()),
                    scala.collection.immutable.Map.from(
                        scala.jdk.CollectionConverters.MapHasAsScala(outputs).asScala()
                    ),
                    entry.value.canGetAndSetFmustate
                )
            }

            val scenario = ScenarioModel(
                scala.collection.immutable.Map.from(
                    scala.jdk.CollectionConverters.MapHasAsScala(fmuNameToFmuModel).asScala()
                ),
                CollectionConverters.asScala(connectionModelList).toList(),
                maxPossibleStepSize
            )

            val masterModel = ScenarioLoader.load(scenario.toString().byteInputStream())
            val synthesizer = SynthesizerSimple(masterModel.scenario(), loopStrategy)

            return MasterModel(
                masterModel.name(),
                masterModel.scenario(),
                masterModel.instantiation(),
                synthesizer.synthesizeInitialization(),
                synthesizer.synthesizeStep(),
                masterModel.terminate()
            )
        }
    }
}