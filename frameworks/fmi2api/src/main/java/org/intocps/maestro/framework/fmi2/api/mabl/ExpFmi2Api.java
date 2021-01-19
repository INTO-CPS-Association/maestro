package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

public class ExpFmi2Api implements Fmi2Builder.ProvidesReferenceExp {
    private final PExp exp;

    public ExpFmi2Api(PExp exp) {
        this.exp = exp;
    }

    @Override
    public PExp getReferenceExp() {
        return this.exp;
    }
}
