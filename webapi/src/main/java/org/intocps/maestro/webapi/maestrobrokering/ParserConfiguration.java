package org.intocps.maestro.webapi.maestrobrokering;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;


public class ParserConfiguration {
    @JsonRawValue
    List<ParserPluginConfiguration> root = new ArrayList<>();

    public ParserConfiguration() {
    }

    public void addParserPluginConfiguration(ParserPluginConfiguration parserPluginConfiguration) {
        this.root.add(parserPluginConfiguration);
    }


    public static class ParserPluginIdentification {
        @JsonProperty
        public String name;
        @JsonProperty
        public String version;

        public void setName(String s) {
            this.name = s;
        }
    }

    public static class ParserPluginConfiguration {
        @JsonProperty
        public ParserPluginIdentification identification;
        @JsonProperty
        public JsonNode config;
    }

}

