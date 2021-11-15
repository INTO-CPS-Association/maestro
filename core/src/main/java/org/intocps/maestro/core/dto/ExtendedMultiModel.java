package org.intocps.maestro.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtendedMultiModel extends MultiModel {
    @JsonProperty("sigver")
    public final Sigver sigver;



    @JsonCreator
    public ExtendedMultiModel(@JsonProperty("fmus") Map<String, String> fmus,
            @JsonProperty("connections") Map<String, List<String>> connections, @JsonProperty("parameters") Map<String, Object> parameters,
            @JsonProperty("logVariables") Map<String, List<String>> logVariables, @JsonProperty("parallelSimulation") boolean parallelSimulation,
            @JsonProperty("stabalizationEnabled") boolean stabalizationEnabled,
            @JsonProperty("global_absolute_tolerance") double global_absolute_tolerance,
            @JsonProperty("global_relative_tolerance") double global_relative_tolerance, @JsonProperty("loggingOn") boolean loggingOn,
            @JsonProperty("visible") boolean visible, @JsonProperty("simulationProgramDelay") boolean simulationProgramDelay,
            @JsonProperty("algorithm") IAlgorithmConfig algorithm, @JsonProperty("overrideLogLevel") InitializeLogLevel overrideLogLevel,
            @JsonProperty("environmentParameters") List<String> environmentParameters, @JsonProperty("logLevels") Map<String, List<String>> logLevels,
            @JsonProperty("sigver") Sigver sigver, @JsonProperty("faultInjectConfigurationPath") String faultInjectConfigurationPath,
            @JsonProperty("faultInjectInstances") Map<String, String> faultInjectInstances) {
        super(fmus, connections, parameters, logVariables, parallelSimulation, stabalizationEnabled, global_absolute_tolerance,
                global_relative_tolerance, loggingOn, visible, simulationProgramDelay, algorithm, overrideLogLevel, environmentParameters,
                logLevels, faultInjectConfigurationPath, faultInjectInstances);
        this.sigver = sigver;
    }

    public static class Sigver {
        @JsonProperty("reactivity")
        public final Map<String, Reactivity> reactivity;

        @JsonProperty("verification")
        public final boolean verification;

        public Sigver(@JsonProperty("reactivity") Map<String, Reactivity> reactivity, @JsonProperty("verification") boolean verification) {
            this.reactivity = reactivity;
            this.verification = verification;
        }

        public enum Reactivity{
            Reactive,
            Delayed
        }
    }
}
