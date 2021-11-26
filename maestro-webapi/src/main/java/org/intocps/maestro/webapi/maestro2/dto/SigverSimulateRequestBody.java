package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

public class SigverSimulateRequestBody extends BaseSimulateRequestBody {

    @ApiModelProperty(value = "The sigver master model containing both the scenario, connections, initialization and co-sim steps")
    @JsonProperty("masterModel")
    final String masterModel;

    @JsonCreator
    public SigverSimulateRequestBody(@JsonProperty("startTime") double startTime, @JsonProperty("endTime") double endTime,
            @JsonProperty("logLevels") Map<String, List<String>> logLevels, @JsonProperty("reportProgress") Boolean reportProgress,
            @JsonProperty("liveLogInterval") Double liveLogInterval, @JsonProperty("masterModel") String masterModel) {
        super(startTime, endTime, logLevels, reportProgress, liveLogInterval, masterModel);
        this.masterModel = masterModel;
    }

    public String getMasterModel() {
        return masterModel;
    }
}
