package org.intocps.maestro.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fixed-step algorithm. Set type to fixed-step.")
public class FixedStepAlgorithmConfig implements IAlgorithmConfig {
    @JsonProperty("size")
    @JsonPropertyDescription("Fixed communication step size in simulation time units.")
    @Schema(description = "Fixed communication step size in simulation time units.", example = "0.1")
    public final Double size;

    @JsonCreator
    public FixedStepAlgorithmConfig(@JsonProperty("size") Double size) {
        this.size = size;
    }

    public Double getSize() {
        return size;
    }

    @Override
    public StepAlgorithm getAlgorithmType() {
        return StepAlgorithm.FIXEDSTEP;
    }

    @Override
    public double getStepSize() {
        return getSize();
    }
}
