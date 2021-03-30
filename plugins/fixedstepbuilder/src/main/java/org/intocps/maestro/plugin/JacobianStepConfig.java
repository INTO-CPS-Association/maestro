package org.intocps.maestro.plugin;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JacobianStepConfig implements IPluginConfiguration {
    public Set<String> variablesOfInterest = new HashSet<>();
    public boolean stabilisation = false;
    public double absoluteTolerance;
    public double relativeTolerance;
    public int stabilisationLoopMaxIterations;
}
