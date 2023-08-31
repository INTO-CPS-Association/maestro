package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.fmi.jnifmuapi.fmi3.Fmi3Instance;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.ModelDescriptionContext;
import org.intocps.maestro.framework.fmi2.api.mabl.ModelDescriptionContext3;
import org.intocps.maestro.framework.fmi2.api.mabl.PredicateFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.TryMaBlScope;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.call;
import static org.intocps.maestro.ast.MableBuilder.newVariable;

public class FmuVariableFmi3Api extends VariableFmi2Api<Fmi2Builder.NamedVariable<PStm>> implements Fmi2Builder.Fmu3Variable<PStm> {
//    public FmuVariableFmi3Api(PStm declaration, PType type, IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope,
//            PStateDesignator designator, PExp referenceExp) {
//        super(declaration, type, declaredScope, dynamicScope, designator, referenceExp);
//    }


    private final ModelDescriptionContext3 modelDescriptionContext;
    private final MablApiBuilder builder;
    private String fmuIdentifier;
//
    public FmuVariableFmi3Api(String fmuIdentifier, MablApiBuilder builder, ModelDescriptionContext3 modelDescriptionContext, PStm declaration,
            PType type, IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope, PStateDesignator designator, PExp referenceExp) {
        this(builder, modelDescriptionContext, declaration, type, declaredScope, dynamicScope, designator, referenceExp);
        this.fmuIdentifier = fmuIdentifier;
    }


    public FmuVariableFmi3Api(MablApiBuilder builder, ModelDescriptionContext3 modelDescriptionContext, PStm declaration, PType type,
            IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope, PStateDesignator designator, PExp referenceExp) {
        super(declaration, type, declaredScope, dynamicScope, designator, referenceExp);
        this.builder = builder;
        this.modelDescriptionContext = modelDescriptionContext;
    }

//
//    public ModelDescriptionContext3 getModelDescriptionContext() {
//        return modelDescriptionContext;
//    }
//
//    @Override
//    public ComponentVariableFmi2Api instantiate(String name, String environmentName) {
//        IMablScope scope = builder.getDynamicScope().getActiveScope();
//        return instantiate(name, scope.findParentScope(TryMaBlScope.class), scope, environmentName);
//    }
//
//    @Override
//    public ComponentVariableFmi2Api instantiate(String name) {
//        IMablScope scope = builder.getDynamicScope().getActiveScope();
//        return instantiate(name, scope.findParentScope(TryMaBlScope.class), scope);
//    }
//
//
//    @Override
//    public ComponentVariableFmi2Api instantiate(String namePrefix, Fmi2Builder.TryScope<PStm> enclosingTryScope, Fmi2Builder.Scope<PStm> scope,
//            String environmentName) {
//        return instantiate(namePrefix, enclosingTryScope, scope, environmentName, true);
//    }
//
//    @Override
//    public ComponentVariableFmi2Api instantiate(String namePrefix, Fmi2Builder.TryScope<PStm> enclosingTryScope, Fmi2Builder.Scope<PStm> scope,
//            String environmentName, boolean loggingOn) {
//
//        String name = builder.getNameGenerator().getName(namePrefix);
//        //TODO: Extract bool visible and bool loggingOn from configuration
//        var var = newVariable(name, newANameType("FMI2Component"), newNullExp());
//
//        PStm instantiateAssign = newAAssignmentStm(newAIdentifierStateDesignator(name),
//                call(getReferenceExp().clone(), "instantiate", newAStringLiteralExp(name), newABoolLiteralExp(true), newABoolLiteralExp(loggingOn)));
//
//
//        if (enclosingTryScope == null) {
//            throw new IllegalArgumentException("Call to instantiate is not allowed with a null enclosing try scope");
//        }
//
//
//        TryMaBlScope mTryScope = (TryMaBlScope) enclosingTryScope;
//        mTryScope.parent().addBefore(mTryScope.getDeclaration(), var);
//
//        ComponentVariableFmi2Api compVar;
//        if (environmentName == null) {
//            compVar = new ComponentVariableFmi2Api(var, this, name, this.modelDescriptionContext, builder, mTryScope.parent(),
//                    newAIdentifierStateDesignator(newAIdentifier(name)), newAIdentifierExp(name));
//        } else {
//
//            AInstanceMappingStm mapping = newAInstanceMappingStm(newAIdentifier(name), environmentName);
//            compVar = new ComponentVariableFmi2Api(var, this, name, this.modelDescriptionContext, builder, mTryScope.parent(),
//                    newAIdentifierStateDesignator(newAIdentifier(name)), newAIdentifierExp(name), environmentName);
//            scope.add(mapping);
//        }
//
//        scope.add(instantiateAssign);
//
//        mTryScope.getFinallyBody().addAfterOrTop(null, newIf(newNotEqual(compVar.getReferenceExp().clone(), newNullExp()),
//                newABlockStm(MableAstFactory.newExpressionStm(call(getReferenceExp().clone(), "freeInstance", compVar.getReferenceExp().clone())),
//                        newAAssignmentStm(compVar.getDesignatorClone(), newNullExp())), null));
//
//        scope.activate();
//
//        if (builder.getSettings().fmiErrorHandlingEnabled) {
//            ScopeFmi2Api thenScope =
//                    (ScopeFmi2Api) scope.enterIf(new PredicateFmi2Api(newEqual(compVar.getReferenceExp().clone(), newNullExp()))).enterThen();
//
//            builder.getLogger().error(thenScope, "Instantiate failed on fmu: '%s' for instance: '%s'", this.getFmuIdentifier(), namePrefix);
//            thenScope.add(new AErrorStm(
//                    newAStringLiteralExp(String.format("Instantiate failed on fmu: '%s' for instance: '%s'", this.getFmuIdentifier(), namePrefix))));
//            thenScope.leave();
//        }
//
//        ((IMablScope) scope).registerComponentVariableFmi2Api(compVar);
//
//        return compVar;
//    }
//
//    @Override
//    public ComponentVariableFmi2Api instantiate(String namePrefix, Fmi2Builder.TryScope<PStm> enclosingTryScope, Fmi2Builder.Scope<PStm> scope) {
//        return instantiate(namePrefix, enclosingTryScope, scope, null);
//    }
//
//    public String getFmuIdentifier() {
//        return fmuIdentifier;
//    }
}