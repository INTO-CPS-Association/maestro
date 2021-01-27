package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;

import java.util.List;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class BooleanBuilderFmi2Api {
    private static final String BOOLEAN_LOGIC_MODULE_IDENTIFIER = "booleanLogic";
    private static final String ALL_TRUE_FUNCTION_NAME = "allTrue";
    private static final String ALL_FALSE_FUNCTION_NAME = "allFalse";
    private final DynamicActiveBuilderScope dynamicScope;
    private final MablApiBuilder builder;

    public BooleanBuilderFmi2Api(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder) {
        this.dynamicScope = dynamicScope;
        this.builder = mablApiBuilder;
    }

    public BooleanVariableFmi2Api allTrue(String variablePrefix, List<Fmi2Builder.ProvidesTypedReferenceExp> parameters) {
        return allTrueFalse(variablePrefix, parameters, ALL_TRUE_FUNCTION_NAME);
    }

    public BooleanVariableFmi2Api allFalse(String variablePrefix, List<Fmi2Builder.ProvidesTypedReferenceExp> parameters) {
        return allTrueFalse(variablePrefix, parameters, ALL_FALSE_FUNCTION_NAME);
    }

    private BooleanVariableFmi2Api allTrueFalse(String variablePrefix, List<Fmi2Builder.ProvidesTypedReferenceExp> parameters, String function) {
        String variableName = dynamicScope.getName(variablePrefix);

        PStm stm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(variableName), newABoleanPrimitiveType(), newAExpInitializer(
                newACallExp(newAIdentifierExp(BOOLEAN_LOGIC_MODULE_IDENTIFIER), newAIdentifier(function),
                        parameters.stream().map(x -> x.getExp()).collect(Collectors.toList())))));
        dynamicScope.add(stm);
        return new BooleanVariableFmi2Api(stm, dynamicScope.getActiveScope(), dynamicScope,
                newAIdentifierStateDesignator(newAIdentifier(variableName)), newAIdentifierExp(variableName));
    }
}
