package org.intocps.maestro.webapi.maestro2.dto;

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
}
