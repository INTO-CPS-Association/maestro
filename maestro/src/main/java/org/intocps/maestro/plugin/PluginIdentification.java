package org.intocps.maestro.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;

class PluginIdentification {
    @JsonProperty("name")
    String name;
    @JsonProperty("version")
    String version;
}
