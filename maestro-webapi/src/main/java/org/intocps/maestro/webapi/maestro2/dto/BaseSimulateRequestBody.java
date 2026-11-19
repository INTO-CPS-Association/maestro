package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Execution times and logging options shared by simulation requests.")
public class BaseSimulateRequestBody {
    @Schema(description = "Start time of the co-simulation in simulation time units.", example = "0")
    @JsonProperty("startTime")
    final double startTime;
    @Schema(description = "End time of the co-simulation in simulation time units.", example = "10")
    @JsonProperty("endTime")
    final double endTime;
    @Schema(description = "Enabled FMI logging categories by instance, using keys such as {FMU}.instanceName.")
    @JsonProperty("logLevels")
    final Map<String, List<String>> logLevels;
    @Schema(description = "Request progress reporting during execution.")
    @JsonProperty("reportProgress")
    final Boolean reportProgress;
    @Schema(description = "Simulation-time interval between WebSocket data updates; zero allows updates at every step.", example = "0.1")
    @JsonProperty("liveLogInterval")
    final Double liveLogInterval;
    @Schema(description = "Scenario master-model configuration text, used by scenario-based simulation.")
    @JsonProperty("masterModel")
    final String masterModel;

    @JsonCreator
    public BaseSimulateRequestBody(@JsonProperty("startTime") double startTime, @JsonProperty("endTime") double endTime,
            @JsonProperty("logLevels") Map<String, List<String>> logLevels, @JsonProperty("reportProgress") Boolean reportProgress,
            @JsonProperty("liveLogInterval") Double liveLogInterval, @JsonProperty("masterModel") String masterModel) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.logLevels = logLevels;
        this.reportProgress = reportProgress;
        this.liveLogInterval = liveLogInterval;
        this.masterModel = masterModel;
    }

    public String getMasterModel() {
        return masterModel;
    }

    public Map<String, List<String>> getLogLevels() {
        return logLevels;
    }

    public Boolean getReportProgress() {
        return reportProgress;
    }

    public Double getLiveLogInterval() {
        return liveLogInterval;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }
}
