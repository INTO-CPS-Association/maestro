package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue;

import static org.intocps.maestro.ast.MableAstFactory.*;
public class UIntVariableFmi2Api extends VariableFmi2Api<FmiBuilder.UIntExpressionValue> implements FmiBuilder.UIntVariable<PStm> {
    public UIntVariableFmi2Api(PStm declaration, IMablScope declaredScope, FmiBuilder.DynamicActiveScope<PStm> dynamicScope,
                               PStateDesignator designator, PExp referenceExp) {
        super(declaration, newARealNumericPrimitiveType(), declaredScope, dynamicScope, designator, referenceExp);
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
    public IntExpressionValue toMath() {
        return new IntExpressionValue(this.getReferenceExp().clone());
    }

    @Override
    public void setValue(FmiBuilder.UIntExpressionValue addition) {
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
    public UIntVariableFmi2Api clone(PStm declaration, IMablScope declaredScope, PStateDesignator designator, PExp referenceExp) {
        return new UIntVariableFmi2Api(declaration, declaredScope, dynamicScope, designator, referenceExp);
    }
}
