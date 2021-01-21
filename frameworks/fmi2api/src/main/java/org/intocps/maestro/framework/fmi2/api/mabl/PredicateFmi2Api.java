package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableUtil;

import static org.intocps.maestro.ast.MableAstFactory.newAnd;

public class PredicateFmi2Api implements Fmi2Builder.LogicBuilder.Predicate {

    private final PExp exp;

    public PredicateFmi2Api(PExp referenceExp) {
        this.exp = referenceExp;
    }

    @Override
    public Fmi2Builder.LogicBuilder.Predicate and(Fmi2Builder.LogicBuilder.Predicate p) {
        return this.and(p.getExp());
    }

    @Override
    public Fmi2Builder.LogicBuilder.Predicate or(Fmi2Builder.LogicBuilder.Predicate p) {
        return null;
    }

    @Override
    public Fmi2Builder.LogicBuilder.Predicate not() {
        return null;
    }

    @Override
    public PExp getExp() {
        return this.exp;
    }

    public PredicateFmi2Api and(PExp pexp) {
        return new PredicateFmi2Api(newAnd(exp.clone(), pexp));
    }

    @Override
    public PredicateFmi2Api and(BooleanVariableFmi2Api booleanVariable) {
        return this.and(VariableUtil.getAsExp(booleanVariable));
    }
}
