package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

public class SimulateRequestBody {
    @ApiModelProperty(value = "The start time of the co-simulation")
    @JsonProperty("startTime")
    final double startTime;
    @JsonProperty("endTime")
    final double endTime;
    @JsonProperty("logLevels")
    final Map<String, List<String>> logLevels;
    @JsonProperty("reportProgress")
    final Boolean reportProgress;
    @JsonProperty("liveLogInterval")
    final Double liveLogInterval;
    @JsonProperty("masterModel")
    final String masterModel;

    @JsonCreator
    public SimulateRequestBody(@JsonProperty("startTime") double startTime, @JsonProperty("endTime") double endTime,
            @JsonProperty("logLevels") Map<String, List<String>> logLevels, @JsonProperty("reportProgress") Boolean reportProgress,
            @JsonProperty("liveLogInterval") Double liveLogInterval, @JsonProperty("masterModel") String masterModel) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.logLevels = logLevels;
        this.reportProgress = reportProgress;
        this.liveLogInterval = liveLogInterval;
        this.masterModel = masterModel;
    }
    public String getMasterModel() { return masterModel;}

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
