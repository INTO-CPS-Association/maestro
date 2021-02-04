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
    private String moduleIdentifier;
    private Fmi2Builder.RuntimeModule<PStm> runtimeModule;
    private boolean runtimeModuleMode = false;


    public BooleanBuilderFmi2Api(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder,
            Fmi2Builder.RuntimeModule<PStm> runtimeModule) {
        this(dynamicScope, mablApiBuilder);
        this.runtimeModuleMode = true;
        this.runtimeModule = runtimeModule;
        this.moduleIdentifier = runtimeModule.getName();
    }

    public BooleanBuilderFmi2Api(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder) {
        this.runtimeModuleMode = false;
        this.dynamicScope = dynamicScope;
        this.builder = mablApiBuilder;
        this.moduleIdentifier = BOOLEAN_LOGIC_MODULE_IDENTIFIER;

    }

    public BooleanVariableFmi2Api allTrue(String variablePrefix, List<? extends Fmi2Builder.ProvidesTypedReferenceExp> parameters) {
        return allTrueFalse(variablePrefix, parameters, ALL_TRUE_FUNCTION_NAME);
    }

    public BooleanVariableFmi2Api allFalse(String variablePrefix, List<? extends Fmi2Builder.ProvidesTypedReferenceExp> parameters) {
        return allTrueFalse(variablePrefix, parameters, ALL_FALSE_FUNCTION_NAME);
    }

    private BooleanVariableFmi2Api allTrueFalse(String variablePrefix, List<? extends Fmi2Builder.ProvidesTypedReferenceExp> parameters,
            String function) {
        String variableName = dynamicScope.getName(variablePrefix);

        PStm stm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(variableName), newABoleanPrimitiveType(), newAExpInitializer(
                newACallExp(newAIdentifierExp(this.moduleIdentifier), newAIdentifier(function),
                        parameters.stream().map(x -> x.getExp()).collect(Collectors.toList())))));
        dynamicScope.add(stm);
        return new BooleanVariableFmi2Api(stm, dynamicScope.getActiveScope(), dynamicScope,
                newAIdentifierStateDesignator(newAIdentifier(variableName)), newAIdentifierExp(variableName));
    }
}
