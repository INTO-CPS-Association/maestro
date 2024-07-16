package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.ModelDescriptionContext3;
import org.intocps.maestro.framework.fmi2.api.mabl.PredicateFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.TryMaBlScope;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.call;
import static org.intocps.maestro.ast.MableBuilder.newVariable;

public class FmuVariableFmi3Api extends VariableFmi2Api<FmiBuilder.NamedVariable<PStm>> implements FmiBuilder.Fmu3Variable<PStm> {
    //    public FmuVariableFmi3Api(PStm declaration, PType type, IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope,
    //            PStateDesignator designator, PExp referenceExp) {
    //        super(declaration, type, declaredScope, dynamicScope, designator, referenceExp);
    //    }


    private final ModelDescriptionContext3 modelDescriptionContext;
    private final MablApiBuilder builder;
    private String fmuIdentifier;

    public FmuVariableFmi3Api(String fmuIdentifier, MablApiBuilder builder, ModelDescriptionContext3 modelDescriptionContext, PStm declaration,
            PType type, IMablScope declaredScope, FmiBuilder.DynamicActiveScope<PStm> dynamicScope, PStateDesignator designator, PExp referenceExp) {
        this(builder, modelDescriptionContext, declaration, type, declaredScope, dynamicScope, designator, referenceExp);
        this.fmuIdentifier = fmuIdentifier;
    }


    public FmuVariableFmi3Api(MablApiBuilder builder, ModelDescriptionContext3 modelDescriptionContext, PStm declaration, PType type,
            IMablScope declaredScope, FmiBuilder.DynamicActiveScope<PStm> dynamicScope, PStateDesignator designator, PExp referenceExp) {
        super(declaration, type, declaredScope, dynamicScope, designator, referenceExp);
        this.builder = builder;
        this.modelDescriptionContext = modelDescriptionContext;
    }

    public ModelDescriptionContext3 getModelDescriptionContext() {
        return modelDescriptionContext;
    }

    public InstanceVariableFmi3Api instantiate(String name, boolean visible, boolean loggingOn, boolean eventModeUsed, boolean earlyReturnAllowed,
            ArrayVariableFmi2Api requiredIntermediateVariables) {
        IMablScope scope = builder.getDynamicScope().getActiveScope();
        return instantiate(name, scope.findParentScope(TryMaBlScope.class), scope, visible, loggingOn, eventModeUsed, earlyReturnAllowed,
                requiredIntermediateVariables);
    }

    public InstanceVariableFmi3Api instantiate(String name, FmiBuilder.TryScope<PStm> enclosingTryScope, FmiBuilder.Scope<PStm> scope,
            boolean visible, boolean loggingOn, boolean eventModeUsed, boolean earlyReturnAllowed,
            ArrayVariableFmi2Api requiredIntermediateVariables) {
        return instantiate(name, enclosingTryScope, scope, null, visible, loggingOn, eventModeUsed, earlyReturnAllowed,
                requiredIntermediateVariables);

    }

    public InstanceVariableFmi3Api instantiate(String name, String environmentName, boolean visible, boolean loggingOn, boolean eventModeUsed,
            boolean earlyReturnAllowed, ArrayVariableFmi2Api requiredIntermediateVariables) {
        IMablScope scope = builder.getDynamicScope().getActiveScope();
        return instantiate(name, scope.findParentScope(TryMaBlScope.class), builder.getDynamicScope(), environmentName, visible, loggingOn,
                eventModeUsed, earlyReturnAllowed, requiredIntermediateVariables);
    }


    public InstanceVariableFmi3Api instantiate(String namePrefix, FmiBuilder.TryScope<PStm> enclosingTryScope, FmiBuilder.Scope<PStm> scope,
            String environmentName, boolean visible, boolean loggingOn, boolean eventModeUsed, boolean earlyReturnAllowed,
            ArrayVariableFmi2Api requiredIntermediateVariables) {

        String name = builder.getNameGenerator().getName(namePrefix);
        var var = newVariable(name, newANameType("FMI3Instance"), newNullExp());

        PStm instantiateAssign = newAAssignmentStm(newAIdentifierStateDesignator(name),
                call(getReferenceExp().clone(), "instantiateCoSimulation", newAStringLiteralExp(name), newABoolLiteralExp(visible),
                        newABoolLiteralExp(loggingOn), newABoolLiteralExp(eventModeUsed), newABoolLiteralExp(earlyReturnAllowed),
                        requiredIntermediateVariables.getReferenceExp().clone()));

        if (enclosingTryScope == null) {
            throw new IllegalArgumentException("Call to instantiate is not allowed with a null enclosing try scope");
        }


        TryMaBlScope mTryScope = (TryMaBlScope) enclosingTryScope;
        mTryScope.parent().addBefore(mTryScope.getDeclaration(), var);

        InstanceVariableFmi3Api instanceVar;
        if (environmentName == null) {
            instanceVar = new InstanceVariableFmi3Api(var, this, name, this.modelDescriptionContext, builder, mTryScope.parent(),
                    newAIdentifierStateDesignator(newAIdentifier(name)), newAIdentifierExp(name));
        } else {

            AInstanceMappingStm mapping = newAInstanceMappingStm(newAIdentifier(name), environmentName);
            instanceVar = new InstanceVariableFmi3Api(var, this, name, this.modelDescriptionContext, builder, mTryScope.parent(),
                    newAIdentifierStateDesignator(newAIdentifier(name)), newAIdentifierExp(name), environmentName);
            scope.add(mapping);
        }

        scope.add(instantiateAssign);

        mTryScope.getFinallyBody().addAfterOrTop(null, newIf(newNotEqual(instanceVar.getReferenceExp().clone(), newNullExp()),
                newABlockStm(MableAstFactory.newExpressionStm(call(getReferenceExp().clone(), "freeInstance", instanceVar.getReferenceExp().clone())),
                        newAAssignmentStm(instanceVar.getDesignatorClone(), newNullExp())), null));

        scope.activate();

        if (builder.getSettings().fmiErrorHandlingEnabled) {
            ScopeFmi2Api thenScope =
                    (ScopeFmi2Api) scope.enterIf(new PredicateFmi2Api(newEqual(instanceVar.getReferenceExp().clone(), newNullExp()))).enterThen();

            builder.getLogger().error(thenScope, "Instantiate failed on fmu: '%s' for instance: '%s'", this.getFmuIdentifier(), namePrefix);
            thenScope.add(new AErrorStm(
                    newAStringLiteralExp(String.format("Instantiate failed on fmu: '%s' for instance: '%s'", this.getFmuIdentifier(), namePrefix))));
            thenScope.leave();
        }

        ((IMablScope) scope).registerInstanceVariableFmi3Api(instanceVar);

        return instanceVar;
    }


    public String getFmuIdentifier() {
        return fmuIdentifier;
    }
}