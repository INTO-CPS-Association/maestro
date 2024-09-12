package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.ast.node.AIntNumericPrimitiveType;
import org.intocps.maestro.ast.node.ALongNumericPrimitiveType;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.NumericExpressionValueFmi2Api;

import static org.intocps.maestro.ast.MableAstFactory.*;


public class LongExpressionValue extends NumericExpressionValueFmi2Api implements FmiBuilder.LongExpressionValue {

    final PType type = new ALongNumericPrimitiveType();
    final PExp exp;

    public LongExpressionValue(PExp exp) {
        this.exp = exp;
    }

    public LongExpressionValue(long value) {
        this.exp = newALongLiteralExp(value);
    }

    public static LongExpressionValue of(int i) {
        return new LongExpressionValue(newAIntLiteralExp(i));
    }

    @Override
    public PExp getExp() {
        return this.exp.clone();
    }

    @Override
    public PType getType() {
        return this.type;
    }

    @Override
    public LongExpressionValue subtraction(int v) {
        return new LongExpressionValue(newMinusExp(getExp(), newAIntLiteralExp(v)));
    }

    @Override
    public LongExpressionValue addition(int v) {
        return new LongExpressionValue(newPlusExp(getExp(), newAIntLiteralExp(v)));
    }

    // TODO: This one is tricky. And int divided by and int, should that be a double or an int? It depends on the target variable.
    // Not considered for now.
    @Override
    public DoubleExpressionValue divide(int v) {
        return new DoubleExpressionValue(newDivideExp(getExp(), newAIntLiteralExp(v)));
    }

    @Override
    public LongExpressionValue multiply(int v) {
        return new LongExpressionValue(newMultiplyExp(getExp(), newAIntLiteralExp(v)));
    }

    @Override
    public DoubleExpressionValue subtraction(double v) {
        return new DoubleExpressionValue(newMinusExp(getExp(), newARealLiteralExp(v)));
    }

    @Override
    public DoubleExpressionValue addition(double v) {
        return new DoubleExpressionValue(newPlusExp(getExp(), newARealLiteralExp(v)));
    }

    @Override
    public DoubleExpressionValue divide(double v) {
        return new DoubleExpressionValue(newDivideExp(getExp(), newARealLiteralExp(v)));
    }

    @Override
    public DoubleExpressionValue multiply(double v) {
        return new DoubleExpressionValue(newMultiplyExp(getExp(), newARealLiteralExp(v)));
    }

    @Override
    public NumericExpressionValueFmi2Api addition(FmiBuilder.NumericTypedReferenceExp v) {
        if (v instanceof DoubleExpressionValue) {
            return new DoubleExpressionValue(newPlusExp(this.getExp(), v.getExp()));
        } else if (v instanceof LongExpressionValue) {
            return new LongExpressionValue(newPlusExp(this.getExp(), v.getExp()));
        } else {
            throw new RuntimeException(v + " is not of type NumericExpressionValue.");
        }
    }

    @Override
    public NumericExpressionValueFmi2Api divide(FmiBuilder.NumericTypedReferenceExp v) {
        if (v instanceof DoubleExpressionValue || v instanceof LongExpressionValue) {
            return new DoubleExpressionValue(newDivideExp(this.getExp(), v.getExp()));
        } else {
            throw new RuntimeException(v + " is not of type IntExpressionValue nor DoubleExpressionValue.");
        }
    }

    @Override
    public NumericExpressionValueFmi2Api subtraction(FmiBuilder.NumericTypedReferenceExp v) {
        if (v instanceof DoubleExpressionValue) {
            return new DoubleExpressionValue(newMinusExp(this.getExp(), v.getExp()));
        } else if (v instanceof LongExpressionValue) {
            return new LongExpressionValue(newMinusExp(this.getExp(), v.getExp()));
        } else {
            throw new RuntimeException(v + " is not of type IntExpressionValue nor DoubleExpressionValue.");
        }
    }

    @Override
    public NumericExpressionValueFmi2Api multiply(FmiBuilder.NumericTypedReferenceExp v) {
        if (v instanceof DoubleExpressionValue) {
            return new DoubleExpressionValue(newMultiplyExp(this.getExp(), v.getExp()));
        } else if (v instanceof LongExpressionValue) {
            return new LongExpressionValue(newMultiplyExp(this.getExp(), v.getExp()));
        } else {
            throw new RuntimeException(v + " is not of type IntExpressionValue nor DoubleExpressionValue.");

        }
    }
}
