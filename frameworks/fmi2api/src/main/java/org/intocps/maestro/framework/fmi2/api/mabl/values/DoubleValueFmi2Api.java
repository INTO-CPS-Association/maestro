package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

import static org.intocps.maestro.ast.MableAstFactory.newRealType;

public class DoubleValueFmi2Api extends ValueFmi2Api<Double> implements Fmi2Builder.DoubleValue {

    public DoubleValueFmi2Api(Double value) {
        super(newRealType(), value);
    }
}
