package org.intocps.maestro.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

@ApiModel(parent = IAlgorithmConfig.class)
public class FixedStepAlgorithmConfig implements IAlgorithmConfig {
    @JsonProperty("size")
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
