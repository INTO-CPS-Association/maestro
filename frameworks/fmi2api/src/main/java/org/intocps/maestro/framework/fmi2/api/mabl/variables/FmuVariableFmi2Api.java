package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.ModelDescriptionContext;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;

import java.util.Arrays;

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
        return instantiate(name, builder.getDynamicScope());
    }

    @Override
    public void unload() {
        unload(builder.getDynamicScope());
    }

    @Override
    public void unload(Fmi2Builder.Scope<PStm> scope) {
        scope.add(newExpressionStm(newUnloadExp(Arrays.asList(getReferenceExp().clone()))));
    }

    @Override
    public ComponentVariableFmi2Api instantiate(String namePrefix, Fmi2Builder.Scope<PStm> scope) {
        String name = builder.getNameGenerator().getName(namePrefix);
        //TODO: Extract bool visible and bool loggingOn from configuration
        PStm var = newVariable(name, newANameType("FMI2Component"),
                call(getReferenceExp().clone(), "instantiate", newAStringLiteralExp(name), newABoolLiteralExp(true), newABoolLiteralExp(true)));
        ComponentVariableFmi2Api aMablFmi2ComponentAPI = null;
        aMablFmi2ComponentAPI = new ComponentVariableFmi2Api(var, this, name, this.modelDescriptionContext, builder, (IMablScope) scope,
                newAIdentifierStateDesignator(newAIdentifier(name)), newAIdentifierExp(name));
        scope.add(var);

        return aMablFmi2ComponentAPI;
    }

    public String getFmuIdentifier() {
        return fmuIdentifier;
    }
}
