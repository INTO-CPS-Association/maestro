package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;

import java.util.List;

public abstract class Environment {
    protected final Environment outer;

    public Environment(Environment outer) {
        this.outer = outer;
    }

    /**
     * The the definitions that this environment can find
     *
     * @return
     */
    protected abstract List<? extends PDeclaration> getDefinitions();

    /**
     * Find a name in the environment of the given scope.
     *
     * @param name
     * @return
     */
    abstract public PDeclaration findName(LexIdentifier name);

    /**
     * Find a type in the environment.
     *
     * @param name
     * @return
     */
    abstract public PDeclaration findType(LexIdentifier name);
}
