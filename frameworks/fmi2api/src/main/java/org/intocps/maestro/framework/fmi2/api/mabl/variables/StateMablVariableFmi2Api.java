package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;

import java.util.Collections;

import static org.intocps.maestro.ast.MableAstFactory.newAAssignmentStm;
import static org.intocps.maestro.ast.MableBuilder.call;

public class StateMablVariableFmi2Api extends VariableFmi2Api<Object> implements FmiBuilder.StateVariable<PStm> {
    private final ComponentVariableFmi2Api owner;
    private final MablApiBuilder builder;
    private boolean valid = true;

    public StateMablVariableFmi2Api(PStm declaration, PType type, IMablScope declaredScope, FmiBuilder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp, MablApiBuilder builder, ComponentVariableFmi2Api owner) {
        super(declaration, type, declaredScope, dynamicScope, designator, referenceExp);
        this.owner = owner;
        this.builder = builder;
    }


    @Override
    public void set() throws IllegalStateException {
        set(builder.getDynamicScope());
    }


    @Override
    public void set(FmiBuilder.Scope<PStm> scope) throws IllegalStateException {
        if (!valid) {
            throw new IllegalStateException();
        }
        AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                call(owner.getReferenceExp().clone(), "setState", Collections.singletonList(this.getReferenceExp().clone())));
        scope.add(stm);
        if (builder.getSettings().fmiErrorHandlingEnabled) {
            ComponentVariableFmi2Api.FmiStatusErrorHandlingBuilder.generate(builder, "setState", this.owner, (IMablScope) scope,
                    MablApiBuilder.FmiStatus.FMI_ERROR, MablApiBuilder.FmiStatus.FMI_FATAL);
        }
    }

    @Override
    public void destroy() throws IllegalStateException {
        destroy(builder.getDynamicScope());
    }

    @Override
    public void destroy(FmiBuilder.Scope<PStm> scope) throws IllegalStateException {
        if (!valid) {
            throw new IllegalStateException();
        }

        AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                call(owner.getReferenceExp().clone(), "freeState", Collections.singletonList(this.getReferenceExp().clone())));
        scope.add(stm);
        if (builder.getSettings().fmiErrorHandlingEnabled) {
            ComponentVariableFmi2Api.FmiStatusErrorHandlingBuilder.generate(builder, "freeState", this.owner, (IMablScope) scope,
                    MablApiBuilder.FmiStatus.FMI_ERROR, MablApiBuilder.FmiStatus.FMI_FATAL);
        }

        valid = false;
    }

    @Override
    public void setValue(FmiBuilder.Scope<PStm> scope, Object value) {
        throw new IllegalStateException();
    }


}
