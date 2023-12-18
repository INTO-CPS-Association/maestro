package org.intocps.maestro.template;

import org.intocps.verification.scenarioverifier.core.masterModel.MasterModel;
import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;

import java.util.List;
import java.util.Map;

public class ScenarioConfiguration {
    private final Fmi2SimulationEnvironment simulationEnvironment;
    private final MasterModel masterModel;
    private final Map<String, Object> parameters;
    private final ExecutionParameters executionParameters;
    private final Pair<Framework, Fmi2SimulationEnvironmentConfiguration> frameworkConfig;
    private final Boolean loggingOn;
    private final Map<String, List<String>> logLevels;

    public ScenarioConfiguration(Fmi2SimulationEnvironment simulationEnvironment, MasterModel masterModel, Map<String, Object> parameters,
            Double convergenceRelativeTolerance, Double convergenceAbsoluteTolerance, Integer convergenceAttempts, Double startTime, Double endTime,
            Double stepSize, Pair<Framework, Fmi2SimulationEnvironmentConfiguration> frameworkConfig, Boolean loggingOn,
            Map<String, List<String>> logLevels) {
        this.simulationEnvironment = simulationEnvironment;
        this.masterModel = masterModel;
        this.parameters = parameters;
        this.frameworkConfig = frameworkConfig;
        this.logLevels = logLevels;
        executionParameters =
                new ExecutionParameters(convergenceRelativeTolerance, convergenceAbsoluteTolerance, convergenceAttempts, startTime, endTime,
                        stepSize);
        this.loggingOn = loggingOn;
    }

    public Fmi2SimulationEnvironment getSimulationEnvironment() {
        return simulationEnvironment;
    }

    public Pair<Framework, Fmi2SimulationEnvironmentConfiguration> getFrameworkConfig() {
        return frameworkConfig;
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

    public Boolean getLoggingOn() {
        return loggingOn;
    }

    public Map<String, List<String>> getLogLevels() {
        return logLevels;
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
