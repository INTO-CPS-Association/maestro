package org.intocps.maestro.plugin;

import org.intocps.maestro.core.dto.IAlgorithmConfig;

import java.util.HashSet;
import java.util.Set;

public class JacobianStepConfig implements IPluginConfiguration {
    public Set<String> variablesOfInterest = new HashSet<>();
    public boolean stabilisation = false;
    public double absoluteTolerance;
    public double relativeTolerance;
    public int stabilisationLoopMaxIterations;
    public boolean simulationProgramDelay;
    public boolean setGetDerivatives;
    public double startTime;
    public double endTime;
    public IAlgorithmConfig stepAlgorithm;
}
