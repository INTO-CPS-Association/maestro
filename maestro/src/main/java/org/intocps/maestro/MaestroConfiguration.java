package org.intocps.maestro;

import com.fasterxml.jackson.annotation.JsonProperty;

interface IMaestroConfiguration {
    int getMaximumExpansionDepth();
}

public class MaestroConfiguration implements IMaestroConfiguration {

    // The value 4 is chosen based on empirical evidence.
    @JsonProperty("maximum_expansion_depth")
    public int maximumExpansionDepth = 5;

    @Override
    public int getMaximumExpansionDepth() {
        return maximumExpansionDepth;
    }
}
