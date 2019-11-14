package org.intocps.orchestration.coe.webapi.services;

import org.intocps.fmi.*;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.HashMap;
import java.util.List;

public class EnvironmentFMUComponent implements IFmiComponent {
    private final IFmu fmu;
    private final HashMap<ModelDescription.ScalarVariable, Object> inputs = new HashMap<>();
    private final HashMap<ModelDescription.ScalarVariable, Object> outputs = new HashMap<>();

    public EnvironmentFMUComponent(IFmu fmu, List<ModelDescription.ScalarVariable> inputs, List<ModelDescription.ScalarVariable> outputs) {
        inputs.forEach(sv -> this.inputs.put(sv, null));
        outputs.forEach(sv -> this.outputs.put(sv, null));
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
        return Fmi2Status.OK;
    }

    @Override
    public Fmi2Status setRealInputDerivatives(long[] longs, int[] ints, double[] doubles) throws FmuInvocationException {
        return Fmi2Status.OK;
    }

    @Override
    public FmuResult<double[]> getRealOutputDerivatives(long[] longs, int[] ints) throws FmuInvocationException {
        return null;
    }

    @Override
    public FmuResult<double[]> getDirectionalDerivative(long[] longs, long[] longs1, double[] doubles) throws FmuInvocationException {
        return null;
    }

    @Override
    public Fmi2Status doStep(double v, double v1, boolean b) throws FmuInvocationException {
        return Fmi2Status.OK;
    }

    @Override
    public FmuResult<double[]> getReal(long[] longs) throws FmuInvocationException {
        return null;
    }

    @Override
    public FmuResult<int[]> getInteger(long[] longs) throws FmuInvocationException {
        return null;
    }

    @Override
    public FmuResult<boolean[]> getBooleans(long[] longs) throws FmuInvocationException {
        return null;
    }

    @Override
    public FmuResult<String[]> getStrings(long[] longs) throws FmuInvocationException {
        return null;
    }

    @Override
    public Fmi2Status setBooleans(long[] longs, boolean[] booleans) throws InvalidParameterException, FmiInvalidNativeStateException {
        return null;
    }

    @Override
    public Fmi2Status setReals(long[] longs, double[] doubles) throws InvalidParameterException, FmiInvalidNativeStateException {
        return null;
    }

    @Override
    public Fmi2Status setIntegers(long[] longs, int[] ints) throws InvalidParameterException, FmiInvalidNativeStateException {
        return null;
    }

    @Override
    public Fmi2Status setStrings(long[] longs, String[] strings) throws InvalidParameterException, FmiInvalidNativeStateException {
        return null;
    }

    @Override
    public FmuResult<Boolean> getBooleanStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        return null;
    }

    @Override
    public FmuResult<Fmi2Status> getStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        return null;
    }

    @Override
    public FmuResult<Integer> getIntegerStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        return null;
    }

    @Override
    public FmuResult<Double> getRealStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        return null;
    }

    @Override
    public FmuResult<String> getStringStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        return null;
    }

    @Override
    public Fmi2Status terminate() throws FmuInvocationException {
        return null;
    }

    @Override
    public void freeInstance() throws FmuInvocationException {

    }

    @Override
    public FmuResult<IFmiComponentState> getState() throws FmuInvocationException {
        return null;
    }

    @Override
    public Fmi2Status setState(IFmiComponentState iFmiComponentState) throws FmuInvocationException {
        return null;
    }

    @Override
    public Fmi2Status freeState(IFmiComponentState iFmiComponentState) throws FmuInvocationException {
        return null;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public FmuResult<Double> getMaxStepSize() throws FmiInvalidNativeStateException {
        return null;
    }
}
