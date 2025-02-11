package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class SimulateRequestBody extends BaseSimulateRequestBody {
    @JsonProperty("logLevels")
    final Map<String, List<String>> logLevels;

    @JsonCreator
    public SimulateRequestBody(@JsonProperty("startTime") double startTime, @JsonProperty("endTime") double endTime,
            @JsonProperty("logLevels") Map<String, List<String>> logLevels, @JsonProperty("reportProgress") Boolean reportProgress,
            @JsonProperty("liveLogInterval") Double liveLogInterval, @JsonProperty("masterModel") String masterModel) {
        super(startTime, endTime, logLevels, reportProgress, liveLogInterval, masterModel);

        this.logLevels = logLevels;
    }

    public Map<String, List<String>> getLogLevels() {
        return logLevels;
    }
}
