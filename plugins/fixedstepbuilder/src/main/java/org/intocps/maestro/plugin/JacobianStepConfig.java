package org.intocps.maestro.plugin;
import java.util.ArrayList;
import java.util.List;

public class JacobianStepConfig implements IPluginConfiguration {
    public List<String> variablesOfInterest = new ArrayList<>();
    public boolean stabilisation = false;
    public double absoluteTolerance;
    public double relativeTolerance;
    public int stabilisationLoopMaxIterations;
}
