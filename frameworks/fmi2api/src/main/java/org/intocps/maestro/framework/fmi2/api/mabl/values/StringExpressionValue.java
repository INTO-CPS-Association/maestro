package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;

import static org.intocps.maestro.ast.MableAstFactory.newAStringLiteralExp;
import static org.intocps.maestro.ast.MableAstFactory.newAStringPrimitiveType;

public class StringExpressionValue implements FmiBuilder.ExpressionValue, FmiBuilder.StringExpressionValue {
    private final PExp exp;

    public StringExpressionValue(PExp exp) {
        this.exp = exp;
    }

    public StringExpressionValue(String value) {
        this.exp = newAStringLiteralExp(value);
    }

    public static StringExpressionValue of(String v) {
        return new StringExpressionValue(newAStringLiteralExp(v));
    }

    @Override
    public PType getType() {
        return newAStringPrimitiveType();
    }

    @Override
    public PExp getExp() {
        return this.exp;
    }
}
