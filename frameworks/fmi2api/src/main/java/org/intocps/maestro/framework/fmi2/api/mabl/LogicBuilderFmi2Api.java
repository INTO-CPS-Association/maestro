package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

public class LogicBuilderFmi2Api {

    public static Fmi2Builder.LogicBuilder.Predicate isEqual(Fmi2Builder.Port a, Fmi2Builder.Port b) {
        return null;
    }


    public static <T> Fmi2Builder.LogicBuilder.Predicate isLess(T a, T b) {
        return null;
    }

    /*
    public static Fmi2Builder.LogicBuilder.Predicate isLessOrEqualTo(Fmi2Builder.ProvidesReferenceExp a, Fmi2Builder.ProvidesReferenceExp b) {
        return new PredicateFmi2Api(newALessEqualBinaryExp(a.getReferenceExp(), b.getReferenceExp()));
    }*/

    public static Fmi2Builder.LogicBuilder.Predicate isGreater(Fmi2Builder.Value<Double> a, double b) {
        return null;
    }

    public static <T> Fmi2Builder.LogicBuilder.Predicate fromValue(Fmi2Builder.Value<T> value) {
        return null;
    }
}
