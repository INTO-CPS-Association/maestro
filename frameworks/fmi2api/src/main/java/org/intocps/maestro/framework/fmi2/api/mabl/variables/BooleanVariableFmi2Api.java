package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;

import static org.intocps.maestro.ast.MableAstFactory.newARealNumericPrimitiveType;

public class BooleanVariableFmi2Api extends VariableFmi2Api<Fmi2Builder.BoolValue> implements Fmi2Builder.BoolVariable<PStm> {
    public BooleanVariableFmi2Api(PStm declaration, IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp) {
        super(declaration, newARealNumericPrimitiveType(), declaredScope, dynamicScope, designator, referenceExp);
    }


    @Override
    public Fmi2Builder.LogicBuilder.Predicate and(Fmi2Builder.LogicBuilder.Predicate p) {
        throw new RuntimeException("and has not been implemented");
    }

    @Override
    public Fmi2Builder.LogicBuilder.Predicate or(Fmi2Builder.LogicBuilder.Predicate p) {
        throw new RuntimeException("or has not been implemented");
    }

    @Override
    public Fmi2Builder.LogicBuilder.Predicate not() {
        throw new RuntimeException("not has not been implemented");

    }

    @Override
    public void setValue(Fmi2Builder.Variable<PStm, Fmi2Builder.BoolValue> variable) {
        throw new RuntimeException("setValue has not been implemented");
    }
}
