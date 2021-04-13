package org.intocps.maestro.plugin;

import java.util.HashSet;
import java.util.Set;

public class JacobianStepConfig implements IPluginConfiguration {
    public Set<String> variablesOfInterest = new HashSet<>();
    public boolean stabilisation = false;
    public double absoluteTolerance;
    public double relativeTolerance;
    public int stabilisationLoopMaxIterations;
    public boolean simulationProgramDelay;
    public boolean retrieveDerivatives;
}
