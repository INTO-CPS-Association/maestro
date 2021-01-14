package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.AMaBLVariableCreator;
import org.intocps.maestro.framework.fmi2.api.mabl.AMablValue;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.AMablFmi2ComponentAPI;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.AMablFmu2Api;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import static org.intocps.maestro.ast.MableAstFactory.newBoleanType;

public class BuilderTester {

    @Test
    public void wt() throws Exception {

        InputStream is = this.getClass().getClassLoader().getResourceAsStream("buildertester/buildertester.json");
        ObjectMapper mapper = new ObjectMapper();
        Fmi2SimulationEnvironmentConfiguration simulationEnvironmentConfiguration =
                mapper.readValue(is, Fmi2SimulationEnvironmentConfiguration.class);
        Fmi2SimulationEnvironment env = Fmi2SimulationEnvironment.of(simulationEnvironmentConfiguration, new IErrorReporter.SilentReporter());

        MablApiBuilder builder = new MablApiBuilder(env);
        AMaBLVariableCreator variableCreator = builder.variableCreator(); // CurrentScopeVariableCreator


        // Create the two FMUs
        AMablFmu2Api controllerFMU = variableCreator.createFMU("controllerFMU", env.getModelDescription("{controllerFMU}"), new URI(""));
        AMablFmu2Api tankFMU = variableCreator.createFMU("tankFMU", env.getModelDescription("{tankFMU}"), new URI(""));

        // Create the controller and tank instanes
        AMablFmi2ComponentAPI controller = controllerFMU.create("controller");
        AMablFmi2ComponentAPI tank = tankFMU.create("tank");

      /*  IMablScope scope1 = builder.getDynamicScope().getActiveScope();
        for (int i = 0; i < 4; i++) {
            builder.getDynamicScope().enterIf(null);
            AMablFmi2ComponentAPI tank2 = tankFMU.create("tank");
        }
        scope1.activate();
        AMablFmi2ComponentAPI tank2 = tankFMU.create("tank");
*/

        controller.getPort("valve").linkTo(tank.getPort("valvecontrol"));
        tank.getPort("level").linkTo(controller.getPort("level"));

        Map<Fmi2Builder.Port, Fmi2Builder.Variable> allVars = tank.get();
        tank.share(allVars);


        controller.getAndShare("valve");
        controller.getAndShare();
        tank.set();
        Fmi2Builder.DoubleVariable<PStm> var = builder.getDynamicScope().store(123.123);

        //no this will not stay this way
        tank.set(tank.getPort("valvecontrol"), new AMablValue(newBoleanType(), true));

        var.set(456.678);

        PStm program = builder.build();

        String test = PrettyPrinter.print(program);
        System.out.println(test);
    }
}
