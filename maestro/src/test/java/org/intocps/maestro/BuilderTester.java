package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.Fmi2AMaBLBuilder.AMaBLVariableCreator;
import org.intocps.maestro.Fmi2AMaBLBuilder.AMablBuilder;
import org.intocps.maestro.Fmi2AMaBLBuilder.AMablFmi2ComponentAPI;
import org.intocps.maestro.Fmi2AMaBLBuilder.AMablFmu2Api;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;

public class BuilderTester {

    @Test
    public void wt() throws Exception {

        InputStream is = this.getClass().getClassLoader().getResourceAsStream("buildertester/buildertester.json");
        ObjectMapper mapper = new ObjectMapper();
        Fmi2SimulationEnvironmentConfiguration simulationEnvironmentConfiguration =
                mapper.readValue(is, Fmi2SimulationEnvironmentConfiguration.class);
        Fmi2SimulationEnvironment env = Fmi2SimulationEnvironment.of(simulationEnvironmentConfiguration, new IErrorReporter.SilentReporter());

        AMablBuilder builder = new AMablBuilder(env);
        AMaBLVariableCreator variableCreator = builder.variableCreator(); // CurrentScopeVariableCreator


        // Create the two FMUs
        AMablFmu2Api controllerFMU = variableCreator.createFMU("controllerFMU", env.getModelDescription("{controllerFMU}"), new URI(""));
        AMablFmu2Api tankFMU = variableCreator.createFMU("tankFMU", env.getModelDescription("{tankFMU}"), new URI(""));

        // Create the controller and tank instanes
        AMablFmi2ComponentAPI controller = controllerFMU.create("controller");
        AMablFmi2ComponentAPI tank = tankFMU.create("tank");

        controller.getPort("valve").linkTo(tank.getPort("valvecontrol"));
        tank.getPort("level").linkTo(controller.getPort("level"));

        controller.getAndShare("valve");
        tank.set("valvecontrol");
        PStm program = builder.build();

        String test = PrettyPrinter.print(program);
        System.out.println(test);
    }
}
