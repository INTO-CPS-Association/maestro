package org.intocps.maestro.multimodelparser;

public class InstanceWithFMU {
    public final FmuWithMD fmuWithMD;
    public final String instanceName;

    public InstanceWithFMU(FmuWithMD fmuWithMD, String instanceName) {
        this.fmuWithMD = fmuWithMD;
        this.instanceName = instanceName;
    }
}
