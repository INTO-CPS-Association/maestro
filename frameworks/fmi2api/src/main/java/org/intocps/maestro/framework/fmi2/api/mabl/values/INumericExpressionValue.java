package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api;

public interface INumericExpressionValue extends Fmi2Builder.ProvidesTypedReferenceExp {
    INumericExpressionValue add(INumericExpressionValue expressionValue);

    INumericExpressionValue add(int i);

    INumericExpressionValue add(double i);

    INumericExpressionValue add(VariableFmi2Api<Fmi2Builder.NumericValue> i);

}
