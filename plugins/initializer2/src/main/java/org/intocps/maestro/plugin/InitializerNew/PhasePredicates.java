package org.intocps.maestro.plugin.InitializerNew;

import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.function.Predicate;

import static org.intocps.orchestration.coe.modeldefinition.ModelDescription.*;

public class PhasePredicates{
    public static Predicate<ScalarVariable> IniPhase() {
        return o -> ((o.initial == Initial.Exact || o.initial == Initial.Approx) && o.variability != Variability.Constant)
                 || (o.causality == Causality.Parameter && o.variability == Variability.Tunable);
    }

    public static Predicate<ScalarVariable> IniePhase() {
        return o -> o.initial == Initial.Exact && o.variability != Variability.Constant;
    }

    public static Predicate<ScalarVariable> InPhase() {
        return o -> o.causality == Causality.Parameter && o.variability == Variability.Tunable;
    }

    public static Predicate<ScalarVariable> InitPhase() {
        return o -> o.causality == Causality.Output;
    }
}
