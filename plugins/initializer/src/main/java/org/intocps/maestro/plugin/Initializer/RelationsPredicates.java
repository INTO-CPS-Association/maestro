package org.intocps.maestro.plugin.Initializer;

import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.function.Predicate;

public class RelationsPredicates {
    public static Predicate<FmiSimulationEnvironment.Relation> external() {
        return o -> (o.getOrigin() == FmiSimulationEnvironment.Relation.InternalOrExternal.External);
    }

    public static Predicate<FmiSimulationEnvironment.Relation> internal() {
        return o -> (o.getOrigin() == FmiSimulationEnvironment.Relation.InternalOrExternal.Internal);
    }

    public static Predicate<FmiSimulationEnvironment.Relation> inputToOutput() {
        return o -> (o.getDirection() == FmiSimulationEnvironment.Relation.Direction.InputToOutput);
    }

    public static Predicate<FmiSimulationEnvironment.Relation> inputSource() {
        return o -> (o.getSource().scalarVariable.getScalarVariable().causality == ModelDescription.Causality.Input);
    }

    public static Predicate<FmiSimulationEnvironment.Relation> outputSource() {
        return o -> (o.getSource().scalarVariable.getScalarVariable().causality == ModelDescription.Causality.Output);
    }

}