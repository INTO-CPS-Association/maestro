package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableCreatorFmi2Api;

public interface IMablScope extends Fmi2Builder.Scope<PStm> {


    @Override
    VariableCreatorFmi2Api getVariableCreator();

    String getName(String prefix);
}
