package org.intocps.maestro.framework.fmi2.api;

import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;

public class Factory {

    public Fmi2Builder get(Types type, Fmi2SimulationEnvironment env) {
        switch (type) {
            case Mabl:
            default:
                return new MablApiBuilder(env);
        }
    }

    public enum Types {
        PlantUml,
        Mabl
    }
}
