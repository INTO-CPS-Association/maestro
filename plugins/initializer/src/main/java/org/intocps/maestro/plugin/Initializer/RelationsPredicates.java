package org.intocps.maestro.plugin.Initializer;

import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.function.Predicate;

public class RelationsPredicates {
    public static Predicate<UnitRelationship.Relation> External() {
        return o -> (o.getOrigin() == UnitRelationship.Relation.InternalOrExternal.External);
    }

    public static Predicate<UnitRelationship.Relation> Internal() {
        return o -> (o.getOrigin() == UnitRelationship.Relation.InternalOrExternal.Internal);
    }

    public static Predicate<UnitRelationship.Relation> InputToOutput() {
        return o -> (o.getDirection() == UnitRelationship.Relation.Direction.InputToOutput);
    }

    public static Predicate<UnitRelationship.Relation> InputSource() {
        return o -> (o.getSource().scalarVariable.getScalarVariable().causality == ModelDescription.Causality.Input);
    }

    public static Predicate<UnitRelationship.Relation> OutputSource() {
        return o -> (o.getSource().scalarVariable.getScalarVariable().causality == ModelDescription.Causality.Output);
    }
    
}