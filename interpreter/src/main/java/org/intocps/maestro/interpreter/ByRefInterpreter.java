package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.AArrayIndexExp;
import org.intocps.maestro.interpreter.values.ArrayValue;
import org.intocps.maestro.interpreter.values.NumericValue;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.utilities.ArrayUpdatableValue;

import java.util.List;
import java.util.stream.Collectors;

class ByRefInterpreter extends Interpreter {

    public ByRefInterpreter(IExternalValueFactory loadFactory) {
        super(loadFactory);
    }

    @Override
    protected Value getInnerArrayValue(ArrayValue<Value> arrayValue, List<NumericValue> indices) {
        if (indices.size() > 2) {
            return getInnerArrayValue((ArrayValue<Value>) arrayValue.getValues().get(indices.get(0).intValue()).deref(),
                    indices.subList(1, indices.size()));
        } else {
            return new ArrayUpdatableValue(arrayValue, indices.get(0).intValue());
        }
        //                        return arrayValue.getValues().get(indices.get(0).intValue());

    }

    @Override
    public Value caseAArrayIndexExp(AArrayIndexExp node, Context question) throws AnalysisException {
        Value value = node.getArray().apply(this, question).deref();

        if (value instanceof ArrayValue) {

            List<NumericValue> indices =
                    evaluate(node.getIndices(), question).stream().map(Value::deref).map(NumericValue.class::cast).collect(Collectors.toList());

            return getInnerArrayValue((ArrayValue) value, indices);
        }
        throw new AnalysisException("No array or index for: " + node);
    }
}
