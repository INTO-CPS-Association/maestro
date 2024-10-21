package org.intocps.maestro.framework.fmi2.api.mabl.values;

import org.intocps.maestro.ast.node.AFloatNumericPrimitiveType;
import org.intocps.maestro.ast.node.ARealNumericPrimitiveType;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.NumericExpressionValueFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.PredicateFmi2Api;

import static org.intocps.maestro.ast.MableAstFactory.*;


public class FloatExpressionValue extends NumericExpressionValueFmi2Api implements FmiBuilder.FloatExpressionValue {

    final PType type = new AFloatNumericPrimitiveType();
    final PExp exp;

    public FloatExpressionValue(float value) {
        this.exp = newAFloatLiteralExp(value);
    }

    public FloatExpressionValue(PExp exp) {
        this.exp = exp;
    }

    public static FloatExpressionValue of(float value) {
        return new FloatExpressionValue(value);
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
    public FloatExpressionValue subtraction(int v) {
        return new FloatExpressionValue(newMinusExp(getExp(), newAIntLiteralExp(v)));
    }

    @Override
    public FloatExpressionValue addition(int v) {
        return new FloatExpressionValue(newPlusExp(getExp(), newAIntLiteralExp(v)));
    }

    @Override
    public FloatExpressionValue divide(int v) {
        return new FloatExpressionValue(newDivideExp(getExp(), newAIntLiteralExp(v)));
    }

    @Override
    public FloatExpressionValue multiply(int v) {
        return new FloatExpressionValue(newMultiplyExp(getExp(), newAIntLiteralExp(v)));
    }


    @Override
    public FloatExpressionValue subtraction(double v) {
        return new FloatExpressionValue(newMinusExp(getExp(), newARealLiteralExp(v)));
    }

    @Override
    public FloatExpressionValue addition(double v) {
        return new FloatExpressionValue(newPlusExp(getExp(), newARealLiteralExp(v)));
    }

    @Override
    public FloatExpressionValue divide(double v) {
        return new FloatExpressionValue(newDivideExp(getExp(), newARealLiteralExp(v)));
    }

    @Override
    public FloatExpressionValue multiply(double v) {
        return new FloatExpressionValue(newMultiplyExp(getExp(), newARealLiteralExp(v)));
    }

    @Override
    public FloatExpressionValue addition(FmiBuilder.NumericTypedReferenceExp v) {
        return new FloatExpressionValue(newPlusExp(getExp(), v.getExp()));
    }

    @Override
    public FloatExpressionValue divide(FmiBuilder.NumericTypedReferenceExp v) {
        return new FloatExpressionValue(newDivideExp(getExp(), v.getExp()));
    }

    @Override
    public FloatExpressionValue subtraction(FmiBuilder.NumericTypedReferenceExp v) {
        return new FloatExpressionValue(newMinusExp(getExp(), v.getExp()));
    }

    @Override
    public FloatExpressionValue multiply(FmiBuilder.NumericTypedReferenceExp v) {
        return new FloatExpressionValue(newMultiplyExp(getExp(), v.getExp()));
    }

    @Override
    public PredicateFmi2Api lessThan(FmiBuilder.NumericTypedReferenceExp endTimeVar) {
        return new PredicateFmi2Api(newALessBinaryExp(getExp(), endTimeVar.getExp()));
    }
}
