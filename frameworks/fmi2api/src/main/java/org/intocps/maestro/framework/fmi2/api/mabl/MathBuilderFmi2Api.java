package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.ARealNumericPrimitiveType;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ArrayVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class MathBuilderFmi2Api {

    //    private static final String DEFAULT_MODULE_IDENTIFIER = "math";
    private static final String FUNCTION_IS_CLOSE = "isClose";
    private static final String FUNCTION_MINREALFROMARRAY = "minRealFromArray";
    private final DynamicActiveBuilderScope dynamicScope;
    private final PExp referenceExp;

    //FIXME this class is made in a wrong way. It is actually a value of a runtime module but the class is completely custom and does not use the
    // remove
    // api. It should also be placed in the variables package
    public MathBuilderFmi2Api(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder, PExp referenceExp) {
        this.dynamicScope = dynamicScope;
        this.referenceExp = referenceExp;
    }


    private BooleanVariableFmi2Api checkConvergenceInternal(FmiBuilder.ProvidesTypedReferenceExp a, FmiBuilder.ProvidesTypedReferenceExp b,
            FmiBuilder.ProvidesTypedReferenceExp absoluteTolerance, FmiBuilder.ProvidesTypedReferenceExp relativeTolerance) {
        String variableName = dynamicScope.getName("convergence");

        PStm stm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(variableName), newABoleanPrimitiveType(), newAExpInitializer(
                newACallExp(this.referenceExp.clone(), newAIdentifier(FUNCTION_IS_CLOSE),
                        Arrays.asList(a.getExp(), b.getExp(), absoluteTolerance.getExp(), relativeTolerance.getExp())))));
        dynamicScope.add(stm);
        return new BooleanVariableFmi2Api(stm, dynamicScope.getActiveScope(), dynamicScope,
                newAIdentifierStateDesignator(newAIdentifier(variableName)), newAIdentifierExp(variableName));

    }

    public BooleanVariableFmi2Api checkConvergence(FmiBuilder.ProvidesTypedReferenceExp a, FmiBuilder.ProvidesTypedReferenceExp b,
            FmiBuilder.DoubleVariable<PStm> absoluteTolerance, FmiBuilder.DoubleVariable<PStm> relativeTolerance) {
        if (Stream.of(a, b, absoluteTolerance, relativeTolerance).allMatch(x -> x.getType() instanceof ARealNumericPrimitiveType)) {
            return this.checkConvergenceInternal(a, b, absoluteTolerance, relativeTolerance);
        } else {
            throw new RuntimeException("Invalid arguments to checkConvergence");
        }
    }


    public DoubleVariableFmi2Api minRealFromArray(ArrayVariableFmi2Api<Double> array) {
        String variableName = dynamicScope.getName("minVal");
        PStm stm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(variableName), newARealNumericPrimitiveType(), newAExpInitializer(
                newACallExp(this.referenceExp.clone(), newAIdentifier(FUNCTION_MINREALFROMARRAY), Collections.singletonList(array.getExp())))));

        dynamicScope.add(stm);
        return new DoubleVariableFmi2Api(stm, dynamicScope.getActiveScope(), dynamicScope,
                newAIdentifierStateDesignator(newAIdentifier(variableName)), newAIdentifierExp(variableName));

    }
}
