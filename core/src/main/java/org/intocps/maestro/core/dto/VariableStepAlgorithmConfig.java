package org.intocps.maestro.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

import java.util.Map;

@ApiModel(parent = IAlgorithmConfig.class)
public class VariableStepAlgorithmConfig implements IAlgorithmConfig {

    @JsonProperty("size")
    final Double[] size;
    @JsonProperty("initsize")
    final Double initsize;
    @JsonProperty("constraints")
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
