package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Session status or operation result. Status is a message, not an HTTP status code.")
public class StatusModel {
    @Schema(description = "Current session state or operation message.", example = "Simulation completed")
    @JsonProperty("status")
    public String status;
    @Schema(description = "Session identifier returned by createSession.")
    @JsonProperty("sessionId")
    public String sessionId;

    @Schema(description = "Duration of the last execution in milliseconds.")
    @JsonProperty("lastExecTime")
    public long lastExecTime;

    @Schema(description = "Warnings or captured process output, depending on execution mode. May be null.")
    @JsonProperty("warnings")
    public List<String> warnings;

    @Schema(description = "Reported errors or captured child-process stderr. May be null; not all failure paths populate this field.")
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
