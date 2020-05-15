package org.intocps.maestro.webapi.maestrobrokering;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class ParserContext {
    @JsonProperty("fmus")
    final Map<String, String> fmus;
    @JsonProperty("connections")
    final Map<String, List<String>> connections;
}
