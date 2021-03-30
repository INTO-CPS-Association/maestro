package org.intocps.maestro.core.api;

public class VariableStepAlgorithm implements IStepAlgorithm {
    private final double endTime;
    private final double startTime;
    private final double minStepSize;
    private final double maxStepSize;
    private final double initialStepSize;
    private final String initialisationDataForVariableStep;

    public VariableStepAlgorithm(double endTime, Double[] stepSizes, Double initSize, String initDataForVarStep, double startTime) {
        if (stepSizes.length != 3) {
            //TODO: throw
        }
        this.endTime = endTime;
        this.minStepSize = stepSizes[0];
        this.maxStepSize = stepSizes[1];
        this.initialStepSize = initSize;
        this.initialisationDataForVariableStep = initDataForVarStep;
        this.startTime = startTime;
    }

    public String getInitialisationDataForVariableStep() {
        return initialisationDataForVariableStep;
    }

    public double getMinStepSize() {
        return minStepSize;
    }

    public double getMaxStepSize() {
        return maxStepSize;
    }

    @Override
    public StepAlgorithm getType() {
        return StepAlgorithm.VARIABLESTEP;
    }

    @Override
    public double getEndTime() {
        return endTime;
    }

    @Override
    public double getStepSize() {
        return initialStepSize;
    }

    @Override
    public double getStartTime() {
        return startTime;
    }

    public class Constraint {

    }
}


