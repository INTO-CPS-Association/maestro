package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.MablDoubleValue;

import static org.intocps.maestro.ast.MableAstFactory.newARealNumericPrimitiveType;

public class AMablDoubleVariable extends AMablVariable<Fmi2Builder.DoubleValue> implements Fmi2Builder.DoubleVariable<PStm> {
    public AMablDoubleVariable(PStm declaration, IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp) {
        super(declaration, newARealNumericPrimitiveType(), declaredScope, dynamicScope, designator, referenceExp);
    }

    @Override
    public Fmi2Builder.TimeDeltaValue toTimeDelta() {
        throw new RuntimeException("toTimeDelta has not been implemented");
    }

    @Override
    public void set(Double value) {
        super.setValue(new MablDoubleValue(value));
    }

    @Override
    public Fmi2Builder.DoubleValue plus(Fmi2Builder.DoubleVariable<PStm> stepSizeVar) {
        throw new RuntimeException("plus has not been implemented");
    }

    @Override
    public void setValue(Fmi2Builder.DoubleValue value) {
        super.setValue(value);
    }

    @Override
    public void setValue(Fmi2Builder.Variable<PStm, Fmi2Builder.DoubleValue> variable) {
        AMablDoubleVariable a = (AMablDoubleVariable) variable;
        a.
                // From a variable you have to construct a value.

                super.setValue(variable);
        throw new RuntimeException("setValue has not been implemented");
    }

    @Override
    public void setValue(Fmi2Builder.DoubleValue value, Fmi2Builder.Scope<PStm> scope) {
        super.setValue(value, scope);
    }

    @Override
    public void set(PStm value) {
        throw new RuntimeException("set has not been implemented");
    }

    @Override
    public PStm get() {
        throw new RuntimeException("get has not been implemented");
    }


}
