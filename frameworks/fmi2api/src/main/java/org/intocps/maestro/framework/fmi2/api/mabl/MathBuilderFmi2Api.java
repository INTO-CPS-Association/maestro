package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.ARealNumericPrimitiveType;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;

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

    private BooleanVariableFmi2Api checkConvergence_(Fmi2Builder.ProvidesTypedReferenceExp a, Fmi2Builder.ProvidesTypedReferenceExp b,
            Fmi2Builder.ProvidesTypedReferenceExp absoluteTolerance, Fmi2Builder.ProvidesTypedReferenceExp relativeTolerance) {
        String variableName = dynamicScope.getName("convergence");

        PStm stm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(variableName), newABoleanPrimitiveType(), newAExpInitializer(

                newACallExp(newAIdentifierExp("math"), newAIdentifier("isClose"),
                        Arrays.asList(a.getExp(), b.getExp(), absoluteTolerance.getExp(), relativeTolerance.getExp())))));
        dynamicScope.add(stm);
        return new BooleanVariableFmi2Api(stm, dynamicScope.getActiveScope(), dynamicScope,
                newAIdentifierStateDesignator(newAIdentifier(variableName)), newAIdentifierExp(variableName));

    }

    public BooleanVariableFmi2Api checkConvergence(Fmi2Builder.ProvidesTypedReferenceExp a, Fmi2Builder.ProvidesTypedReferenceExp b,
            Fmi2Builder.DoubleVariable<PStm> absoluteTolerance, Fmi2Builder.DoubleVariable<PStm> relativeTolerance) {
        if (Stream.of(a, b, absoluteTolerance, relativeTolerance).allMatch(x -> x.getType() instanceof ARealNumericPrimitiveType)) {
            return this.checkConvergence_(a, b, absoluteTolerance, relativeTolerance);
        } else {
            throw new RuntimeException("Invalid arguments to checkConvergence");
        }
    }
}
