package org.intocps.maestro.core.dto;

import com.fasterxml.jackson.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simulation step algorithm. Select fixed-step or var-step using the type property.")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = FixedStepAlgorithmConfig.class, name = "fixed-step"),
        @JsonSubTypes.Type(value = VariableStepAlgorithmConfig.class, name = "var-step")})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public interface IAlgorithmConfig {
    @JsonIgnore
    StepAlgorithm getAlgorithmType();
    @JsonIgnore
    double getStepSize();
}
