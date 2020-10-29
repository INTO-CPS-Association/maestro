package org.intocps.maestro.plugin.Initializer;

import java.util.function.Predicate;

import static org.intocps.orchestration.coe.modeldefinition.ModelDescription.*;

public class PhasePredicates {
    public static Predicate<ScalarVariable> iniPhase() {
        return o -> ((o.initial == Initial.Exact || o.initial == Initial.Approx) && o.variability != Variability.Constant) ||
                (o.causality == Causality.Parameter && o.variability == Variability.Tunable);
    }

    public static Predicate<ScalarVariable> iniePhase() {
        return o -> o.initial == Initial.Exact && o.variability != Variability.Constant;
    }

    public static Predicate<ScalarVariable> inPhase() {
        return o -> o.causality == Causality.Parameter && o.variability == Variability.Tunable;
    }

    public static Predicate<ScalarVariable> initPhase() {
        return o -> o.causality == Causality.Output;
    }
}