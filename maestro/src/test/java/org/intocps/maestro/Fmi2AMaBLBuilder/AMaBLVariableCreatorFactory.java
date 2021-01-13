package org.intocps.maestro.Fmi2AMaBLBuilder;

import org.intocps.maestro.Fmi2AMaBLBuilder.scopebundle.IScopeBundle;
import org.intocps.maestro.Fmi2AMaBLBuilder.scopebundle.ScopeBundle;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;

import java.util.function.Supplier;

public class AMaBLVariableCreatorFactory {

    public static AMaBLVariableCreator CreateCurrentScopeVariableCreator(ScopeBundle scopeBundle, Fmi2SimulationEnvironment simulationEnvironment) {
        return new AMaBLVariableCreator(scopeBundle, () -> scopeBundle.getCurrentScope(), simulationEnvironment);
    }

    public static AMaBLVariableCreator CreateScopeSpecificVariableCreator(IScopeBundle scopeBundle, Supplier<AMaBLScope> scopeSupplier,
            Fmi2SimulationEnvironment simulationEnvironment) {
        return new AMaBLVariableCreator(scopeBundle, scopeSupplier, simulationEnvironment);
    }
}
