package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;

public class TryMaBlScope implements Fmi2Builder.TryScope<PStm> {
    private final MablApiBuilder builder;
    private final PStm declaration;
    private final ScopeFmi2Api declaringScope;
    private final ScopeFmi2Api bodyScope;
    private final ScopeFmi2Api finallyScope;

    public TryMaBlScope(MablApiBuilder builder, PStm declaration, ScopeFmi2Api declaringScope, ScopeFmi2Api body, ScopeFmi2Api finallyScope) {
        this.builder = builder;
        this.declaration = declaration;
        this.declaringScope = declaringScope;
        this.bodyScope = body;
        this.finallyScope = finallyScope;

        enter();
    }

    @Override
    public ScopeFmi2Api enter() {
        return bodyScope.activate();
    }

    @Override
    public ScopeFmi2Api enterFinally() {
        return finallyScope.activate();
    }

    @Override
    public ScopeFmi2Api leave() {
        return declaringScope.activate();
    }
}
