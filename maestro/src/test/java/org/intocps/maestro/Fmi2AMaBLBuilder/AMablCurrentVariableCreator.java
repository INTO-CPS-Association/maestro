package org.intocps.maestro.Fmi2AMaBLBuilder;

import org.intocps.maestro.Fmi2AMaBLBuilder.scopebundle.ScopeBundle;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;

import java.util.function.Supplier;

public class AMablCurrentVariableCreator extends AMaBLVariableCreator {

    public AMablCurrentVariableCreator(Fmi2SimulationEnvironment simEnv, Supplier<AMaBLScope> currentScopeSupplier) {
        super(simEnv, new ScopeBundle(currentScopeSupplier));
    }
}
