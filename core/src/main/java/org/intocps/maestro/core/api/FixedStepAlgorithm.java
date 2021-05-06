package org.intocps.maestro.core.api;

public class FixedStepAlgorithm implements IStepAlgorithm {
    private final double endTime;
    private final double stepSize;
    private final double startTime;

    public FixedStepAlgorithm(double endTime, double stepSize, double startTime) {
        this.endTime = endTime;
        this.stepSize = stepSize;
        this.startTime = startTime;
    }

    @Override
    public StepAlgorithm getType() {
        return StepAlgorithm.FIXEDSTEP;
    }

    @Override
    public double getEndTime(){
        return endTime;
    }

    @Override
    public double getStepSize(){
        return stepSize;
    }

    @Override
    public double getStartTime() {
        return startTime;
    }
}
