import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.derivativeestimator.DerivativeEstimatorInstanceValue;
import org.intocps.maestro.interpreter.values.derivativeestimator.DerivativeEstimatorValue;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DerivativeEstimatorTests {


    @Test
    public void calculateDerivatives() {
        //variablesOfInterest(["x","y"], [2,1], [1,0],2);
        RealValue x = new RealValue(1.0), xdot = new RealValue(2.0), xdotdot = new RealValue(0.0), y = new RealValue(10.0), ydot = new RealValue(0.0);
        ArrayValue<StringValue> variables = new ArrayValue<>(Arrays.asList(new StringValue("x"), new StringValue("y")));
        ArrayValue<IntegerValue> orders = new ArrayValue<>(Arrays.asList(new IntegerValue(2), new IntegerValue(1)));
        ArrayValue<IntegerValue> provided = new ArrayValue<>(Arrays.asList(new IntegerValue(1), new IntegerValue(0)));
        UnsignedIntegerValue size = new UnsignedIntegerValue(2);

        DerivativeEstimatorValue derivativeEstimatorValue = new DerivativeEstimatorValue();
        FunctionValue.ExternalFunctionValue variablesOfInterest =
                (FunctionValue.ExternalFunctionValue) derivativeEstimatorValue.lookup("variablesOfInterest");
        Value evaluate = variablesOfInterest.evaluate(variables, orders, provided, size);
        DerivativeEstimatorInstanceValue deref = (DerivativeEstimatorInstanceValue) evaluate.deref();

        //calculate(1.0, [1.0,2.0,10.0], array[3])
        RealValue time = new RealValue(1.0);
        ArrayValue<RealValue> values = new ArrayValue<>(Arrays.asList(x, xdot, y));
        ArrayList<RealValue> updateableArray = new ArrayList<>();
        UpdatableValue updatableValue = new UpdatableValue(new ArrayValue<>(updateableArray));
        FunctionValue.ExternalFunctionValue calcuate = (FunctionValue.ExternalFunctionValue) deref.lookup("calculate");
        calcuate.evaluate(time, values, updatableValue);

        List<RealValue> expected = Arrays.asList(xdot, xdotdot, ydot);
        Assert.assertTrue(updateableArray.size() == 3);
        for (int i = 0; i < updateableArray.size(); i++) {
            Assert.assertTrue(expected.get(i).getValue() == updateableArray.get(i).getValue());
        }


    }
}
