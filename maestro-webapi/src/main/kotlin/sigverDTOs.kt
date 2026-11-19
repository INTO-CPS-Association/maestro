package org.intocps.maestro.webapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.intocps.maestro.core.dto.ExtendedMultiModel

@Schema(description = "Execution settings accompanying a scenario multi-model.")
data class ExecutionParameters(
    @field:Schema(description = "Relative convergence tolerance requested by the client.")
    @JsonProperty("convergenceRelativeTolerance") val convergenceRelativeTolerance: Double?,
    @field:Schema(description = "Absolute convergence tolerance requested by the client.")
    @JsonProperty("convergenceAbsoluteTolerance") val convergenceAbsoluteTolerance: Double?,
    @field:Schema(description = "Requested maximum convergence attempts.")
    @JsonProperty("convergenceAttempts") val convergenceAttempts: Int?,
    @field:Schema(description = "Simulation start time.")
    @JsonProperty("startTime") val startTime: Double,
    @field:Schema(description = "Simulation end time.")
    @JsonProperty("endTime") val endTime: Double,
    @field:Schema(description = "Requested communication step size.")
    @JsonProperty("stepSize") val stepSize: Double,
    @field:Schema(description = "Requested simulation-time interval for live logging.")
    @JsonProperty("liveLogInterval") val liveLogInterval: Double
)

@Schema(description = "Multi-model, master model, and times for sessionless algorithm execution. " +
    "The current endpoint uses startTime and endTime; other executionParameters do not override the multi-model algorithm settings.")
data class ExecutableMasterAndMultiModelTDO(
    @field:Schema(description = "Master-model configuration text. Use an empty string to generate an algorithm from the multi-model.")
    @JsonProperty("masterModel") var masterModel: String,
    @field:Schema(description = "Extended multi-model containing FMUs, connections, and scenario-verification options.")
    @JsonProperty("multiModel") val multiModel: ExtendedMultiModel,
    @field:Schema(description = "Execution times and associated options; see the endpoint description for supported fields.")
    @JsonProperty("executionParameters") val executionParameters: ExecutionParameters
)

data class VerificationDTO(
    @JsonProperty("verifiedSuccessfully") val verifiedSuccessfully: Boolean,
    @JsonProperty("uppaalModel") val uppaalModel: String,
    @JsonProperty("errorMessage") val errorMessage: String
)