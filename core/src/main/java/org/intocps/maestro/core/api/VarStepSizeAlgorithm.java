package org.intocps.maestro.core.api;

public class VarStepSizeAlgorithm implements IStepAlgorithm {
    private final double endTime;
    private final double minStepSize;
    private final double maxStepSize;
    private final double initialStepSize;
    private final String initialisationDataForVariableStep;

    public VarStepSizeAlgorithm(double endTime, Double[] stepSizes, Double initSize, String initDataForVarStep) {
        if (stepSizes.length != 3) {
            //TODO: throw
        }
        this.endTime = endTime;
        this.minStepSize = stepSizes[0];
        this.maxStepSize = stepSizes[1];
        this.initialStepSize = initSize;
        this.initialisationDataForVariableStep = initDataForVarStep;
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

    public class Constraint {

    }
}


