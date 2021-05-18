package org.intocps.maestro.webapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.intocps.maestro.webapi.maestro2.dto.MultiModelScenarioVerifier

data class FmuDTO(
    @JsonProperty("canRejectStep") val canRejectStep: Boolean,
    @JsonProperty("inputs") val inputs: Map<String, InputDTO>,
    @JsonProperty("outputs") val outputs: Map<String, OutputDTO>
)

data class InputDTO(@JsonProperty("reactive") val reactive: Boolean)

data class OutputDTO(@JsonProperty("dependenciesInit") val dependenciesInit: List<String>, @JsonProperty("dependencies") val dependencies: List<String>)

data class ScenarioDTO(@JsonProperty("fmus") val fmus: Map<String, FmuDTO>, @JsonProperty("connections") val connections: List<String>, @JsonProperty("maxPossibleStepSize") val maxPossibleStepSize: Int)

data class MasterModelDTO(
    @JsonProperty("scenario") val scenario: ScenarioDTO?,
    @JsonProperty("instantiation") val instantiation: List<String>?,
    @JsonProperty("initialization") val initialization: List<String>,
    @JsonProperty("coSimStep") val coSimStep: List<String>,
    @JsonProperty("terminate") val terminate: List<String>?
)

data class MasterModelWithMultiModelDTO(@JsonProperty("masterModel") val masterModel: MasterModelDTO, @JsonProperty("multiModel") val multiModel: MultiModelScenarioVerifier)