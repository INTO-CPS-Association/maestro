package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.PredicateFmi2Api;

import static org.intocps.maestro.ast.MableAstFactory.newABoolLiteralExp;

public class BooleanExpressionValue extends PredicateFmi2Api implements Fmi2Builder.ExpressionValue {

    public BooleanExpressionValue(PExp exp) {
        super(exp);
    }

    public BooleanExpressionValue(Boolean value) {
        super(newABoolLiteralExp(value));
    }

    public static BooleanExpressionValue of(boolean v) {
        return new BooleanExpressionValue(newABoolLiteralExp(v));
    }
}
