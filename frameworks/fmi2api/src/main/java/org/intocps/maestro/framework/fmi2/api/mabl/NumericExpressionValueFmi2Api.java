package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder.NumericExpressionValue;

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

    public abstract NumericExpressionValueFmi2Api addition(FmiBuilder.NumericTypedReferenceExp v);

    public abstract NumericExpressionValueFmi2Api divide(FmiBuilder.NumericTypedReferenceExp v);

    public abstract NumericExpressionValueFmi2Api subtraction(FmiBuilder.NumericTypedReferenceExp v);

    public abstract NumericExpressionValueFmi2Api multiply(FmiBuilder.NumericTypedReferenceExp v);

    public PredicateFmi2Api lessThan(FmiBuilder.NumericTypedReferenceExp var) {
        return new PredicateFmi2Api(newALessBinaryExp(getExp(), var.getExp()));
    }

    public PredicateFmi2Api greaterThan(FmiBuilder.NumericTypedReferenceExp var) {
        return new PredicateFmi2Api(newAGreaterBinaryExp(getExp(), var.getExp()));
    }


    public PredicateFmi2Api equalTo(FmiBuilder.NumericTypedReferenceExp var) {
        return new PredicateFmi2Api(newAEqualBinaryExp(getExp(), var.getExp()));
    }


    public PredicateFmi2Api lessEqualTo(FmiBuilder.NumericTypedReferenceExp var) {
        return new PredicateFmi2Api(newALessEqualBinaryExp(getExp(), var.getExp()));
    }


    public PredicateFmi2Api greaterEqualTo(FmiBuilder.NumericTypedReferenceExp var) {
        return new PredicateFmi2Api(newAGreaterEqualBinaryExp(getExp(), var.getExp()));
    }

}
