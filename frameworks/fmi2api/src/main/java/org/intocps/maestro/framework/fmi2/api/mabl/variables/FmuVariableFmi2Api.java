package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.ModelDescriptionContext;
import org.intocps.maestro.framework.fmi2.api.mabl.PredicateFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.TryMaBlScope;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.call;
import static org.intocps.maestro.ast.MableBuilder.newVariable;

public class FmuVariableFmi2Api extends VariableFmi2Api<Fmi2Builder.NamedVariable<PStm>> implements Fmi2Builder.Fmu2Variable<PStm> {

    private final ModelDescriptionContext modelDescriptionContext;
    private final MablApiBuilder builder;
    private String fmuIdentifier;

    public FmuVariableFmi2Api(String fmuIdentifier, MablApiBuilder builder, ModelDescriptionContext modelDescriptionContext, PStm declaration,
            PType type, IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope, PStateDesignator designator, PExp referenceExp) {
        this(builder, modelDescriptionContext, declaration, type, declaredScope, dynamicScope, designator, referenceExp);
        this.fmuIdentifier = fmuIdentifier;
    }

    public FmuVariableFmi2Api(MablApiBuilder builder, ModelDescriptionContext modelDescriptionContext, PStm declaration, PType type,
            IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope, PStateDesignator designator, PExp referenceExp) {
        super(declaration, type, declaredScope, dynamicScope, designator, referenceExp);
        this.builder = builder;
        this.modelDescriptionContext = modelDescriptionContext;
    }

    @Override
    public ComponentVariableFmi2Api instantiate(String name) {
        IMablScope scope = builder.getDynamicScope().getActiveScope();
        return instantiate(name, scope.findParentScope(TryMaBlScope.class), scope);
    }

    //    @Override
    //    public void unload() {
    //        unload(builder.getDynamicScope());
    //    }
    //
    //    @Override
    //    public void unload(Fmi2Builder.Scope<PStm> scope) {
    //        scope.add(newIf(newNotEqual(getReferenceExp().clone(), newNullExp()),
    //                newABlockStm(newExpressionStm(newUnloadExp(Arrays.asList(getReferenceExp().clone()))),
    //                        newAAssignmentStm(getDesignator().clone(), newNullExp())), null));
    //    }


    //    public void freeInstance(Fmi2Builder.Fmi2ComponentVariable<PStm> comp) {
    //        freeInstance(builder.getDynamicScope(), comp);
    //    }

    //    /**
    //     * Performs null check and frees the instance
    //     *
    //     * @param scope
    //     * @param comp
    //     */
    //    private void freeInstance(Fmi2Builder.Scope<PStm> scope, Fmi2Builder.Fmi2ComponentVariable<PStm> comp) {
    //        if (comp instanceof ComponentVariableFmi2Api) {
    //            scope.add(newIf(newNotEqual(((ComponentVariableFmi2Api) comp).getReferenceExp().clone(), newNullExp()), newABlockStm(
    //                    MableAstFactory.newExpressionStm(
    //                            call(getReferenceExp().clone(), "freeInstance", ((ComponentVariableFmi2Api) comp).getReferenceExp().clone())),
    //                    newAAssignmentStm(((ComponentVariableFmi2Api) comp).getDesignatorClone(), newNullExp())), null));
    //        } else {
    //            throw new RuntimeException("Argument is not an FMU instance - it is not an instance of ComponentVariableFmi2API");
    //        }
    //    }

    @Override
    public ComponentVariableFmi2Api instantiate(String namePrefix, Fmi2Builder.TryScope<PStm> enclosingTryScope, Fmi2Builder.Scope<PStm> scope) {
        String name = builder.getNameGenerator().getName(namePrefix);
        //TODO: Extract bool visible and bool loggingOn from configuration
        var var = newVariable(name, newANameType("FMI2Component"), newNullExp());

        PStm instantiateAssign = newAAssignmentStm(newAIdentifierStateDesignator(name),
                call(getReferenceExp().clone(), "instantiate", newAStringLiteralExp(name), newABoolLiteralExp(true), newABoolLiteralExp(true)));


        ComponentVariableFmi2Api compVar = new ComponentVariableFmi2Api(var, this, name, this.modelDescriptionContext, builder, (IMablScope) scope,
                newAIdentifierStateDesignator(newAIdentifier(name)), newAIdentifierExp(name));

        if (enclosingTryScope == null) {
            throw new IllegalArgumentException("Call to instantiate is not allowed with a null enclosing try scope");
        }


        TryMaBlScope mTryScope = (TryMaBlScope) enclosingTryScope;
        mTryScope.parent().addBefore(mTryScope.getDeclaration(), var);

        scope.add(instantiateAssign);

        mTryScope.getFinallyBody().addAfterOrTop(null, newIf(newNotEqual(compVar.getReferenceExp().clone(), newNullExp()),
                newABlockStm(MableAstFactory.newExpressionStm(call(getReferenceExp().clone(), "freeInstance", compVar.getReferenceExp().clone())),
                        newAAssignmentStm(compVar.getDesignatorClone(), newNullExp())), null));

        scope.activate();

        if (builder.getSettings().fmiErrorHandlingEnabled) {
            ScopeFmi2Api thenScope =
                    (ScopeFmi2Api) scope.enterIf(new PredicateFmi2Api(newEqual(compVar.getReferenceExp().clone(), newNullExp()))).enterThen();

            builder.getLogger().error(thenScope, "Instantiate failed on fmu: '%s' for instance: '%s'", this.getFmuIdentifier(), namePrefix);
            thenScope.add(new AErrorStm(
                    newAStringLiteralExp(String.format("Instantiate failed on fmu: '%s' for instance: '%s'", this.getFmuIdentifier(), namePrefix))));
            thenScope.leave();
        }

        ((IMablScope) scope).registerComponentVariableFmi2Api(compVar);

        return compVar;
    }

    public String getFmuIdentifier() {
        return fmuIdentifier;
    }
}
