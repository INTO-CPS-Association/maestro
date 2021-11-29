package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class InitializeStatusModel extends StatusModel {

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
        public final String name;
        public final String description;

        @JsonCreator
        public LogLevelModel(@JsonProperty("name") String name, @JsonProperty("description") String description) {
            this.name = name;
            this.description = description;
        }
    }
}
