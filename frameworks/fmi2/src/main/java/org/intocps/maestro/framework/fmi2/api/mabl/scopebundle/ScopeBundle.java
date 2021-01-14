package org.intocps.maestro.framework.fmi2.api.mabl.scopebundle;

import org.intocps.maestro.framework.fmi2.api.mabl.scoping.AMaBLScope;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ScopeBundle implements IScopeBundle {
    private final Consumer<AMaBLScope> currentScopeSetter;
    private final Supplier<AMaBLScope> scopeGetter;
    private final Supplier<AMaBLScope> rootScopeGetter;

    public ScopeBundle(Consumer<AMaBLScope> currentScopeSetter, Supplier<AMaBLScope> scopeGetter, Supplier<AMaBLScope> rootScopeGetter) {
        this.currentScopeSetter = currentScopeSetter;
        this.scopeGetter = scopeGetter;
        this.rootScopeGetter = rootScopeGetter;
    }

    public ScopeBundle(Supplier<AMaBLScope> scopeGetter, Supplier<AMaBLScope> rootScopeGetter) {
        this(null, scopeGetter, rootScopeGetter);
    }

    public ScopeBundle(Supplier<AMaBLScope> scopeGetter) {
        this(null, scopeGetter, null);
    }

    @Override
    public AMaBLScope getRootScope() {
        return this.rootScopeGetter.get();
    }

    @Override
    public AMaBLScope getCurrentScope() {
        return scopeGetter.get();
    }

    @Override
    public void setCurrentScope(AMaBLScope scope) {
        this.currentScopeSetter.accept(scope);
    }
}
