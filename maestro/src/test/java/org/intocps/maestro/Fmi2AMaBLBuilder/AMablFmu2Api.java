package org.intocps.maestro.Fmi2AMaBLBuilder;

import org.intocps.maestro.Fmi2AMaBLBuilder.scopebundle.IBasicScopeBundle;
import org.intocps.maestro.ast.MableBuilder;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;

public class AMablFmu2Api implements Fmi2Builder.Fmu2Api {


    private final Fmi2SimulationEnvironment simulationEnvironment;
    private final ModelDescriptionContext modelDescriptionContext;
    private final String name;
    private final IBasicScopeBundle scopeBundle;
    //    private final Supplier<AMaBLScope> currentScopeSupplier;
    private AMablVariable<AMablFmu2Api> variable;

    public AMablFmu2Api(String name, Fmi2SimulationEnvironment simulationEnvironment, ModelDescriptionContext modelDescriptionContext,
            IBasicScopeBundle scopeBundle) {
        this.name = name;
        this.simulationEnvironment = simulationEnvironment;
        this.modelDescriptionContext = modelDescriptionContext;
        this.scopeBundle = scopeBundle;
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
        return this.create(name, scopeBundle.getCurrentScope());
    }

    @Override
    public Fmi2Builder.Fmi2ComponentApi create(String name, Fmi2Builder.Scope scope) {
        return this.create(name, (AMaBLScope) scope);
    }

    // Todo: Ensure that parent variable is available via scoping
    // Todo: Perhaps move to variablecreator?
    public AMablFmi2ComponentAPI create(String name, AMaBLScope scope) {

        //TODO: Extract bool visible and bool loggingOn from configuration
        PStm var = newVariable(name, newANameType("FMI2Component"),
                MableBuilder.call(name, "instantiate", newAStringLiteralExp(name), newABoolLiteralExp(true), newABoolLiteralExp(true)));
        AMablFmi2ComponentAPI aMablFmi2ComponentAPI = null;
        AMablVariable fmuComponent = new AMablVariable(name, newANameType("FMI2Component"), scope, new AMaBLVariableLocation.BasicPosition());
        aMablFmi2ComponentAPI = new AMablFmi2ComponentAPI(this, name, fmuComponent, this.modelDescriptionContext, scopeBundle);
        scope.addStatement(var);
        scope.addVariable(fmuComponent.getValue(), fmuComponent);

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
