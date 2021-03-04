package org.intocps.maestro.webapi.maestro2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CliExecutionRequestBody {
    @JsonProperty("executeViaCLI")
    public final boolean executeViaCLI;

    @JsonCreator
    public CliExecutionRequestBody(@JsonProperty("executeViaCLI") boolean executeViaCLI) {
        this.executeViaCLI = executeViaCLI;
    }
}
