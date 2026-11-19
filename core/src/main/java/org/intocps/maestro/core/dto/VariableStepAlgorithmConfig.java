package org.intocps.maestro.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Variable-step algorithm. Set type to var-step.")
public class VariableStepAlgorithmConfig implements IAlgorithmConfig {

    @JsonProperty("size")
    @JsonPropertyDescription("Minimum and maximum communication step sizes, in that order.")
    @Schema(description = "Minimum and maximum communication step sizes, in that order.")
    final Double[] size;
    @JsonProperty("initsize")
    @JsonPropertyDescription("Initial communication step size in simulation time units.")
    @Schema(description = "Initial communication step size in simulation time units.")
    final Double initsize;
    @JsonProperty("constraints")
    @JsonPropertyDescription("Named constraints used to select communication step sizes.")
    @Schema(description = "Named constraints used to select communication step sizes.")
    final Map<String, VarStepConstraint> constraints;

    public VariableStepAlgorithmConfig(@JsonProperty("size") Double[] size, @JsonProperty("initsize") Double initsize,
            @JsonProperty("constraints") final Map<String, VarStepConstraint> constraints) {
        this.size = size;
        this.initsize = initsize;
        this.constraints = constraints;
    }

    public Double[] getSize() {
        return size;
    }

    public Double getInitsize() {
        return initsize;
    }

    public Map<String, VarStepConstraint> getConstraints() {
        return constraints;
    }

    @Override
    public StepAlgorithm getAlgorithmType() {
        return StepAlgorithm.VARIABLESTEP;
    }

    @Override
    public double getStepSize() {
        return getInitsize();
    }
}
