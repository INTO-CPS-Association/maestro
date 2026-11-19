package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Execution options and an explicit scenario master model.")
public class SigverSimulateRequestBody extends BaseSimulateRequestBody {

    @Schema(description = "Master-model configuration text containing the scenario, connections, initialization, and co-simulation steps.")
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
