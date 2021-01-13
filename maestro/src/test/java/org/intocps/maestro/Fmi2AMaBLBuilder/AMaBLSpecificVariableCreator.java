package org.intocps.maestro.Fmi2AMaBLBuilder;

import org.intocps.maestro.Fmi2AMaBLBuilder.scopebundle.ScopeBundle;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.net.URI;

public class AMaBLSpecificVariableCreator extends AMaBLVariableCreator {
    private final AMaBLScope scope;

    public AMaBLSpecificVariableCreator(Fmi2SimulationEnvironment simEnv, AMaBLScope scope) {
        super(simEnv, new ScopeBundle(() -> scope));
        this.scope = scope;
    }

    @Override
    public AMablFmu2Api createFMU(String name, ModelDescription modelDescription, URI path) throws XPathExpressionException {
        return this.createFMU(name, modelDescription, path);
    }
}
