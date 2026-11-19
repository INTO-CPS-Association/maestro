package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Algorithm verification outcome. Inspect correct and errorMessage even when HTTP status is 200.")
public class VerificationResult {
    @Schema(description = "True only when verification succeeded.")
    @JsonProperty("correct")
    public Boolean correct;
    @Schema(description = "Generated UPPAAL XML encoding, or an empty string when unavailable.")
    @JsonProperty("masterModel")
    public String masterModel;
    @Schema(description = "Verification failure or missing-tool message; empty on success.")
    @JsonProperty("errorMessage")
    public String errorMessage;

    public VerificationResult() {
    }

    public VerificationResult(Boolean correct, String masterModel, String errorMessage) {
        this.correct = correct;
        this.masterModel = masterModel;
        this.errorMessage = errorMessage;
    }
}
