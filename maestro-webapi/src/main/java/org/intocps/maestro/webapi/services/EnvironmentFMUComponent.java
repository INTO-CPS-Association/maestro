package org.intocps.maestro.webapi.services;

import org.intocps.fmi.*;
import org.intocps.maestro.fmi.HierarchicalCoeStateComponent;
import org.intocps.maestro.fmi.Fmi2ModelDescription;

import java.util.List;

public class EnvironmentFMUComponent extends HierarchicalCoeStateComponent {
    private final IFmu fmu;

    public EnvironmentFMUComponent(IFmu fmu, List<Fmi2ModelDescription.ScalarVariable> inputs, List<Fmi2ModelDescription.ScalarVariable> outputs) {
        inputs.forEach(sv -> {
            inputsSvToValue.put(sv, sv.type.start != null ? sv.type.start : null);
            refToSv.put(sv.valueReference, sv);
        });
        outputs.forEach(sv -> {
            outputsSvToValue.put(sv, sv.type.start != null ? sv.type.start : null);
            refToSv.put(sv.valueReference, sv);
        });

        this.fmu = fmu;
    }

    @Override
    public IFmu getFmu() {
        return this.fmu;
    }

    @Override
    public Fmi2Status setDebugLogging(boolean b, String[] strings) throws FmuInvocationException {
        return Fmi2Status.OK;
    }

    @Override
    public Fmi2Status setupExperiment(boolean b, double v, double v1, boolean b1, double v2) throws FmuInvocationException {
        return Fmi2Status.OK;
    }

    @Override
    public Fmi2Status enterInitializationMode() throws FmuInvocationException {
        return Fmi2Status.OK;
    }

    @Override
    public Fmi2Status exitInitializationMode() throws FmuInvocationException {
        return Fmi2Status.OK;
    }

    @Override
    public Fmi2Status reset() throws FmuInvocationException {
        return Fmi2Status.Error;
    }

    @Override
    public Fmi2Status doStep(double v, double v1, boolean b) throws FmuInvocationException {
        return Fmi2Status.OK;
    }

    @Override
    public FmuResult<Boolean> getBooleanStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        return new FmuResult<>(Fmi2Status.Error, null);
    }

    @Override
    public FmuResult<Fmi2Status> getStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        return new FmuResult<>(Fmi2Status.Error, null);
    }

    @Override
    public FmuResult<Integer> getIntegerStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        return new FmuResult<>(Fmi2Status.Error, null);
    }

    @Override
    public FmuResult<Double> getRealStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        return new FmuResult<>(Fmi2Status.Error, null);
    }

    @Override
    public FmuResult<String> getStringStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        return new FmuResult<>(Fmi2Status.Error, null);
    }

    @Override
    public Fmi2Status terminate() throws FmuInvocationException {
        return Fmi2Status.OK;
    }

    @Override
    public void freeInstance() throws FmuInvocationException {

    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public FmuResult<Double> getMaxStepSize() throws FmiInvalidNativeStateException {
        return null;
    }

    public Object getValue(Fmi2ModelDescription.ScalarVariable value) {
        return this.inputsSvToValue.get(value);
    }

    public void setOutput(Fmi2ModelDescription.ScalarVariable scalarVariable, Object value) {
        this.outputsSvToValue.replace(scalarVariable, value);
    }
}
