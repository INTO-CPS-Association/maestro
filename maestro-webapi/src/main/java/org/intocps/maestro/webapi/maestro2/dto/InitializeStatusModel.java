package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class InitializeStatusModel extends StatusModel {

    @JsonProperty("avaliableLogLevels")
    private final Map<String, List<LogLevelModel>> avaliableLogLevels;

    @JsonCreator
    public InitializeStatusModel(@JsonProperty("status") String status, @JsonProperty("sessionid") String sessionId,
            @JsonProperty("avaliableLogLevels") Map<String, List<LogLevelModel>> avaliableLogLevels,
            @JsonProperty("lastExecTime") final long lastExecTime) {
        super(status, sessionId, lastExecTime);
        this.avaliableLogLevels = avaliableLogLevels;
    }

    public static class LogLevelModel {
        final String name;
        final String description;

        public LogLevelModel(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
}
