package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.ast.node.ARealNumericPrimitiveType;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.PredicateFmi2Api;

import static org.intocps.maestro.ast.MableAstFactory.*;


public class DoubleExpressionValue implements Fmi2Builder.NumericExpressionValue {

    final PType type = new ARealNumericPrimitiveType();
    final PExp exp;

    public DoubleExpressionValue(PExp exp) {
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

    public DoubleExpressionValue subtraction(int v) {
        return new DoubleExpressionValue(newMinusExp(getExp(), newAIntLiteralExp(v)));
    }

    public DoubleExpressionValue addition(int v) {
        return new DoubleExpressionValue(newPlusExp(getExp(), newAIntLiteralExp(v)));
    }

    public DoubleExpressionValue divide(int v) {
        return new DoubleExpressionValue(newDivideExp(getExp(), newAIntLiteralExp(v)));
    }

    public DoubleExpressionValue multiply(int v) {
        return new DoubleExpressionValue(newMultiplyExp(getExp(), newAIntLiteralExp(v)));
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

    public DoubleExpressionValue addition(Fmi2Builder.NumericExpressionValue v) {
        return new DoubleExpressionValue(newPlusExp(getExp(), v.getExp()));
    }

    public DoubleExpressionValue divide(Fmi2Builder.NumericExpressionValue v) {
        return new DoubleExpressionValue(newDivideExp(getExp(), v.getExp()));
    }

    public DoubleExpressionValue subtraction(Fmi2Builder.NumericExpressionValue v) {
        return new DoubleExpressionValue(newMinusExp(getExp(), v.getExp()));
    }

    public DoubleExpressionValue multiply(Fmi2Builder.NumericExpressionValue v) {
        return new DoubleExpressionValue(newMultiplyExp(getExp(), v.getExp()));
    }

    public PredicateFmi2Api lessThan(Fmi2Builder.DoubleVariable<PStm> endTimeVar) {
        return new PredicateFmi2Api(newALessBinaryExp(getExp(), endTimeVar.getExp()));
    }
}
