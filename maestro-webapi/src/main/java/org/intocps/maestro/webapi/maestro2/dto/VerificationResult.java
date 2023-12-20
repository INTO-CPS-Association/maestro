package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VerificationResult {
    @JsonProperty("correct")
    public Boolean correct;
    @JsonProperty("masterModel")
    public String masterModel;
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
