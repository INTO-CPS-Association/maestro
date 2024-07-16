package org.intocps.maestro.framework.fmi2.api;

import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;

public class Factory {

    public FmiBuilder get(Types type) {
        switch (type) {
            case Mabl:
            default:
                return new MablApiBuilder();
        }
    }

    public enum Types {
        PlantUml,
        Mabl
    }
}
