package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.AMablValue;

import static org.intocps.maestro.ast.MableAstFactory.newRealType;

public class MablDoubleValue extends AMablValue<Double> implements Fmi2Builder.DoubleValue {

    public MablDoubleValue(Double value) {
        super(newRealType(), value);
    }
}
