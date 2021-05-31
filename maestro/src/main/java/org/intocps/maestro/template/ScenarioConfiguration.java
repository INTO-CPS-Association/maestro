package org.intocps.maestro.template;

import core.MasterModel;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import java.util.Map;

public class ScenarioConfiguration {
    private final Fmi2SimulationEnvironment simulationEnvironment;
    private final MasterModel masterModel;
    private final Map<String, Object> parameters;
    private final ExecutionParameters executionParameters;

    public ScenarioConfiguration(Fmi2SimulationEnvironment simulationEnvironment, MasterModel masterModel, Map<String, Object> parameters,
            Double convergenceRelativeTolerance, Double convergenceAbsoluteTolerance, Integer convergenceAttempts, Double startTime, Double endTime,
            Double stepSize) {
        this.simulationEnvironment = simulationEnvironment;
        this.masterModel = masterModel;
        this.parameters = parameters;
        executionParameters =
                new ExecutionParameters(convergenceRelativeTolerance, convergenceAbsoluteTolerance, convergenceAttempts, startTime, endTime,
                        stepSize);
    }

    public Fmi2SimulationEnvironment getSimulationEnvironment() {
        return simulationEnvironment;
    }

    public MasterModel getMasterModel() {
        return masterModel;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public ExecutionParameters getExecutionParameters() {
        return executionParameters;
    }

    public static class ExecutionParameters {
        private final Double convergenceRelativeTolerance;
        private final Double convergenceAbsoluteTolerance;
        private final Integer convergenceAttempts;
        private final Double startTime;
        private final Double endTime;
        private final Double stepSize;
        ExecutionParameters(Double convergenceRelativeTolerance, Double convergenceAbsoluteTolerance, Integer convergenceAttempts, Double startTime,
                Double endTime, Double stepSize) {
            this.convergenceRelativeTolerance = convergenceRelativeTolerance;
            this.convergenceAbsoluteTolerance = convergenceAbsoluteTolerance;
            this.convergenceAttempts = convergenceAttempts;
            this.startTime = startTime;
            this.endTime = endTime;
            this.stepSize = stepSize;
        }

        public Double getConvergenceRelativeTolerance() {
            return convergenceRelativeTolerance;
        }

        public Double getConvergenceAbsoluteTolerance() {
            return convergenceAbsoluteTolerance;
        }

        public int getConvergenceAttempts() {
            return convergenceAttempts;
        }

        public Double getStartTime() {
            return startTime;
        }

        public Double getEndTime() {
            return endTime;
        }

        public Double getStepSize() {
            return stepSize;
        }
    }
}
