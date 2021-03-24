package org.intocps.maestro.core.api;

public class VarStepSizeAlgorithm implements IStepAlgorithm {
    public final double endTime;
    public final double minStepSize;
    public final double maxStepSize;
    public final double initialStepSize;

    public VarStepSizeAlgorithm(double endTime, Double[] stepSizes, Double initSize) {
        if(stepSizes.length != 3) {
            //TODO: throw
        }
        this.endTime = endTime;
        this.minStepSize = stepSizes[0];
        this.maxStepSize = stepSizes[1];
        this.initialStepSize = initSize;
    }

    @Override
    public StepAlgorithm getType() {
        return StepAlgorithm.VARIABLESTEP;
    }

    @Override
    public double getEndTime(){
        return endTime;
    }

    @Override
    public double getStepSize(){
        return initialStepSize;
    }

    public class Constraint {

    }
}


