package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;

import java.util.Collections;

import static org.intocps.maestro.ast.MableAstFactory.newAAssignmentStm;
import static org.intocps.maestro.ast.MableBuilder.call;

public class StateMablVariableFmi3Api extends VariableFmi2Api<Object> implements Fmi2Builder.StateVariable<PStm> {
    private final InstanceVariableFmi3Api owner;
    private final MablApiBuilder builder;
    private boolean valid = true;

    public StateMablVariableFmi3Api(PStm declaration, PType type, IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope,
            PStateDesignator designator, PExp referenceExp, MablApiBuilder builder, InstanceVariableFmi3Api owner) {
        super(declaration, type, declaredScope, dynamicScope, designator, referenceExp);
        this.owner = owner;
        this.builder = builder;
    }


    @Override
    public void set() throws IllegalStateException {
        set(builder.getDynamicScope());
    }

    @Override
    public void set(Fmi2Builder.Scope<PStm> scope) throws IllegalStateException {
        if (!valid) {
            throw new IllegalStateException();
        }
        AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                call(owner.getReferenceExp().clone(), "setState", Collections.singletonList(this.getReferenceExp().clone())));
        scope.add(stm);
        if (builder.getSettings().fmiErrorHandlingEnabled) {
            InstanceVariableFmi3Api.FmiStatusErrorHandlingBuilder
                    .generate(builder, "setState", this.owner, (IMablScope) scope, MablApiBuilder.Fmi3Status.FMI_ERROR,
                            MablApiBuilder.Fmi3Status.FMI_FATAL);
        }
    }

    @Override
    public void destroy() throws IllegalStateException {
        destroy(builder.getDynamicScope());
    }

    @Override
    public void destroy(Fmi2Builder.Scope<PStm> scope) throws IllegalStateException {
        if (!valid) {
            throw new IllegalStateException();
        }

        AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                call(owner.getReferenceExp().clone(), "freeState", Collections.singletonList(this.getReferenceExp().clone())));
        scope.add(stm);
        if (builder.getSettings().fmiErrorHandlingEnabled) {
            InstanceVariableFmi3Api.FmiStatusErrorHandlingBuilder
                    .generate(builder, "freeState", this.owner, (IMablScope) scope, MablApiBuilder.Fmi3Status.FMI_ERROR,
                            MablApiBuilder.Fmi3Status.FMI_FATAL);
        }

        valid = false;
    }

    @Override
    public void setValue(Fmi2Builder.Scope<PStm> scope, Object value) {
        throw new IllegalStateException();
    }


}
