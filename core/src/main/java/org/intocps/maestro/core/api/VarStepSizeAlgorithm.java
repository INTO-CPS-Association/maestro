package org.intocps.maestro.core.api;

public class VarStepSizeAlgorithm implements IStepAlgorithm {
    public final double endTime;
    public final double initialStepSize;
    public final double startTime;

    public VarStepSizeAlgorithm(double endTime, double initialStepSize, double startTime) {
        this.endTime = endTime;
        this.initialStepSize = initialStepSize;
        this.startTime = startTime;
    }

    @Override
    public StepAlgorithm getType() {
        return StepAlgorithm.FIXEDSTEP;
    }
}
