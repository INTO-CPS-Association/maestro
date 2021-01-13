package org.intocps.maestro.Fmi2AMaBLBuilder.scopebundle;

import org.intocps.maestro.Fmi2AMaBLBuilder.AMaBLScope;

public interface IBasicRootScopeBundle extends IBasicScopeBundle {
    @Override
    public AMaBLScope getScope();

    public AMaBLScope getRootScope();
}

