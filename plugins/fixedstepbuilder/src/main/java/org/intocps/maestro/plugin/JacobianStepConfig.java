package org.intocps.maestro.plugin;
import java.util.List;

class JacobianStepConfig implements IPluginConfiguration {
    public List<String> variablesOfInterest;
    public boolean stabilisation;
    public double absoluteTolerance;
    public double relativeTolerance;
    public int stabilisationLoopMaxIterations;
}
