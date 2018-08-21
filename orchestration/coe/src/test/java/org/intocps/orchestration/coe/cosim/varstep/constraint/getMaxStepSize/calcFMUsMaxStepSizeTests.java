package org.intocps.orchestration.coe.cosim.varstep.constraint.getMaxStepSize;

import org.intocps.fmi.Fmi2Status;
import org.intocps.fmi.FmiInvalidNativeStateException;
import org.intocps.fmi.FmuResult;
import org.intocps.fmi.IFmiComponent;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.cosim.VariableStepSizeCalculator;
import org.intocps.orchestration.coe.cosim.base.FmiSimulationInstance;
import org.intocps.orchestration.coe.cosim.varstep.StepsizeInterval;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class calcFMUsMaxStepSizeTests {

    @Test
    public void testAnyLessThanMinStepReturnsMinStep() throws FmiInvalidNativeStateException {
        StepsizeInterval ssi = new StepsizeInterval(1.0, 5.0);
        VariableStepSizeCalculator vssc = new VariableStepSizeCalculator(null, ssi, 0.0);

        scala.collection.immutable.Map<ModelConnection.ModelInstance, FmiSimulationInstance> m = createScalaMap2(
                new ModelConnection.ModelInstance("a","a"),  new FmuResult<Double>(Fmi2Status.OK, 0.1),
                new ModelConnection.ModelInstance("b","b"), new FmuResult<Double>(Fmi2Status.OK, 2.0));

        double min = 1.0;
        double result = vssc.calcFMUsMaxStepSize(min, 5.0, m);
        assertEquals(min, result, 0.000001);

    }

    @Test
    public void testAllGreaterThanMaxStepReturnsMaxStep() throws FmiInvalidNativeStateException {
        StepsizeInterval ssi = new StepsizeInterval(1.0, 5.0);
        VariableStepSizeCalculator vssc = new VariableStepSizeCalculator(null, ssi, 0.0);

        scala.collection.immutable.Map<ModelConnection.ModelInstance, FmiSimulationInstance> m = createScalaMap2(
                new ModelConnection.ModelInstance("a","a"),  new FmuResult<Double>(Fmi2Status.OK, 7.0),
                new ModelConnection.ModelInstance("b","b"), new FmuResult<Double>(Fmi2Status.OK, 6.0));

        double max = 5.0;
        double result = vssc.calcFMUsMaxStepSize(1.0, max, m);
        assertEquals(max, result, 0.000001);
    }

    @Test
    public void testMinOfInterval() throws FmiInvalidNativeStateException {
        StepsizeInterval ssi = new StepsizeInterval(1.0, 5.0);
        VariableStepSizeCalculator vssc = new VariableStepSizeCalculator(null, ssi, 0.0);

        double expected = 3.5;

        scala.collection.immutable.Map<ModelConnection.ModelInstance, FmiSimulationInstance> m = createScalaMap2(
                new ModelConnection.ModelInstance("a","a"),  new FmuResult<Double>(Fmi2Status.OK, expected),
                new ModelConnection.ModelInstance("b","b"), new FmuResult<Double>(Fmi2Status.OK, 4.1));

        double result = vssc.calcFMUsMaxStepSize(1.0, 5.0, m);
        assertEquals(expected, result, 0.000001);
    }

    @Test
    public void testNonSuceedsReturnsMax() throws FmiInvalidNativeStateException {
        StepsizeInterval ssi = new StepsizeInterval(1.0, 5.0);
        VariableStepSizeCalculator vssc = new VariableStepSizeCalculator(null, ssi, 0.0);

        scala.collection.immutable.Map<ModelConnection.ModelInstance, FmiSimulationInstance> m = createScalaMap2(
                new ModelConnection.ModelInstance("a","a"),  new FmuResult<Double>(Fmi2Status.Error, 3.5),
                new ModelConnection.ModelInstance("b","b"), new FmuResult<Double>(Fmi2Status.Error, 4.1));

        double max = 5.0;
        double min = 1.0;
        double result = vssc.calcFMUsMaxStepSize(min, max, m);
        assertEquals(max, result, 0.000001);
    }

    public static  scala.collection.immutable.Map<ModelConnection.ModelInstance, FmiSimulationInstance> createScalaMap2(ModelConnection.ModelInstance mi1, FmuResult res1, ModelConnection.ModelInstance mi2, FmuResult res2) throws FmiInvalidNativeStateException {
        IFmiComponent fc1 = mock(IFmiComponent.class);
        when(fc1.getMaxStepSize()).thenReturn(res1);
        FmiSimulationInstance fsi1 = new FmiSimulationInstance(fc1,null);

        IFmiComponent fc2 = mock(IFmiComponent.class);
        when(fc2.getMaxStepSize()).thenReturn(res2);
        FmiSimulationInstance fsi2 = new FmiSimulationInstance(fc2,null);
        return new scala.collection.immutable.Map.Map2<>(mi1, fsi1, mi2, fsi2);
    }
}
