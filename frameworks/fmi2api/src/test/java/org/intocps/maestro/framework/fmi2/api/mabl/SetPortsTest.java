package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.PortValueMapImpl;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.FmuVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class SetPortsTest {
    @Test
    public void undeclaredPortsTest() throws Exception {
        // ARRANGE
        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        MablApiBuilder builder = new MablApiBuilder(settings);
        Path tankFmuPath = Paths.get("src", "test", "resources", "set_wrong_port_test", "singlewatertank-20sim.fmu");
        Path controllerFmuPath = Paths.get("src", "test", "resources", "set_wrong_port_test", "watertankcontroller-c.fmu");

        DynamicActiveBuilderScope scope = builder.getDynamicScope();

        FmuVariableFmi2Api tankFMU = scope.createFMU("Tank", "FMI2", tankFmuPath.toUri().toASCIIString());
        FmuVariableFmi2Api controllerFMU = scope.createFMU("Controller", "FMI2", controllerFmuPath.toUri().toASCIIString());

        ComponentVariableFmi2Api tankInstance = tankFMU.instantiate("wtInstance");
        ComponentVariableFmi2Api controllerInstance = controllerFMU.instantiate("wtInstance");

        // ACT
        Map<PortFmi2Api, VariableFmi2Api<Object>> controllerPortMap =
                controllerInstance.get(controllerInstance.getPorts().stream().map(PortFmi2Api::getName).toArray(String[]::new));
        controllerInstance.share(controllerPortMap);

        Fmi2Builder.Fmi2ComponentVariable.PortValueMap<Object> portsToSet = new PortValueMapImpl(controllerPortMap);

        // ASSERT
        Assertions.assertThrows(RuntimeException.class,() -> tankInstance.set(portsToSet));
    }
}
