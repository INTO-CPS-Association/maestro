package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.LongExpressionValue;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class LongVariableFmi2Api extends VariableFmi2Api<FmiBuilder.LongExpressionValue> implements FmiBuilder.LongVariable<PStm> {
    public LongVariableFmi2Api(PStm declaration, IMablScope declaredScope, FmiBuilder.DynamicActiveScope<PStm> dynamicScope,
                               PStateDesignator designator, PExp referenceExp) {
        super(declaration, newALongNumericPrimitiveType(), declaredScope, dynamicScope, designator, referenceExp);
    }


    @Override
    public void decrement() {
        this.decrement(dynamicScope);
    }

    public void decrement(FmiBuilder.Scope<PStm> scope) {
        scope.add(newAAssignmentStm(this.getDesignator(), newMinusExp(this.getReferenceExp(), newAIntLiteralExp(1))));
    }

    @Override
    public void increment() {
        this.increment(dynamicScope);
    }

    @Override
    public LongExpressionValue toMath() {
        return new LongExpressionValue(this.getReferenceExp().clone());
    }

    @Override
    public void setValue(FmiBuilder.LongExpressionValue addition) {
        declaredScope.add(newAAssignmentStm(this.getDesignator(), addition.getExp()));
    }

    public void increment(FmiBuilder.Scope<PStm> scope) {
        scope.add(newAAssignmentStm(this.getDesignator(), newPlusExp(this.getReferenceExp(), newAIntLiteralExp(1))));
    }

    @Override
    public PType getType() {
        return new AIntNumericPrimitiveType();
    }

    @Override
    public PExp getExp() {
        return this.getReferenceExp();
    }

    @Override
    public LongVariableFmi2Api clone(PStm declaration, IMablScope declaredScope, PStateDesignator designator, PExp referenceExp) {
        return new LongVariableFmi2Api(declaration, declaredScope, dynamicScope, designator, referenceExp);
    }
}
