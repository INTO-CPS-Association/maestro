package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.NumericExpressionValueFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.DoubleExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.ValueFmi2Api;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class VariableFmi2Api<V> implements Fmi2Builder.Variable<PStm, V>, IndexedVariableFmi2Api<V>, Fmi2Builder.ProvidesTypedReferenceExp {

    private final PStateDesignator designator;
    private final PExp referenceExp;
    /**
     * The declaration which is added in the scope block
     */
    private final PStm declaration;
    protected PType type;
    protected Fmi2Builder.DynamicActiveScope<PStm> dynamicScope;
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

    public PExp getReferenceExp() {
        return referenceExp;
    }

    @Override
    public String getName() {
        return this.referenceExp + "";
    }

    @Override
    public void setValue(V value) {
        setValue(dynamicScope, value);
    }

    @Override
    public void setValue(Fmi2Builder.Variable<PStm, V> variable) {
        setValue(dynamicScope, variable);
    }

    @Override
    public void setValue(Fmi2Builder.Scope<PStm> scope, Fmi2Builder.Variable<PStm, V> variable) {
        //TODO use  BuilderUtil.createTypeConvertingAssignment(bui)
        scope.add(newAAssignmentStm(this.designator.clone(), ((VariableFmi2Api<V>) variable).getReferenceExp().clone()));
    }

    @Override
    public void setValue(Fmi2Builder.Scope<PStm> scope, V value) {
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


    @Override
    public PType getType() {
        return this.type;
    }


    @Override
    public PExp getExp() {
        return this.referenceExp.clone();
    }

    protected void setValue(Fmi2Builder.Scope<PStm> scope, PExp exp) {
        scope.add(MableAstFactory.newAAssignmentStm(this.designator.clone(), exp));
    }

    protected void setValue(PExp exp) {
        this.setValue(dynamicScope, exp);
    }

    public VariableFmi2Api<V> clone(PStm declaration, IMablScope declaredScope, PStateDesignator designator, PExp referenceExp) {
        return new VariableFmi2Api<>(declaration, type, declaredScope, dynamicScope, designator, referenceExp);
    }


    public NumericExpressionValueFmi2Api toMath() {
        if (this instanceof DoubleVariableFmi2Api) {
            return new DoubleExpressionValue(this.getExp());
        } else if (this instanceof IntVariableFmi2Api) {
            return new IntExpressionValue(this.getExp());
        } else {
            throw new RuntimeException("Variable is not of Numeric Type but of type: " + this.getClass());
        }
    }
}