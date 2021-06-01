package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.SBlockStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;

public class WhileMaBLScope extends ScopeFmi2Api implements Fmi2Builder.WhileScope<PStm> {
    private final MablApiBuilder builder;
    private final PStm declaration;
    private final SBlockStm block;
    private final ScopeFmi2Api declaringScope;

    public WhileMaBLScope(MablApiBuilder builder, PStm declaration, ScopeFmi2Api declaringScope, SBlockStm whileBlock) {
        super(builder, declaringScope, whileBlock);
        this.builder = builder;
        this.declaration = declaration;
        this.declaringScope = declaringScope;
        this.block = whileBlock;
    }
}
