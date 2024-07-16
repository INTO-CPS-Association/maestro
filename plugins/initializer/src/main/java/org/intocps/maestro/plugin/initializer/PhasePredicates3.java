package org.intocps.maestro.plugin.initializer;


import org.intocps.maestro.fmi.ModelDescription;
import org.intocps.maestro.fmi.fmi3.Fmi3Causality;
import org.intocps.maestro.fmi.fmi3.Fmi3Variable;

import java.util.function.Predicate;

public class PhasePredicates3 {

    public static Predicate<Fmi3Variable> iniPhase() {
        return o -> ((o.getInitial() == ModelDescription.Initial.Exact || o.getInitial() == ModelDescription.Initial.Approx) &&
                o.getVariability() != ModelDescription.Variability.Constant) ||
                (o.getCausality() == Fmi3Causality.Parameter && o.getVariability() == ModelDescription.Variability.Tunable);
    }


    public static Predicate<Fmi3Variable> inPhase() {
        return o -> (o.getCausality() == Fmi3Causality.Input && o.getInitial() == ModelDescription.Initial.Calculated ||
                o.getCausality() == Fmi3Causality.Parameter && o.getVariability() == ModelDescription.Variability.Tunable);
    }
}
