package org.intocps.maestro.plugin.InitializerWrapCoe.FMIStatementInterface;


import org.intocps.fmi.*;
import org.intocps.maestro.plugin.InitializerNew.ConversionUtilities.BooleanUtils;
import org.intocps.maestro.plugin.InitializerNew.Spec.StatementContainer;

import java.util.Arrays;

public class StatementFMIComponent implements IFmiComponent {
    private final String name;
    StatementContainer stmContainer = StatementContainer.getInstance();

    public StatementFMIComponent(String name) {
        this.name = name;
    }

    @Override
    public IFmu getFmu() {
        return null;
    }

    @Override
    public Fmi2Status setDebugLogging(boolean b, String[] strings) throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public Fmi2Status setupExperiment(boolean b, double v, double v1, boolean b1, double v2) throws FmuInvocationException {
        stmContainer.createSetupExperimentStatement(name, b, v, v1, b1, v2);

        return Fmi2Status.OK;
    }

    @Override
    public Fmi2Status enterInitializationMode() throws FmuInvocationException {
        stmContainer.enterInitializationMode(name);
        return Fmi2Status.OK;
    }

    @Override
    public Fmi2Status exitInitializationMode() throws FmuInvocationException {
        stmContainer.exitInitializationMode(name);
        return Fmi2Status.OK;
    }

    @Override
    public Fmi2Status reset() throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public Fmi2Status setRealInputDerivatives(long[] longs, int[] ints, double[] doubles) throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public FmuResult<double[]> getRealOutputDerivatives(long[] longs, int[] ints) throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public FmuResult<double[]> getDirectionalDerivative(long[] longs, long[] longs1, double[] doubles) throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public Fmi2Status doStep(double v, double v1, boolean b) throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public FmuResult<double[]> getReal(long[] longs) throws FmuInvocationException {
        stmContainer.getReals(name, longs);
        FmuResult<double[]> result = new FmuResult<>(Fmi2Status.OK, Arrays.stream(longs).mapToDouble(v -> 0.0).toArray());
        return result;
    }

    @Override
    public FmuResult<int[]> getInteger(long[] longs) throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public FmuResult<boolean[]> getBooleans(long[] longs) throws FmuInvocationException {
        stmContainer.getBooleans(name, longs);
        FmuResult<boolean[]> result =
                new FmuResult<>(Fmi2Status.OK, Arrays.stream(longs).mapToObj(v -> false).collect(BooleanUtils.TO_BOOLEAN_ARRAY));
        return result;
    }

    @Override
    public FmuResult<String[]> getStrings(long[] longs) throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public Fmi2Status setBooleans(long[] longs, boolean[] booleans) throws InvalidParameterException, FmiInvalidNativeStateException {
        stmContainer.setBooleans(name, longs, booleans);
        return Fmi2Status.OK;
    }

    @Override
    public Fmi2Status setReals(long[] longs, double[] doubles) throws InvalidParameterException, FmiInvalidNativeStateException {
        stmContainer.setReals2(name, longs, doubles);
        return Fmi2Status.OK;
    }

    @Override
    public Fmi2Status setIntegers(long[] longs, int[] ints) throws InvalidParameterException, FmiInvalidNativeStateException {
        stmContainer.setIntegers(name, longs, ints);
        return Fmi2Status.OK;
    }

    @Override
    public Fmi2Status setStrings(long[] longs, String[] strings) throws InvalidParameterException, FmiInvalidNativeStateException {
        throw new FmiInvalidNativeStateException("notImplemented");
    }

    @Override
    public FmuResult<Boolean> getBooleanStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public FmuResult<Fmi2Status> getStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public FmuResult<Integer> getIntegerStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public FmuResult<Double> getRealStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public FmuResult<String> getStringStatus(Fmi2StatusKind fmi2StatusKind) throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public Fmi2Status terminate() throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public void freeInstance() throws FmuInvocationException {

    }

    @Override
    public FmuResult<IFmiComponentState> getState() throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public Fmi2Status setState(IFmiComponentState iFmiComponentState) throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public Fmi2Status freeState(IFmiComponentState iFmiComponentState) throws FmuInvocationException {
        throw new FmuInvocationException("notImplemented");
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public FmuResult<Double> getMaxStepSize() throws FmiInvalidNativeStateException {
        throw new FmiInvalidNativeStateException("notImplemented");
    }
}
