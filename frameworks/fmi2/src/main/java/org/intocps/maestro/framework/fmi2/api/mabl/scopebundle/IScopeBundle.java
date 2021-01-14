package org.intocps.maestro.framework.fmi2.api.mabl.scopebundle;

import org.intocps.maestro.framework.fmi2.api.mabl.scoping.AMaBLScope;

public interface IScopeBundle extends IBasicRootScopeBundle {
    public void setCurrentScope(AMaBLScope scope);
}
