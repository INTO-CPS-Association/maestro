package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.AExpressionStm;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;

import java.util.Arrays;
import java.util.Collections;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class RealTime {

    private final DynamicActiveBuilderScope dynamicScope;
    private final MablApiBuilder mablApiBuilder;
    private String moduleIdentifier;


    public RealTime(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder) {
        this.dynamicScope = dynamicScope;
        this.mablApiBuilder = mablApiBuilder;
        this.moduleIdentifier = "realTime";
    }

    public RealTime(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder, Fmi2Builder.RuntimeModule<PStm> runtimeModule) {
        this(dynamicScope, mablApiBuilder);
        this.moduleIdentifier = runtimeModule.getName();
    }


    public String getModuleIdentifier() {
        return moduleIdentifier;
    }

    public void unload() {
        mablApiBuilder.getDynamicScope().add(newExpressionStm(newUnloadExp(Collections.singletonList(getReferenceExp().clone()))));
    }

    private PExp getReferenceExp() {
        return newAIdentifierExp(moduleIdentifier);
    }

    public DoubleVariableFmi2Api getRealTime() {
        PStm targetVarStm;

        String variableName = dynamicScope.getName("realTime");
        final String FUNCTION_GETREALTIME = "getRealTime";
        targetVarStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(variableName), newARealNumericPrimitiveType(),
                newAExpInitializer(newACallExp(newAIdentifierExp(this.getModuleIdentifier()), newAIdentifier(FUNCTION_GETREALTIME),
                        Collections.emptyList()))));

        this.dynamicScope.add(targetVarStm);

        return new DoubleVariableFmi2Api(targetVarStm, dynamicScope.getActiveScope(), dynamicScope,
                newAIdentifierStateDesignator(newAIdentifier(variableName)), newAIdentifierExp(variableName));
    }

    public void sleep(DoubleVariableFmi2Api sleepTime) {
        final String FUNCTION_SLEEP = "sleep";
        AExpressionStm stm = MableAstFactory.newExpressionStm(MableAstFactory
                .newACallExp(MableAstFactory.newAIdentifierExp(this.moduleIdentifier),
                        MableAstFactory.newAIdentifier(FUNCTION_SLEEP), Collections.singletonList(sleepTime.getReferenceExp().clone())));
        this.dynamicScope.add(stm);
    }

}
