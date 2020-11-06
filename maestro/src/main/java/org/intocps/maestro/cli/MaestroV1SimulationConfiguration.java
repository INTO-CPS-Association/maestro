package org.intocps.maestro.cli;

import com.fasterxml.jackson.annotation.*;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;

import java.util.List;
import java.util.Map;

public class MaestroV1SimulationConfiguration extends Fmi2SimulationEnvironmentConfiguration {
    @JsonProperty("parameters")
    public Map<String, Object> parameters;

    @JsonProperty("startTime")
    public double startTime;
    @JsonProperty("endTime")
    public double endTime;
    @JsonProperty("logLevels")
    public Map<String, List<String>> logLevels;
    @JsonProperty("reportProgress")
    public Boolean reportProgress;
    @JsonProperty("loggingOn")
    public boolean loggingOn;
    @JsonProperty("visible")
    public boolean visible;

    @JsonProperty("algorithm")
    public IAlgorithmConfig algorithm;


    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({@JsonSubTypes.Type(value = FixedStepAlgorithmConfig.class, name = "fixed-step")})
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public interface IAlgorithmConfig {
    }

    public static class FixedStepAlgorithmConfig implements IAlgorithmConfig {
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
}
