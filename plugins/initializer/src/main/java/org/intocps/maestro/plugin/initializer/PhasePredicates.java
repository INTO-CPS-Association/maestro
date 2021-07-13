package org.intocps.maestro.plugin.initializer;

import org.intocps.maestro.fmi.Fmi2ModelDescription;

import static org.intocps.maestro.fmi.Fmi2ModelDescription.*;

import java.util.function.Predicate;



public class PhasePredicates {
    public static Predicate<Fmi2ModelDescription.ScalarVariable> iniPhase() {
        return o -> ((o.initial == Initial.Exact || o.initial == Initial.Approx) && o.variability != Variability.Constant) ||
                (o.causality == Causality.Parameter && o.variability == Variability.Tunable);
    }

    public static Predicate<ScalarVariable> iniePhase() {
        return o -> o.initial == Initial.Exact && o.variability != Variability.Constant;
    }

    public static Predicate<ScalarVariable> inPhase() {
        return o -> (o.causality == Causality.Input && o.initial == Initial.Calculated || o.causality == Causality.Parameter && o.variability == Variability.Tunable);
    }

    public static Predicate<ScalarVariable> initPhase() {
        return o -> o.causality == Causality.Output;
    }
}