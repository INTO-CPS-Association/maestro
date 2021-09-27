package org.intocps.maestro.webapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.intocps.maestro.core.dto.ExtendedMultiModel

data class ExecutionParameters(
    @JsonProperty("convergenceRelativeTolerance") val convergenceRelativeTolerance: Double?,
    @JsonProperty("convergenceAbsoluteTolerance") val convergenceAbsoluteTolerance: Double?,
    @JsonProperty("convergenceAttempts") val convergenceAttempts: Int?,
    @JsonProperty("startTime") val startTime: Double,
    @JsonProperty("endTime") val endTime: Double,
    @JsonProperty("stepSize") val stepSize: Double
)

data class ExecutableMasterAndMultiModelTDO(
    @JsonProperty("masterModel") var masterModel: String,
    @JsonProperty("multiModel") val multiModel: ExtendedMultiModel,
    @JsonProperty("executionParameters") val executionParameters: ExecutionParameters
)

data class VerificationDTO(
    @JsonProperty("verifiedSuccessfully") val verifiedSuccessfully: Boolean,
    @JsonProperty("uppaalModel") val uppaalModel: String,
    @JsonProperty("errorMessage") val errorMessage: String
)