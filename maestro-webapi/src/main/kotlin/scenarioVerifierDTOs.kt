package org.intocps.maestro.webapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.intocps.maestro.core.dto.MultiModelScenarioVerifier

data class FmuDTO(
    @JsonProperty("can-reject-step") val canRejectStep: Boolean,
    @JsonProperty("inputs") val inputs: Map<String, InputDTO>,
    @JsonProperty("outputs") val outputs: Map<String, OutputDTO>
)

data class InputDTO(@JsonProperty("reactivity") val reactive: Boolean)

data class OutputDTO(
    @JsonProperty("dependencies-init") val dependenciesInit: List<String>,
    @JsonProperty("dependencies") val dependencies: List<String>
)

data class ScenarioDTO(
    @JsonProperty("fmus") val fmus: Map<String, FmuDTO>,
    @JsonProperty("connections") val connections: List<String>,
    @JsonProperty("maxPossibleStepSize") val maxPossibleStepSize: Int
)

data class MasterModelDTO(
    @JsonProperty("name") val name: String?,
    @JsonProperty("scenario") val scenario: ScenarioDTO,
    @JsonProperty("instantiation") val instantiation: String?,
    @JsonProperty("initialization") val initialization: String?,
    @JsonProperty("coSimStep") val coSimStep: String,
    @JsonProperty("terminate") val terminate: String?,
)

data class ExecutionParameters(
    @JsonProperty("convergenceRelativeTolerance") val convergenceRelativeTolerance: Double?,
    @JsonProperty("convergenceAbsoluteTolerance") val convergenceAbsoluteTolerance: Double?,
    @JsonProperty("convergenceAttempts") val convergenceAttempts: Int?,
    @JsonProperty("startTime") val startTime: Double,
    @JsonProperty("endTime") val endTime: Double,
    @JsonProperty("stepSize") val stepSize: Double
)

data class MasterMultiModelDTO(
    @JsonProperty("masterModel") val masterModel: String,
    @JsonProperty("multiModel") val multiModel: MultiModelScenarioVerifier
)

data class ExecutableMasterAndMultiModelTDO(
    @JsonProperty("masterModel") val masterModel: String,
    @JsonProperty("multiModel") val multiModel: MultiModelScenarioVerifier,
    @JsonProperty("executionParameters") val executionParameters: ExecutionParameters
)

data class VerificationDTO(@JsonProperty("verifiedSuccessfully") val verifiedSuccessfully: Boolean, @JsonProperty("detailsMessage") val detailsMessage: String, @JsonProperty("UppaalModel") val uppaalModel: String?)