package org.intocps.maestro.framework.core;

import org.intocps.maestro.ast.LexIdentifier;

import java.util.Map;

public interface IRelation {
    InternalOrExternal getOrigin();

    IVariable getSource();

    Direction getDirection();

    Map<LexIdentifier, ? extends IVariable> getTargets();

    public enum InternalOrExternal {
        Internal,
        External
    }

    public enum Direction {
        OutputToInput,
        InputToOutput
    }
}
