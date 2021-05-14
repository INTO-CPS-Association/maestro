package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScenarioVerifierInitializationData extends InitializationData {

    @JsonProperty("reactivity")
    final Map<String, Boolean> reactivity;

    public ScenarioVerifierInitializationData(Map<String, String> fmus, Map<String, List<String>> connections, Map<String, Object> parameters,
            Map<String, List<String>> livestream, Map<String, List<String>> logVariables, boolean parallelSimulation, boolean stabalizationEnabled,
            double global_absolute_tolerance, double global_relative_tolerance, boolean loggingOn, boolean visible, boolean simulationProgramDelay,
            boolean hasExternalSignals, IAlgorithmConfig algorithm, InitializeLogLevel overrideLogLevel, Object liveGraphColumns,
            Object liveGraphVisibleRowCount, Object livestreamInterval, Map<String, Boolean> reactivity) {
        super(fmus, connections, parameters, livestream, logVariables, parallelSimulation, stabalizationEnabled, global_absolute_tolerance,
                global_relative_tolerance, loggingOn, visible, simulationProgramDelay, hasExternalSignals, algorithm, overrideLogLevel,
                liveGraphColumns, liveGraphVisibleRowCount, livestreamInterval);
        this.reactivity = reactivity;
    }

    public Map<String, Boolean> getReactivity() {
        return this.reactivity;
    }
}
