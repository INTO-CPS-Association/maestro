package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Times and logging settings for co-simulation of an initialized session.")
public class SimulateRequestBody extends BaseSimulateRequestBody {
    @Schema(description = "Enabled FMI logging categories by instance, using keys such as {FMU}.instanceName.")
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
