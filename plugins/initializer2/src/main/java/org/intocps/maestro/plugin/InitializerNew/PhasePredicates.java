package org.intocps.maestro.plugin.InitializerNew;

import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.function.Predicate;

public class PhasePredicates{
    public static Predicate<ModelDescription.ScalarVariable> IniPhase() {
        return o -> (o.initial == ModelDescription.Initial.Exact || o.initial == ModelDescription.Initial.Approx) &&
                o.variability != ModelDescription.Variability.Constant ||
                o.causality == ModelDescription.Causality.Parameter && o.variability == ModelDescription.Variability.Tunable;
    }

    public static Predicate<ModelDescription.ScalarVariable> IniePhase() {
        return o -> o.initial == ModelDescription.Initial.Exact && o.variability != ModelDescription.Variability.Constant;
    }

    public static Predicate<ModelDescription.ScalarVariable> InPhase() {
        return o -> o.causality == ModelDescription.Causality.Input ||
                o.causality == ModelDescription.Causality.Parameter && o.variability == ModelDescription.Variability.Tunable;
    }

    public static Predicate<ModelDescription.ScalarVariable> InitPhase() {
        return o -> o.causality == ModelDescription.Causality.Output;
    }
}
