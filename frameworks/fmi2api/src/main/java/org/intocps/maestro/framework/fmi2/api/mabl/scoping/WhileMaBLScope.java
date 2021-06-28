package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.SBlockStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;

import static org.intocps.maestro.ast.MableAstFactory.newBreak;

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

    /**
     * If fmiErrorHandling is enabled in the MablApiBuilder.settings then the leave operation on a while scope automatically created an subsequent if
     * with an
     * additional break if global_execution_continue is false in
     * order to break out sufficiently.
     *
     * @return
     */
    @Override
    public ScopeFmi2Api leave() {
        if (builder.getSettings().fmiErrorHandlingEnabled) {
            ScopeFmi2Api scopeToReturn = super.leave();
            IfMaBlScope scope = scopeToReturn.enterIf(builder.getGlobalExecutionContinue().toPredicate().not());
            ScopeFmi2Api then = scope.enterThen();
            then.add(newBreak());
            then.leave();
            return scope.leave();
        } else {
            return leave();
        }
    }
}
