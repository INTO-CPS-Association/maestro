package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.PredicateFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;

import static org.intocps.maestro.ast.MableAstFactory.newABoleanPrimitiveType;

public class BooleanVariableFmi2Api extends VariableFmi2Api<FmiBuilder.BooleanExpressionValue> implements FmiBuilder.BoolVariable<PStm> {

    private final PredicateFmi2Api predicate;

    public BooleanVariableFmi2Api(PStm declaration, IMablScope declaredScope, FmiBuilder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp) {
        super(declaration, newABoleanPrimitiveType(), declaredScope, dynamicScope, designator, referenceExp);
        this.predicate = new PredicateFmi2Api(referenceExp);
    }

    @Override
    public PredicateFmi2Api toPredicate() {
        return this.predicate;
    }

    @Override
    public BooleanVariableFmi2Api clone(PStm declaration, IMablScope declaredScope, PStateDesignator designator, PExp referenceExp) {
        return new BooleanVariableFmi2Api(declaration, declaredScope, dynamicScope, designator, referenceExp);
    }
}
