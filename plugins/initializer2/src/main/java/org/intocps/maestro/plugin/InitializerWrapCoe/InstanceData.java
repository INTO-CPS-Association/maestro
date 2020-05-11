package org.intocps.maestro.plugin.InitializerWrapCoe;

import org.intocps.orchestration.coe.config.ModelParameter;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.List;

public class InstanceData {
    public final ModelDescription modelDescription;
    //public final List<ModelParameter> modelParameters;
    //public final double startTime;
    //public final double endTime;
    public final String fmu;

    public InstanceData(String instanceName, ModelDescription modelDescription, List<ModelParameter> modelParameters, double startTime, double endTime, String fmu) {
        this.instanceName = instanceName;
        this.modelDescription = modelDescription;
        this.modelParameters = modelParameters;
        this.startTime = startTime;
        this.endTime = endTime;
        this.fmu = fmu;
    }
}
