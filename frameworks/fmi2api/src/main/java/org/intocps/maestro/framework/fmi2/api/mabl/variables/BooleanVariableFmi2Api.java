package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.PredicateFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;

import static org.intocps.maestro.ast.MableAstFactory.newARealNumericPrimitiveType;

public class BooleanVariableFmi2Api extends VariableFmi2Api<Fmi2Builder.BoolValue> implements Fmi2Builder.BoolVariable<PStm> {

    private final PredicateFmi2Api predicate;

    public BooleanVariableFmi2Api(PStm declaration, IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp) {
        super(declaration, newARealNumericPrimitiveType(), declaredScope, dynamicScope, designator, referenceExp);
        this.predicate = new PredicateFmi2Api(referenceExp);
    }

    @Override
    public PredicateFmi2Api toPredicate() {
        return this.predicate;
    }
}
