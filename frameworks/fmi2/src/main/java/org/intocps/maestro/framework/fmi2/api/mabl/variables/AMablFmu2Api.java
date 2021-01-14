package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.MableBuilder;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.ModelDescriptionContext;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;

public class AMablFmu2Api implements Fmi2Builder.Fmu2Api {


    private final ModelDescriptionContext modelDescriptionContext;
    private final String name;
    private final MablApiBuilder builder;
    //    private final Supplier<AMaBLScope> currentScopeSupplier;
    private AMablVariable<AMablFmu2Api> variable;

    public AMablFmu2Api(String name, MablApiBuilder builder, ModelDescriptionContext modelDescriptionContext) {
        this.name = name;
        this.builder = builder;
        this.modelDescriptionContext = modelDescriptionContext;

    }

    //    public AMablFmu2Api(String name, Fmi2SimulationEnvironment simEnv, ModelDescriptionContext modelDescriptionContext,
    //            Supplier<AMaBLScope> currentScopeSupplier) {
    //        this.name = name;
    //        this.simulationEnvironment = simEnv;
    //        this.modelDescriptionContext = modelDescriptionContext;
    //        this.currentScopeSupplier = currentScopeSupplier;
    //    }

    @Override
    // Returns a lambda
    public AMablFmi2ComponentAPI create(String name) {
        return createInternal(name, builder.getDynamicScope());

    }

    @Override
    public Fmi2Builder.Fmi2ComponentApi create(String name, Fmi2Builder.Scope scope) {
        return createInternal(name, (IMablScope) scope);
    }


    // Todo: Ensure that parent variable is available via scoping
    // Todo: Perhaps move to variablecreator?
    public AMablFmi2ComponentAPI createInternal(String namePrefix, IMablScope scope) {

        String name = builder.getNameGenerator().getName(namePrefix);
        //TODO: Extract bool visible and bool loggingOn from configuration
        PStm var = newVariable(name, newANameType("FMI2Component"),
                MableBuilder.call(name, "instantiate", newAStringLiteralExp(name), newABoolLiteralExp(true), newABoolLiteralExp(true)));
        AMablFmi2ComponentAPI aMablFmi2ComponentAPI = null;
        /*AMablVariable fmuComponent =
                new AMablVariable(newANameType("FMI2Component"), scope, dynamicScope, newAIdentifierStateDesignator(newAIdentifier(name)),
                        newAIdentifierExp(name));*/
        aMablFmi2ComponentAPI = new AMablFmi2ComponentAPI(var, this, name, this.modelDescriptionContext, builder, scope,
                newAIdentifierStateDesignator(newAIdentifier(name)), newAIdentifierExp(name));
        scope.add(var);
        //scope.addVariable(fmuComponent.getValue(), fmuComponent);

        return aMablFmi2ComponentAPI;
    }

    //    public Function<AMaBLScope, AMablFmi2ComponentAPI> create(String name) {
    //        //TODO: Extract bool visible and bool loggingOn from configuration
    //        return (scope) -> {
    //            PStm var = newVariable(name, newANameType("FMI2Component"),
    //                    MableBuilder.call(name, "instantiate", newAStringLiteralExp(name), newABoolLiteralExp(true), newABoolLiteralExp(true)));
    //            AMablVariable fmuComponent = new AMablVariable(name, newANameType("FMI2Component"), scope, new AMaBLVariableLocation.BasicPosition());
    //            AMablFmi2ComponentAPI aMablFmi2ComponentAPI =
    //                    new AMablFmi2ComponentAPI(this, name, fmuComponent, this.modelDescriptionContext, scopeBundle);
    //            scope.addStatement(var);
    //            scope.addVariable(fmuComponent.getValue(), fmuComponent);
    //
    //            return aMablFmi2ComponentAPI;
    //        };
    //    }

    public void setVariable(AMablVariable<AMablFmu2Api> fmu) {
        this.variable = fmu;
    }

    public String getName() {
        return this.name;
    }
}
