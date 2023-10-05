package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.AInstanceMappingStm;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.TryMaBlScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.FmuVariableFmi2Api;

import java.util.Arrays;
import java.util.Collections;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class FaultInject {

    private final DynamicActiveBuilderScope dynamicScope;
    private final MablApiBuilder mablApiBuilder;
    private final String FAULTINJECT_POSTFIX = "_m_fi";
    private String moduleIdentifier;


    public FaultInject(DynamicActiveBuilderScope dynamicScope, MablApiBuilder mablApiBuilder) {
        this.dynamicScope = dynamicScope;
        this.mablApiBuilder = mablApiBuilder;
        this.moduleIdentifier = "faultInject";
    }

    public FaultInject(MablApiBuilder mablApiBuilder, FmiBuilder.RuntimeModule<PStm> runtimeModule) {
        this(mablApiBuilder.getDynamicScope(), mablApiBuilder);
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

    public ComponentVariableFmi2Api faultInject(FmuVariableFmi2Api creator, ComponentVariableFmi2Api component, String constraintId) {
        String fiComponentName = component.getName().concat(FAULTINJECT_POSTFIX);
        PStm var = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(fiComponentName), newANameType("FMI2Component").clone(),
                newAExpInitializer(newNullExp().clone())));

        IMablScope scope = this.dynamicScope.getActiveScope();
        TryMaBlScope mTryScope = scope.findParentScope(TryMaBlScope.class);
        mTryScope.parent().addBefore(mTryScope.getDeclaration(), var);

        AInstanceMappingStm mapping = newAInstanceMappingStm(newAIdentifier(fiComponentName), component.getName());

        ComponentVariableFmi2Api fiComp =
                new ComponentVariableFmi2Api(var, creator, fiComponentName, creator.getModelDescriptionContext(), mablApiBuilder, mTryScope.parent(),
                        newAIdentifierStateDesignator(newAIdentifier(fiComponentName)), newAIdentifierExp(fiComponentName), fiComponentName);

        this.dynamicScope.add(mapping);

        this.dynamicScope.addAll((Arrays.asList(newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(fiComponentName)),
                newACallExp(newAIdentifierExp(getModuleIdentifier()), newAIdentifier("faultInject"),
                        Arrays.asList(newAIdentifierExp(creator.getName()), newAIdentifierExp(component.getName()),
                                newAStringLiteralExp(constraintId)))), newIf(newEqual(newAIdentifierExp(fiComponentName), newNullExp()),
                newABlockStm(newError(newAStringLiteralExp(fiComponentName + " IS NULL "))), null))));
        return fiComp;
    }

    public ComponentVariableFmi2Api observe(FmuVariableFmi2Api creator, ComponentVariableFmi2Api component, String constraintId) {

        //observe(FMI2 creator, FMI2Component component, string constraintId);
        return null;
    }

    public ComponentVariableFmi2Api returnFmuComponentValue(ComponentVariableFmi2Api component) {
        //returnFmuComponentValue(FMI2Component component);
        return null;
    }

}
