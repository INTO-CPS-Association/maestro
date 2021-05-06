import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.derivativeestimator.DerivativeEstimatorInstanceValue;
import org.intocps.maestro.interpreter.values.derivativeestimator.DerivativeEstimatorValue;
import org.intocps.maestro.interpreter.values.derivativeestimator.ScalarDerivativeEstimator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * The values used and expected values in the following tests originates from Maestro 1 tests of the derivative estimator.
 */
public class DerivativeEstimatorTests {

    @Test
    public void testDerivativeEstimationFromDatapointsForConstantInput() {
        //Arrange
        double assertionDelta = 0.0001;
        Double td1 = null;
        double x1 = 1.0, x2 = 1.0, x3 = 1.0, expectedXDotDot = 0.0, expectedXDot = 0.0, td2 = 1.0, td3 = 1.0;

        ScalarDerivativeEstimator estimator = new ScalarDerivativeEstimator(2);

        //Act
        estimator.advance(new Double[]{x1, null, null}, td1);
        estimator.advance(new Double[]{x2, null, null}, td2);
        estimator.advance(new Double[]{x3, null, null}, td3);
        Double xDot = estimator.getFirstDerivative();
        Double xDotDot = estimator.getSecondDerivative();

        //Assert
        Assertions.assertEquals(expectedXDot, xDot, assertionDelta, "xdot must be 0");
        Assertions.assertEquals(expectedXDotDot, xDotDot, assertionDelta, "xdotdot must be 0");
    }

    @Test
    public void testDerivativeEstimationFromDatapointsForStraightLineInput() {
        //Arrange
        double assertionDelta = 0.0001;
        Double td1 = null;
        double x1 = 0.0, x2 = 1.0, x3 = 7.3, expectedXDotDot = 0.0, expectedXDot = 1.0, td2 = 1.0, td3 = 6.3;

        ScalarDerivativeEstimator estimator = new ScalarDerivativeEstimator(2);

        //Act
        estimator.advance(new Double[]{x1, null, null}, td1);
        estimator.advance(new Double[]{x2, null, null}, td2);
        estimator.advance(new Double[]{x3, null, null}, td3);
        Double xDot = estimator.getFirstDerivative();
        Double xDotDot = estimator.getSecondDerivative();

        //Assert
        Assertions.assertEquals(expectedXDot, xDot, assertionDelta, "xdot must be 1.0");
        Assertions.assertEquals(expectedXDotDot, xDotDot, assertionDelta, "xdotdot must be 0");
    }

    @Test
    public void testDerivativeEstimationFromDatapointsForParabolaInput() {
        //Arrange
        double assertionDelta = 0.0001;
        Double td1 = null;
        double x1 = 0.0, x2 = 1.0, x3 = 4.0, x4 = 16.0, expectedXDotDot = 2.0, expectedXDot = 8.0, td2 = 1.0, td3 = 1.0, td4 = 2.0;

        ScalarDerivativeEstimator estimator = new ScalarDerivativeEstimator(2);

        //Act
        estimator.advance(new Double[]{x1, null, null}, td1);
        estimator.advance(new Double[]{x2, null, null}, td2);
        estimator.advance(new Double[]{x3, null, null}, td3);
        estimator.advance(new Double[]{x4, null, null}, td4);
        Double xDot = estimator.getFirstDerivative();
        Double xDotDot = estimator.getSecondDerivative();

        //Assert
        Assertions.assertEquals(expectedXDot, xDot, assertionDelta, "xdot must be 8.0");
        Assertions.assertEquals(expectedXDotDot, xDotDot, assertionDelta, "xdotdot must be 2.0");
    }

    @Test
    public void testCalculateDerivatives() {
        //Arrange
        double assertionDelta = 0.0001;
        RealValue x1 = new RealValue(4.0), x2 = new RealValue(9.0), x3 = new RealValue(25.0), expectedXDotDot = new RealValue(2.0), y1 =
                new RealValue(4.0), y2 = new RealValue(9.0), y3 = new RealValue(25.0), expectedYDot = new RealValue(8.0);

        UnsignedIntegerValue providedDerOrderForX = new UnsignedIntegerValue(0);
        UnsignedIntegerValue providedDerOrderForY = new UnsignedIntegerValue(0);
        UnsignedIntegerValue indexOfX = new UnsignedIntegerValue(0);
        UnsignedIntegerValue indexOfY = new UnsignedIntegerValue(2);
        UnsignedIntegerValue derOrderOfX = new UnsignedIntegerValue(2);
        UnsignedIntegerValue derOrderOfY = new UnsignedIntegerValue(1);

        ArrayValue<RealValue> sharedDataStep1 = new ArrayValue<>(Arrays.asList(x1, new RealValue(-1), y1));
        ArrayValue<RealValue> sharedDataStep2 = new ArrayValue<>(Arrays.asList(x2, new RealValue(-1), y2));
        ArrayValue<RealValue> sharedDataStep3 = new ArrayValue<>(Arrays.asList(x3, new RealValue(-1), y3));

        ArrayValue<ArrayValue> sharedDataDerivatives = new ArrayValue<>(
                Arrays.asList(new ArrayValue<Value>(Arrays.asList(new NullValue(), new NullValue())),
                        new ArrayValue<Value>(Arrays.asList(new NullValue(), new NullValue())),
                        new ArrayValue<Value>(Arrays.asList(new NullValue(), new NullValue()))));
        ArrayValue<UnsignedIntegerValue> derivativeOrders = new ArrayValue<>(Arrays.asList(derOrderOfX, derOrderOfY));
        ArrayValue<UnsignedIntegerValue> indicesOfInterest = new ArrayValue<>(Arrays.asList(indexOfX, indexOfY));
        ArrayValue<UnsignedIntegerValue> providedDerivativeOrders = new ArrayValue<>(Arrays.asList(providedDerOrderForX, providedDerOrderForY));

        DerivativeEstimatorValue derivativeEstimatorValue = new DerivativeEstimatorValue();
        FunctionValue.ExternalFunctionValue createFunc = (FunctionValue.ExternalFunctionValue) derivativeEstimatorValue.lookup("create");
        DerivativeEstimatorInstanceValue derEs =
                (DerivativeEstimatorInstanceValue) createFunc.evaluate(indicesOfInterest, derivativeOrders, providedDerivativeOrders).deref();

        RealValue stepSize = new RealValue(1.0);
        FunctionValue.ExternalFunctionValue estimateFunc = (FunctionValue.ExternalFunctionValue) derEs.lookup("estimate");

        //Act
        estimateFunc.evaluate(stepSize, sharedDataStep1, sharedDataDerivatives);
        stepSize = new RealValue(1.0);
        estimateFunc.evaluate(stepSize, sharedDataStep2, sharedDataDerivatives);
        stepSize = new RealValue(2.0);
        estimateFunc.evaluate(stepSize, sharedDataStep3, sharedDataDerivatives);

        RealValue xDotDot = ((RealValue) sharedDataDerivatives.getValues().get(0).getValues().get(1));
        RealValue yDot = ((RealValue) sharedDataDerivatives.getValues().get(2).getValues().get(0));

        //Assert
        Assertions.assertEquals(expectedXDotDot.getValue(), xDotDot.getValue(), assertionDelta, "xdotdot must be 2.0");
        Assertions.assertEquals(expectedYDot.getValue(), yDot.getValue(), assertionDelta, "ydot must be 8.0");
    }

    @Test
    public void testRollBackFromMableInterface() {
        //Arrange
        double assertionDelta = 0.0001;
        RealValue x = new RealValue(1.0), providedXDot = new RealValue(2.0), y = new RealValue(10.0);

        ArrayValue<RealValue> sharedDataStep1 = new ArrayValue<>(Arrays.asList(x, new RealValue(-1), y));

        ArrayValue<ArrayValue> sharedDataDerivatives = new ArrayValue<>(
                Arrays.asList(new ArrayValue<Value>(Arrays.asList(providedXDot, new NullValue())),
                        new ArrayValue<Value>(Arrays.asList(new NullValue(), new NullValue())),
                        new ArrayValue<Value>(Arrays.asList(new NullValue(), new NullValue()))));
        ArrayValue<UnsignedIntegerValue> derivativeOrders = new ArrayValue<>(Arrays.asList(new UnsignedIntegerValue(2), new UnsignedIntegerValue(1)));
        ArrayValue<UnsignedIntegerValue> indicesOfInterest =
                new ArrayValue<>(Arrays.asList(new UnsignedIntegerValue(0), new UnsignedIntegerValue(2)));
        ArrayValue<UnsignedIntegerValue> providedDerivativeOrders =
                new ArrayValue<>(Arrays.asList(new UnsignedIntegerValue(1), new UnsignedIntegerValue(0)));

        DerivativeEstimatorValue derivativeEstimatorValue = new DerivativeEstimatorValue();
        FunctionValue.ExternalFunctionValue createFunc = (FunctionValue.ExternalFunctionValue) derivativeEstimatorValue.lookup("create");
        DerivativeEstimatorInstanceValue derEs =
                (DerivativeEstimatorInstanceValue) createFunc.evaluate(indicesOfInterest, derivativeOrders, providedDerivativeOrders).deref();

        RealValue stepSize = new RealValue(1.0);
        FunctionValue.ExternalFunctionValue estimateFunc = (FunctionValue.ExternalFunctionValue) derEs.lookup("estimate");
        estimateFunc.evaluate(stepSize, sharedDataStep1, sharedDataDerivatives);

        FunctionValue.ExternalFunctionValue rollbackFunc = (FunctionValue.ExternalFunctionValue) derEs.lookup("rollback");

        //Act
        rollbackFunc.evaluate(sharedDataDerivatives);

        Value xDotDot = (Value) sharedDataDerivatives.getValues().get(0).getValues().get(1);
        Value yDot = (Value) sharedDataDerivatives.getValues().get(2).getValues().get(0);
        RealValue xDot = (RealValue) sharedDataDerivatives.getValues().get(0).getValues().get(0);

        //Assert
        Assertions.assertTrue(yDot instanceof NullValue, "yDot should be null");
        Assertions.assertTrue(xDotDot instanceof NullValue, "xDotDot should be null");
        Assertions.assertEquals(providedXDot.getValue(), xDot.getValue(), assertionDelta, "xDot should be 2.0");
    }
}
