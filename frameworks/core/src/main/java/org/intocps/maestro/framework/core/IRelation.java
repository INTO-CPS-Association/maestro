package org.intocps.maestro.framework.core;

import org.intocps.maestro.ast.LexIdentifier;

import java.util.Map;

public interface IRelation {
    InternalOrExternal getOrigin();

    RelationVariable getSource();

    Direction getDirection();

    Map<LexIdentifier, ? extends RelationVariable> getTargets();

    public enum InternalOrExternal {
        Internal,
        External
    }

    public enum Direction {
        OutputToInput,
        InputToOutput
    }
}
