package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.BuilderUtil;
import org.intocps.maestro.framework.fmi2.api.mabl.NumericExpressionValueFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.DoubleExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue;

import static org.intocps.maestro.ast.MableAstFactory.newAAssignmentStm;

public class VariableFmi2Api<V> implements FmiBuilder.Variable<PStm, V>, IndexedVariableFmi2Api<V>, FmiBuilder.ProvidesTypedReferenceExp {

    private final PStateDesignator designator;
    private final PExp referenceExp;
    /**
     * The declaration which is added in the scope block
     */
    private final PStm declaration;
    protected PType type;
    protected FmiBuilder.DynamicActiveScope<PStm> dynamicScope;
    IMablScope declaredScope;

    public VariableFmi2Api(PStm declaration, PType type, IMablScope declaredScope, FmiBuilder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp) {
        if (declaration != null && (declaredScope == null || declaredScope.indexOf(declaration) == -1)) {
            throw new IllegalArgumentException("Declared scope is illegal it does not declare the declaration");
        }
        this.declaration = declaration;
        this.declaredScope = declaredScope;
        this.dynamicScope = dynamicScope;
        this.designator = designator;
        this.referenceExp = referenceExp;

        this.type = type;
    }

    public PStateDesignator getDesignator() {
        return designator;
    }

    public PStateDesignator getDesignatorClone() {
        return designator.clone();
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
    public void setValue(FmiBuilder.Variable<PStm, V> variable) {
        setValue(dynamicScope, variable);
    }

    @Override
    public void setValue(FmiBuilder.Scope<PStm> scope, FmiBuilder.Variable<PStm, V> variable) {
        //TODO use  BuilderUtil.createTypeConvertingAssignment(bui)
        scope.add(newAAssignmentStm(this.designator.clone(), ((VariableFmi2Api<V>) variable).getReferenceExp().clone()));
    }

    @Override
    public void setValue(FmiBuilder.Scope<PStm> scope, V value) {
        if (!(value instanceof FmiBuilder.ProvidesTypedReferenceExp)) {
            throw new IllegalArgumentException();
        }

        scope.add(MableAstFactory.newAAssignmentStm(this.designator.clone(), ((FmiBuilder.ProvidesTypedReferenceExp) value).getExp()));
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

    protected void setValue(FmiBuilder.Scope<PStm> scope, PExp exp) {
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

    public void setValue(FmiBuilder.ExpressionValue value) {
        this.setValue(dynamicScope, value);
    }

    private void setValue(FmiBuilder.DynamicActiveScope<PStm> dynamicScope, FmiBuilder.ExpressionValue value) {
        this.dynamicScope.addAll(BuilderUtil.createTypeConvertingAssignment(this.designator, value.getExp(), value.getType(), this.getType()));
    }
}