package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;

public class IfMaBlScope implements Fmi2Builder.IfScope<PStm> {
    private final MablApiBuilder builder;
    private final PStm declaration;
    private final AMaBLScope declaringScope;
    private final IMablScope thenScope;
    private final IMablScope elseScope;

    public IfMaBlScope(MablApiBuilder builder, PStm declaration, AMaBLScope declaringScope, IMablScope thenScope, IMablScope elseScope) {
        this.builder = builder;
        this.declaration = declaration;
        this.declaringScope = declaringScope;
        this.thenScope = thenScope;
        this.elseScope = elseScope;

        enterThen();
    }

    @Override
    public Fmi2Builder.Scope<PStm> enterThen() {
        return thenScope.activate();
    }

    @Override
    public Fmi2Builder.Scope<PStm> enterElse() {
        return elseScope.activate();
    }
}
