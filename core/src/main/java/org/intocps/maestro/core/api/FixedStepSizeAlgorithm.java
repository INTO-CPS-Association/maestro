package org.intocps.maestro.core.api;

public class FixedStepSizeAlgorithm implements IStepAlgorithm {
    public final double endTime;
    public final double stepSize;

    public FixedStepSizeAlgorithm(double endTime, double stepSize) {
        this.endTime = endTime;
        this.stepSize = stepSize;
    }

    @Override
    public StepAlgorithm getType() {
        return StepAlgorithm.FIXEDSTEP;
    }
}
