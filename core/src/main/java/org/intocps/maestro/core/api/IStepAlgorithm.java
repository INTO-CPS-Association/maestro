package org.intocps.maestro.core.api;

public interface IStepAlgorithm {
    StepAlgorithm getType();
    double getEndTime();
    double getStepSize();
    double getStartTime();
}
