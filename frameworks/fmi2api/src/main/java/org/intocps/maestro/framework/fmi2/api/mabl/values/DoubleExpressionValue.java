package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.ast.node.ARealNumericPrimitiveType;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

import static org.intocps.maestro.ast.MableAstFactory.*;


public class DoubleExpressionValue implements Fmi2Builder.NumericExpressionValue {

    final PType type = new ARealNumericPrimitiveType();
    final PExp exp;

    public DoubleExpressionValue(PExp exp) {
        this.exp = exp;
    }

    @Override
    public PExp getReferenceExp() {
        return null;
    }

    @Override
    public PType getType() {
        return this.type;
    }

    public DoubleExpressionValue subtraction(double v) {
        return new DoubleExpressionValue(newAParExp(newMinusExp(exp, newARealLiteralExp(v))));
    }

    public DoubleExpressionValue addition(double v) {
        return new DoubleExpressionValue(newAParExp(newPlusExp(exp, newARealLiteralExp(v))));
    }

    public DoubleExpressionValue divide(double v) {
        return new DoubleExpressionValue(newAParExp(newDivideExp(exp, newARealLiteralExp(v))));
    }

    public DoubleExpressionValue multiply(double v) {
        return new DoubleExpressionValue(newAParExp(newMultiplyExp(exp, newARealLiteralExp(v))));
    }

    public DoubleExpressionValue addition(Fmi2Builder.NumericExpressionValue v) {
        return new DoubleExpressionValue(newAParExp(newPlusExp(exp, v.getReferenceExp())));
    }

    public DoubleExpressionValue divide(Fmi2Builder.NumericExpressionValue v) {
        return new DoubleExpressionValue(newAParExp(newDivideExp(exp, v.getReferenceExp())));
    }

    public DoubleExpressionValue subtraction(Fmi2Builder.NumericExpressionValue v) {
        return new DoubleExpressionValue(newAParExp(newMinusExp(exp, v.getReferenceExp())));
    }

    public DoubleExpressionValue multiply(Fmi2Builder.NumericExpressionValue v) {
        return new DoubleExpressionValue(newAParExp(newMultiplyExp(exp, v.getReferenceExp())));
    }
}
