package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;

public class IfMaBlScope implements Fmi2Builder.IfScope<PStm> {
    private final MablApiBuilder builder;
    private final PStm declaration;
    private final ScopeFmi2Api declaringScope;
    private final ScopeFmi2Api thenScope;
    private final ScopeFmi2Api elseScope;

    public IfMaBlScope(MablApiBuilder builder, PStm declaration, ScopeFmi2Api declaringScope, ScopeFmi2Api thenScope, ScopeFmi2Api elseScope) {
        this.builder = builder;
        this.declaration = declaration;
        this.declaringScope = declaringScope;
        this.thenScope = thenScope;
        this.elseScope = elseScope;

        enterThen();
    }

    @Override
    public ScopeFmi2Api enterThen() {
        return thenScope.activate();
    }

    @Override
    public ScopeFmi2Api enterElse() {
        return elseScope.activate();
    }
}
