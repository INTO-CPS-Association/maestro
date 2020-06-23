package org.intocps.maestro.plugin.Initializer;

import org.intocps.maestro.plugin.env.UnitRelationship;

import java.util.function.Predicate;

public class RelationsPredicates {
    public static Predicate<UnitRelationship.Relation> External() {
        return o -> (o.getOrigin() == UnitRelationship.Relation.InternalOrExternal.External);
    }

    public static Predicate<UnitRelationship.Relation> Internal() {
        return o -> (o.getOrigin() == UnitRelationship.Relation.InternalOrExternal.Internal);

    }
}