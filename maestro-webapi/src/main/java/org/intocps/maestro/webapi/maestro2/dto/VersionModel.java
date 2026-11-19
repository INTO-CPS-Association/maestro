package org.intocps.maestro.webapi.maestro2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Version of the running Maestro build.")
public class VersionModel {
    @Schema(description = "Build version from maestro.properties.", example = "4.1.1-SNAPSHOT")
    public final String version;

    public VersionModel(String version) {
        this.version = version;
    }
}
