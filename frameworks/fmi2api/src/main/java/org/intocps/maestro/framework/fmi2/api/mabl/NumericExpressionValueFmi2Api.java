package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.framework.fmi2.api.Fmi2Builder.NumericExpressionValue;

import static org.intocps.maestro.ast.MableAstFactory.*;

public abstract class NumericExpressionValueFmi2Api implements NumericExpressionValue {

    public abstract NumericExpressionValueFmi2Api subtraction(int v);

    public abstract NumericExpressionValueFmi2Api addition(int v);

    public abstract NumericExpressionValueFmi2Api divide(int v);

    public abstract NumericExpressionValueFmi2Api multiply(int v);

    public abstract NumericExpressionValueFmi2Api subtraction(double v);

    public abstract NumericExpressionValueFmi2Api addition(double v);

    public abstract NumericExpressionValueFmi2Api divide(double v);

    public abstract NumericExpressionValueFmi2Api multiply(double v);

    public abstract NumericExpressionValueFmi2Api addition(NumericExpressionValue v);

    public abstract NumericExpressionValueFmi2Api divide(NumericExpressionValue v);

    public abstract NumericExpressionValueFmi2Api subtraction(NumericExpressionValue v);

    public abstract NumericExpressionValueFmi2Api multiply(NumericExpressionValue v);

    public PredicateFmi2Api lessThan(NumericExpressionValue var) {
        return new PredicateFmi2Api(newALessBinaryExp(getExp(), var.getExp()));
    }

    public PredicateFmi2Api greaterThan(NumericExpressionValue var) {
        return new PredicateFmi2Api(newAGreaterBinaryExp(getExp(), var.getExp()));
    }


    public PredicateFmi2Api equalTo(NumericExpressionValue var) {
        return new PredicateFmi2Api(newAEqualBinaryExp(getExp(), var.getExp()));
    }


    public PredicateFmi2Api lessEqualTo(NumericExpressionValue var) {
        return new PredicateFmi2Api(newALessEqualBinaryExp(getExp(), var.getExp()));
    }


    public PredicateFmi2Api greaterEqualTo(NumericExpressionValue var) {
        return new PredicateFmi2Api(newAGreaterEqualBinaryExp(getExp(), var.getExp()));
    }

}
