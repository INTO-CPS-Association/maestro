package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Select the execution mode for subsequent simulations in a session.")
public class CliExecutionRequestBody {
    @Schema(description = "True to launch a child Java process; false to execute in-process.")
    @JsonProperty("executeViaCLI")
    public final boolean executeViaCLI;

    @JsonCreator
    public CliExecutionRequestBody(@JsonProperty("executeViaCLI") boolean executeViaCLI) {
        this.executeViaCLI = executeViaCLI;
    }
}
