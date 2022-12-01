package org.intocps.maestro.plugin;

import org.intocps.maestro.core.dto.IAlgorithmConfig;
import org.intocps.maestro.core.dto.VarStepConstraint;
import org.intocps.maestro.core.dto.VariableStepAlgorithmConfig;

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
    public Double endTime;
    public IAlgorithmConfig stepAlgorithm;

    public Set<String> getVariablesOfInterest() {
        if (stepAlgorithm instanceof VariableStepAlgorithmConfig && variablesOfInterest.isEmpty()) {
            Set<String> variablesOfInterest = new HashSet<>();

            ((VariableStepAlgorithmConfig) stepAlgorithm).getConstraints().values().forEach(v -> {
                if (v instanceof VarStepConstraint.ZeroCrossingConstraint) {
                    variablesOfInterest.addAll(((VarStepConstraint.ZeroCrossingConstraint) v).getPorts());
                } else if (v instanceof VarStepConstraint.BoundedDifferenceConstraint) {
                    variablesOfInterest.addAll(((VarStepConstraint.BoundedDifferenceConstraint) v).getPorts());
                }
            });
            this.variablesOfInterest = variablesOfInterest;
            return variablesOfInterest;
        }
        return this.variablesOfInterest;
    }
}
