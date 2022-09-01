package org.intocps.maestro.interpreter.values.simulationcontrol;

import org.intocps.maestro.interpreter.values.BooleanValue;
import org.intocps.maestro.interpreter.values.ExternalModuleValue;
import org.intocps.maestro.interpreter.values.FunctionValue;
import org.intocps.maestro.interpreter.values.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SimulationControlValue extends ExternalModuleValue<Object> {


    public SimulationControlValue(Supplier<Boolean> isStopped) {
        super(createMembers(isStopped), null);
    }

    public SimulationControlValue() {
        this(() -> false);
    }


    static Map<String, Value> createMembers(Supplier<Boolean> isStopped) {
        Map<String, Value> componentMembers = new HashMap<>();

        componentMembers.put("stopRequested", new FunctionValue.ExternalFunctionValue(fcargs -> new BooleanValue(isStopped.get())));
        return componentMembers;
    }


}
