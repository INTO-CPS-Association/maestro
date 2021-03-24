package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;

@ApiModel(subTypes = {InitializationData.BoundedDifferenceConstraint.class, InitializationData.ZeroCrossingConstraint.class,
        InitializationData.SamplingConstraint.class, InitializationData.FmuMaxStepSizeConstraint.class}, discriminator = "type",
        description = "Simulation variable step algorithm constraint.", value = "VarStepConstraint")

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY, visible = true)
@JsonSubTypes({@JsonSubTypes.Type(value = InitializationData.BoundedDifferenceConstraint.class, name = "boundeddifference"),
        @JsonSubTypes.Type(value = InitializationData.ZeroCrossingConstraint.class, name = "zerocrossing"),
        @JsonSubTypes.Type(value = InitializationData.SamplingConstraint.class, name = "samplingrate"),
        @JsonSubTypes.Type(value = InitializationData.FmuMaxStepSizeConstraint.class, name = "fmumaxstepsize")})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties("type")
public interface IVarStepConstraint {

    void validate() throws Exception;
}
