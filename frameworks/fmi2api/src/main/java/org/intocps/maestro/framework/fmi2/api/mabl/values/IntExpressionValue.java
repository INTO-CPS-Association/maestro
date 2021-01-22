package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.ast.node.AIntNumericPrimitiveType;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

import static org.intocps.maestro.ast.MableAstFactory.*;


public class IntExpressionValue implements Fmi2Builder.NumericExpressionValue {

    final PType type = new AIntNumericPrimitiveType();
    final PExp exp;

    public IntExpressionValue(PExp exp) {
        this.exp = exp;
    }

    @Override
    public PExp getExp() {
        return this.exp.clone();
    }

    @Override
    public PType getType() {
        return this.type;
    }

    public IntExpressionValue subtraction(int v) {
        return new IntExpressionValue(newMinusExp(getExp(), newAIntLiteralExp(v)));
    }

    public IntExpressionValue addition(int v) {
        return new IntExpressionValue(newPlusExp(getExp(), newAIntLiteralExp(v)));
    }

    // TODO: This one is tricky. And int divided by and int, should that be a double or an int? It depends on the target variable.
    // Not considered for now.
    public DoubleExpressionValue divide(int v) {
        return new DoubleExpressionValue(newDivideExp(getExp(), newAIntLiteralExp(v)));
    }

    public IntExpressionValue divideToInt(int v) {
        return new IntExpressionValue(newDivideExp(getExp(), newAIntLiteralExp(v)));
    }

    public IntExpressionValue multiply(int v) {
        return new IntExpressionValue(newMultiplyExp(getExp(), newAIntLiteralExp(v)));
    }

    public DoubleExpressionValue subtraction(double v) {
        return new DoubleExpressionValue(newMinusExp(getExp(), newARealLiteralExp(v)));
    }

    public DoubleExpressionValue addition(double v) {
        return new DoubleExpressionValue(newPlusExp(getExp(), newARealLiteralExp(v)));
    }

    public DoubleExpressionValue divide(double v) {
        return new DoubleExpressionValue(newDivideExp(getExp(), newARealLiteralExp(v)));
    }

    public DoubleExpressionValue multiply(double v) {
        return new DoubleExpressionValue(newMultiplyExp(getExp(), newARealLiteralExp(v)));
    }

    public IntExpressionValue addition(IntExpressionValue v) {
        return new IntExpressionValue(newPlusExp(getExp(), v.getExp()));
    }

    public DoubleExpressionValue divide(DoubleExpressionValue v) {
        return new DoubleExpressionValue(newDivideExp(getExp(), v.getExp()));
    }

    public IntExpressionValue divide(IntExpressionValue v) {
        return new IntExpressionValue(newDivideExp(getExp(), v.getExp()));
    }

    public IntExpressionValue subtraction(IntExpressionValue v) {
        return new IntExpressionValue(newMinusExp(getExp(), v.getExp()));
    }

    public DoubleExpressionValue subtraction(DoubleExpressionValue v) {
        return new DoubleExpressionValue(newMinusExp(getExp(), v.getExp()));
    }

    public IntExpressionValue multiply(IntExpressionValue v) {
        return new IntExpressionValue(newMultiplyExp(getExp(), v.getExp()));
    }

    public DoubleExpressionValue multiply(DoubleExpressionValue v) {
        return new DoubleExpressionValue(newMultiplyExp(getExp(), v.getExp()));
    }

    public IntExpressionValue addition(Fmi2Builder.IntVariable<PStm> d) {
        return new IntExpressionValue(newPlusExp(getExp(), d.getExp()));
    }
}
