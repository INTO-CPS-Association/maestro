package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Initialization result and log categories discovered from the FMUs.")
public class InitializeStatusModel extends StatusModel {

    @Schema(description = "Available FMI logging categories, keyed by FMU identifier. May be incomplete if FMU inspection failed.")
    @JsonProperty("availableLogLevels")
    public final Map<String, List<LogLevelModel>> availableLogLevels;

    @JsonCreator
    public InitializeStatusModel(@JsonProperty("status") String status, @JsonProperty("sessionid") String sessionId,
            @JsonProperty("availableLogLevels") Map<String, List<LogLevelModel>> availableLogLevels,
            @JsonProperty("lastExecTime") final long lastExecTime) {
        super(status, sessionId, lastExecTime);
        this.availableLogLevels = availableLogLevels;
    }

    public static class LogLevelModel {
        @Schema(description = "FMI log-category name.")
        public final String name;
        @Schema(description = "Log-category description from the FMU model description.")
        public final String description;

        @JsonCreator
        public LogLevelModel(@JsonProperty("name") String name, @JsonProperty("description") String description) {
            this.name = name;
            this.description = description;
        }
    }
}
