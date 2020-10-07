package org.intocps.maestro.plugin.Initializer;

import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.function.Predicate;

public class RelationsPredicates {
    public static Predicate<UnitRelationship.Relation> external() {
        return o -> (o.getOrigin() == UnitRelationship.Relation.InternalOrExternal.External);
    }

    public static Predicate<UnitRelationship.Relation> internal() {
        return o -> (o.getOrigin() == UnitRelationship.Relation.InternalOrExternal.Internal);
    }

    public static Predicate<UnitRelationship.Relation> inputToOutput() {
        return o -> (o.getDirection() == UnitRelationship.Relation.Direction.InputToOutput);
    }

    public static Predicate<UnitRelationship.Relation> inputSource() {
        return o -> (o.getSource().scalarVariable.getScalarVariable().causality == ModelDescription.Causality.Input);
    }

    public static Predicate<UnitRelationship.Relation> outputSource() {
        return o -> (o.getSource().scalarVariable.getScalarVariable().causality == ModelDescription.Causality.Output);
    }

}