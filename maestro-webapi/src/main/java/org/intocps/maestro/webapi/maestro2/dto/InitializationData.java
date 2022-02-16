package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.intocps.maestro.core.dto.IAlgorithmConfig;
import org.intocps.maestro.core.dto.MultiModel;

import java.util.List;
import java.util.Map;
@JsonIgnoreProperties(ignoreUnknown = true)
public class InitializationData extends MultiModel {

    @JsonProperty("livestream")
    private final Map<String, List<String>> livestream;

    @JsonProperty("hasExternalSignals")
    private final boolean hasExternalSignals;

    @JsonCreator
    public InitializationData(@JsonProperty("fmus") Map<String, String> fmus, @JsonProperty("connections") Map<String, List<String>> connections,
            @JsonProperty("parameters") Map<String, Object> parameters, @JsonProperty("logVariables") Map<String, List<String>> logVariables,
            @JsonProperty("parallelSimulation") boolean parallelSimulation, @JsonProperty("stabalizationEnabled") boolean stabalizationEnabled,
            @JsonProperty("global_absolute_tolerance") double global_absolute_tolerance,
            @JsonProperty("global_relative_tolerance") double global_relative_tolerance, @JsonProperty("loggingOn") boolean loggingOn,
            @JsonProperty("visible") boolean visible, @JsonProperty("simulationProgramDelay") boolean simulationProgramDelay,
            @JsonProperty("algorithm") IAlgorithmConfig algorithm, @JsonProperty("overrideLogLevel") InitializeLogLevel overrideLogLevel,
            @JsonProperty("environmentParameters") List<String> environmentParameters,
            @JsonProperty("logLevels") Map<String, List<String>> logLevels, @JsonProperty("livestream") Map<String, List<String>> livestream,
            @JsonProperty("hasExternalSignals") boolean hasExternalSignals, @JsonProperty("faultInjectConfigurationPath") String faultInjectConfigurationPath,
            @JsonProperty("faultInjectInstances") Map<String, String> faultInjectInstances,
            @JsonProperty("convergenceAttempts") int convergenceAttempts) {
        super(fmus, connections, parameters, logVariables, parallelSimulation, stabalizationEnabled, global_absolute_tolerance,
                global_relative_tolerance, loggingOn, visible, simulationProgramDelay, algorithm, overrideLogLevel, environmentParameters,
                logLevels, faultInjectConfigurationPath, faultInjectInstances, convergenceAttempts);
        this.livestream = livestream;
        this.hasExternalSignals = hasExternalSignals;
    }

    public Map<String, List<String>> getLivestream() {
        return livestream;
    }

    public boolean isHasExternalSignals() {
        return hasExternalSignals;
    }

}
