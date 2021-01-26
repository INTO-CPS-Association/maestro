package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class IntVariableFmi2Api extends VariableFmi2Api<Fmi2Builder.IntValue> implements Fmi2Builder.IntVariable<PStm> {
    public IntVariableFmi2Api(PStm declaration, IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp) {
        super(declaration, newARealNumericPrimitiveType(), declaredScope, dynamicScope, designator, referenceExp);
    }


    @Override
    public void decrement() {
        this.decrement(dynamicScope);
    }

    public void decrement(Fmi2Builder.Scope<PStm> scope) {
        scope.add(newAAssignmentStm(this.getDesignator(), newMinusExp(this.getReferenceExp(), newAIntLiteralExp(1))));
    }

    @Override
    public void increment() {
        this.increment(dynamicScope);
    }

    public IntExpressionValue toMath() {
        return new IntExpressionValue(this.getReferenceExp().clone());
    }

    public void setValue(IntExpressionValue addition) {
        declaredScope.add(newAAssignmentStm(this.getDesignator(), addition.getExp()));
    }

    public void increment(Fmi2Builder.Scope<PStm> scope) {
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
}
