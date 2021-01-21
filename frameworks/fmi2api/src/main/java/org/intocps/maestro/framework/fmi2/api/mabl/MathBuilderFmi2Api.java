package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;

public class MathBuilderFmi2Api {

    private final DynamicActiveBuilderScope dynamicScope;
    private final MablApiBuilder builder;

    public MathBuilderFmi2Api(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder) {
        this.dynamicScope = dynamicScope;
        this.builder = mablApiBuilder;

    }
/*
    public static Fmi2Builder.ProvidesReferenceExp add(Fmi2Builder.ProvidesReferenceExp left, Fmi2Builder.ProvidesReferenceExp right) {
        return new ExpFmi2Api(newPlusExp(left.getReferenceExp(), right.getReferenceExp()));
    }*/
/*
    public Fmi2Builder.BoolVariable checkConvergence(VariableFmi2Api a, VariableFmi2Api b, VariableFmi2Api absoluteTolerance,
            VariableFmi2Api relativeTolerance) {
        String variableName = dynamicScope.getName("convergence");

        PStm stm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(variableName), newABoleanPrimitiveType(), newAExpInitializer(

                newACallExp(newAIdentifierExp("math"), newAIdentifier("isClose"),
                        Arrays.asList(a.getReferenceExp(), b.getReferenceExp(), absoluteTolerance.getReferenceExp(),
                                relativeTolerance.getReferenceExp())))));
        dynamicScope.add(stm);
        return new BooleanVariableFmi2Api(stm, dynamicScope.getActiveScope(), dynamicScope,
                newAIdentifierStateDesignator(newAIdentifier(variableName)), newAIdentifierExp(variableName));

    }

    public Fmi2Builder.BoolVariable checkConvergence(Fmi2Builder.Variable<PStm, Object> a, VariableFmi2Api b,
            Fmi2Builder.DoubleVariable<PStm> absoluteTolerance, Fmi2Builder.DoubleVariable<PStm> relativeTolerance) {
        if (a instanceof VariableFmi2Api && b instanceof VariableFmi2Api && relativeTolerance instanceof VariableFmi2Api &&
                absoluteTolerance instanceof VariableFmi2Api) {
            return this.checkConvergence((VariableFmi2Api) a, (VariableFmi2Api) b, (VariableFmi2Api) absoluteTolerance,
                    (VariableFmi2Api) relativeTolerance);
        }
        throw new RuntimeException("Invalid arguments to checkConvergence");
    }*/
}
