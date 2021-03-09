package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableUtil;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class PredicateFmi2Api implements Fmi2Builder.Predicate, Fmi2Builder.ProvidesTypedReferenceExp {

    private final PExp exp;

    public PredicateFmi2Api(PExp exp) {
        this.exp = exp;
    }


    public PredicateFmi2Api and(PredicateFmi2Api p) {
        return this.and(p.getExp());
    }


    public PredicateFmi2Api or(PredicateFmi2Api p) {
        return new PredicateFmi2Api(newOr(this.getExp(), p.getExp()));
    }

    @Override
    public PredicateFmi2Api and(Fmi2Builder.Predicate p) {
        if (p instanceof PredicateFmi2Api) {
            return this.and((PredicateFmi2Api) p);
        } else {
            throw new RuntimeException("Predicate has to be of type PredicateFmi2Api. Unknown predicate: " + p.getClass());
        }
    }

    @Override
    public PredicateFmi2Api or(Fmi2Builder.Predicate p) {
        if (p instanceof PredicateFmi2Api) {
            return this.or((PredicateFmi2Api) p);
        } else {
            throw new RuntimeException("Predicate has to be of type PredicateFmi2Api. Unknown predicate: " + p.getClass());
        }
    }

    @Override
    public PredicateFmi2Api not() {
        return new PredicateFmi2Api(newNot(getExp()));
    }

    @Override
    public PType getType() {
        return newABoleanPrimitiveType();
    }

    @Override
    public PExp getExp() {
        return this.exp.clone();
    }

    public PredicateFmi2Api and(PExp pexp) {
        return new PredicateFmi2Api(newAnd(getExp(), pexp));
    }

    public PredicateFmi2Api and(BooleanVariableFmi2Api booleanVariable) {
        return this.and(VariableUtil.getAsExp(booleanVariable));
    }
}
