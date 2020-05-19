package org.intocps.maestro.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PluginIdentification {
    @JsonProperty("name")
    public String name;
    @JsonProperty("version")
    public String version;
}
