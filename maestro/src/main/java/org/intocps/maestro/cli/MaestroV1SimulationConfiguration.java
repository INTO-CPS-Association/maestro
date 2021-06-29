package org.intocps.maestro.cli;

import com.fasterxml.jackson.annotation.*;
import org.intocps.maestro.core.dto.IAlgorithmConfig;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;

import java.util.List;
import java.util.Map;

public class MaestroV1SimulationConfiguration extends Fmi2SimulationEnvironmentConfiguration {
    @JsonProperty("parameters")
    public Map<String, Object> parameters;
    /**
     * Named list of parameter names as in {@link #parameters}
     */
    @JsonProperty("environmentParameters")
    public List<String> environmentParameters;
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
}
