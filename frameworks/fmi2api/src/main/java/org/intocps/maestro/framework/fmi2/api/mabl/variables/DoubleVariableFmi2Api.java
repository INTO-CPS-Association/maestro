package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.DoubleExpressionValue;

import static org.intocps.maestro.ast.MableAstFactory.newARealNumericPrimitiveType;

public class DoubleVariableFmi2Api extends VariableFmi2Api<FmiBuilder.DoubleExpressionValue> implements FmiBuilder.DoubleVariable<PStm> {
    public DoubleVariableFmi2Api(PStm declaration, IMablScope declaredScope, FmiBuilder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp) {
        super(declaration, newARealNumericPrimitiveType(), declaredScope, dynamicScope, designator, referenceExp);
    }


    @Override
    public void set(Double value) {
        super.setValue(DoubleExpressionValue.of(value));
    }


    @Override
    public void setValue(FmiBuilder.DoubleExpressionValue value) {
        super.setValue(value.getExp());
    }


    @Override
    public DoubleExpressionValue toMath() {
        return new DoubleExpressionValue(this.getExp());
    }


    @Override
    public PType getType() {
        return newARealNumericPrimitiveType();
    }

    @Override
    public DoubleVariableFmi2Api clone(PStm declaration, IMablScope declaredScope, PStateDesignator designator, PExp referenceExp) {
        return new DoubleVariableFmi2Api(declaration, declaredScope, dynamicScope, designator, referenceExp);
    }
}
