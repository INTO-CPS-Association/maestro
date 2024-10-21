package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.FloatExpressionValue;

import static org.intocps.maestro.ast.MableAstFactory.newAFloatNumericPrimitiveType;

public class FloatVariableFmi2Api extends VariableFmi2Api<FmiBuilder.FloatExpressionValue> implements FmiBuilder.FloatVariable<PStm> {
    public FloatVariableFmi2Api(PStm declaration, IMablScope declaredScope, FmiBuilder.DynamicActiveScope<PStm> dynamicScope,
                                PStateDesignator designator, PExp referenceExp) {
        super(declaration, newAFloatNumericPrimitiveType(), declaredScope, dynamicScope, designator, referenceExp);
    }


    @Override
    public void set(Float value) {
        super.setValue(FloatExpressionValue.of(value));
    }


    @Override
    public void setValue(FmiBuilder.FloatExpressionValue value) {
        super.setValue(value.getExp());
    }


    @Override
    public FloatExpressionValue toMath() {
        return new FloatExpressionValue(this.getExp());
    }


    @Override
    public PType getType() {
        return newAFloatNumericPrimitiveType();
    }

    @Override
    public FloatVariableFmi2Api clone(PStm declaration, IMablScope declaredScope, PStateDesignator designator, PExp referenceExp) {
        return new FloatVariableFmi2Api(declaration, declaredScope, dynamicScope, designator, referenceExp);
    }
}
