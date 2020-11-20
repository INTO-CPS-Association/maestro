package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class StatusModel {
    @JsonProperty("status")
    public String status;
    @JsonProperty("sessionId")
    public String sessionId;

    @JsonProperty("lastExecTime")
    public long lastExecTime;

    @JsonProperty("warnings")
    public List<String> warnings;

    @JsonProperty("errors")
    public List<String> errors;

    public StatusModel() {
    }

    public StatusModel(String status, String sessionId, long lastExecTime) {
        this.status = status;
        this.sessionId = sessionId;
        this.lastExecTime = lastExecTime;
    }

    public StatusModel(String status, String sessionId, long lastExecTime, List<String> errors, List<String> warnings) {
        this.status = status;
        this.sessionId = sessionId;
        this.lastExecTime = lastExecTime;
        this.errors = errors;
        this.warnings = warnings;
    }
}
