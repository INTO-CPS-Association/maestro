package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.ValueFmi2Api;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class VariableFmi2Api<V> implements Fmi2Builder.Variable<PStm, V>, IndexedVariableFmi2Api<V> {

    private final PStateDesignator designator;
    private final PExp referenceExp;
    /**
     * The declaration which is added in the scope block
     */
    private final PStm declaration;
    PType type;
    Fmi2Builder.DynamicActiveScope<PStm> dynamicScope;
    IMablScope declaredScope;

    public VariableFmi2Api(PStm declaration, PType type, IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp) {
        this.declaration = declaration;
        this.declaredScope = declaredScope;
        this.dynamicScope = dynamicScope;
        this.designator = designator;
        this.referenceExp = referenceExp;

        this.type = type;
    }

    protected PStateDesignator getDesignator() {
        return designator;
    }

    protected PExp getReferenceExp() {
        return referenceExp;
    }

    @Override
    public String getName() {
        return this.referenceExp + "";
    }

    @Override
    public void setValue(V value) {
        setValue(value, dynamicScope);
    }

    @Override
    public void setValue(Fmi2Builder.Variable<PStm, V> variable) {
        throw new RuntimeException("setValue has not been implemented");
    }

    @Override
    public void setValue(V value, Fmi2Builder.Scope<PStm> scope) {
        if (!(value instanceof ValueFmi2Api) || ((ValueFmi2Api<?>) value).get() == null) {
            throw new IllegalArgumentException();
        }

        ValueFmi2Api<V> v = (ValueFmi2Api<V>) value;

        PExp exp = null;

        if (v.getType() instanceof ARealNumericPrimitiveType) {
            exp = newARealLiteralExp((Double) v.get());

        } else if (v.getType() instanceof AIntNumericPrimitiveType) {
            exp = newAIntLiteralExp((Integer) v.get());

        } else if (v.getType() instanceof ABooleanPrimitiveType) {
            exp = newABoolLiteralExp((Boolean) v.get());
        } else if (v.getType() instanceof AStringPrimitiveType) {
            exp = newAStringLiteralExp((String) v.get());
        }

        scope.add(MableAstFactory.newAAssignmentStm(this.designator.clone(), exp));
    }

    @Override
    public IMablScope getDeclaredScope() {
        return declaredScope;
    }

    @Override
    public PStm getDeclaringStm() {
        return declaration;
    }
}