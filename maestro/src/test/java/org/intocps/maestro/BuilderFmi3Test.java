package org.intocps.maestro;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.fmi.jnifmuapi.fmi3.Fmu3;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
import org.intocps.maestro.fmi3.Fmi3ModuleReferenceFmusTest;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.LoggerFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.PortFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.typechecker.TypeChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class BuilderFmi3Test {

    @BeforeAll
    public static void downloadFmus() throws IOException {
        Fmi3ModuleReferenceFmusTest.downloadReferenceFmus();

    }

    @Test
    public void wt() throws Exception {

        InputStream is = this.getClass().getClassLoader().getResourceAsStream("buildertester/buildertester.json");
        Fmi2SimulationEnvironmentConfiguration simulationEnvironmentConfiguration =
                Fmi2SimulationEnvironmentConfiguration.createFromJsonString(new String(Objects.requireNonNull(is).readAllBytes()));


        Fmi2SimulationEnvironment env = Fmi2SimulationEnvironment.of(simulationEnvironmentConfiguration, new IErrorReporter.SilentReporter());

        MablApiBuilder builder = new MablApiBuilder();

       /* Fmi2Builder.RuntimeModule<PStm> logger = builder.loadRuntimeModule("Logger");
        Fmi2Builder.RuntimeFunction func =
                builder.getFunctionBuilder().setName("log").addArgument("msg", Fmi2Builder.RuntimeFunction.FunctionType.Type.String)
                        .setReturnType(Fmi2Builder.RuntimeFunction.FunctionType.Type.Void).build();
        Fmi2Builder.RuntimeFunction func2 =
                builder.getFunctionBuilder().setName("log").addArgument("msg", Fmi2Builder.RuntimeFunction.FunctionType.Type.String)
                        .addArgument("code", Fmi2Builder.RuntimeFunction.FunctionType.Type.Int)
                        .addArgument("other", Fmi2Builder.RuntimeFunction.FunctionType.Type.Int)
                        .setReturnType(Fmi2Builder.RuntimeFunction.FunctionType.Type.Int).build();
        logger.initialize(func, func2);
        logger.call(func, "ddd");
        */

        LoggerFmi2Api logger = builder.getLogger();

        Fmi2Builder.IntVariable<PStm> v8 = builder.getDynamicScope().enterTry().enter().store(6);
        // Fmi2Builder.Variable<PStm, Object> logReturnValue = logger.call(func2, "ddd", 6, v8);


        URI ballUri = new File("target/Fmi3ModuleReferenceFmusTest/cache/BouncingBall.fmu").getAbsoluteFile().toURI();
        Fmu3 ball = new Fmu3(new File(ballUri));
        ArrayVariableFmi2Api varArray = builder.getDynamicScope().store("varArray", new Long[] {1L});
        Fmi3ModelDescription md3Ball = new Fmi3ModelDescription(ball.getModelDescription());


        FmuVariableFmi3Api ballFmu = builder.getDynamicScope().createFMU("ball", md3Ball, ballUri);

        boolean visible = true;
        boolean loggingOn = true;
        boolean eventModeUsed = true;
        boolean earlyReturnAllowed = true;
        InstanceVariableFmi3Api ballInstance = ballFmu.instantiate("ballInstance", visible, loggingOn, eventModeUsed, earlyReturnAllowed,
                varArray);

        int res = ballInstance.enterInitializationMode(false, 0.0, 0.0, true,10.0);
        res = ballInstance.exitInitializationMode();
        res = ballInstance.terminate();
        ballInstance.freeInstance();









        // Create the two FMUs
        FmuVariableFmi2Api controllerFMU = builder.getDynamicScope()
                .createFMU("controllerFMU", (Fmi2ModelDescription) env.getModelDescription("{controllerFMU}"),
                        env.getUriFromFMUName("{controllerFMU" + "}"));
        FmuVariableFmi2Api tankFMU = builder.getDynamicScope()
                .createFMU("tankFMU", (Fmi2ModelDescription) env.getModelDescription("{tankFMU}"), env.getUriFromFMUName("{tankFMU}"));


        // Create the controller and tank instances
        ComponentVariableFmi2Api controller = controllerFMU.instantiate("controller");
        ComponentVariableFmi2Api tank = tankFMU.instantiate("tank");
        DynamicActiveBuilderScope dynamicScope = builder.getDynamicScope();

        tank.setupExperiment(0d, 10d, null);
        controller.setupExperiment(0d, 10d, null);

        controller.enterInitializationMode();
        tank.enterInitializationMode();

        controller.exitInitializationMode();
        tank.exitInitializationMode();
 /*
        IMablScope scope1 = dynamicScope.getActiveScope();
        for (int i = 0; i < 4; i++) {
            dynamicScope.enterIf(null);
            AMablFmi2ComponentAPI tank2 = tankFMU.create("tank");
        }
        scope1.activate();*/
        ComponentVariableFmi2Api tank2 = tankFMU.instantiate("tank");


        controller.getPort("valve").linkTo(tank.getPort("valvecontrol"));
        tank.getPort("level").linkTo(controller.getPort("level"));

        Map<PortFmi2Api, VariableFmi2Api<Object>> allVars = tank.get();
        tank.share(allVars);


        controller.getAndShare("valve");
        controller.getAndShare();

        tank.setLinked();
        //        tank.set();
        Fmi2Builder.DoubleVariable<PStm> var = dynamicScope.store(123.123);
        Fmi2Builder.DoubleVariable<PStm> current_time_point = dynamicScope.store(0.0);
        Fmi2Builder.DoubleVariable<PStm> step = dynamicScope.store(0.1);
        tank.step(current_time_point, step);
        //Fmi2Builder.StateVariable<PStm> s = tank.getState();

        logger.warn("Something is wrong %f -- %f. Fmu %s, Instance %s", 1.3, step, controllerFMU, controller);
        //s.set();
        //s.destroy();

        //no this will not stay this way
        // tank.set(tank.getPort("valvecontrol"), new AMablValue(newBoleanType(), true));

        var.set(456.678);
        //        PortVariableMapImpl<Fmi2Builder.DoubleValue> allVars2 = new PortVariableMapImpl<>();
        //        allVars2.put(allVars.keySet().iterator().next(), var);
        //tank.set(allVars);


        //        controllerFMU.unload();
        //        tankFMU.unload();
        // logger.destroy();
        ASimulationSpecificationCompilationUnit program = builder.build();

        String test = PrettyPrinter.print(program);

        System.out.println(PrettyPrinter.printLineNumbers(program));

        File workingDirectory = new File("target/apitest");
        workingDirectory.mkdirs();
        File specFile = new File(workingDirectory, "m.mabl");
        FileUtils.write(specFile, test, StandardCharsets.UTF_8);
        Mabl mabl = new Mabl(workingDirectory, workingDirectory);
        IErrorReporter reporter = new ErrorReporter();
        mabl.setReporter(reporter);
        mabl.setVerbose(true);

        mabl.parse(Collections.singletonList(specFile));

        var tcRes = mabl.typeCheck();
        mabl.verify(Framework.FMI2);
        if (reporter.getErrorCount() > 0) {
            reporter.printErrors(new PrintWriter(System.err, true));
            Assertions.fail();
        }
        mabl.dump(workingDirectory);
        new MableInterpreter(new DefaultExternalValueFactory(workingDirectory, name -> TypeChecker.findModule(tcRes.getValue(), name),
                IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8))).execute(mabl.getMainSimulationUnit());

    }

    public interface Ext {
        //void initialize(Fmi2Builder.Variable owner, List<ExtFunc> declaredFuncs);

        //not sure how to allow a mix of double, int and var except for object
        void callVoid(ExtFunc func, Object... args);

        void call(ExtFunc func, Object... args);

        void destroy();
    }

    public interface ExtFunc {
        String getName();

        String getArgNames();

        Class<Object> getArgTypes();
    }

    static class Logger extends ExternalVariable {
        final ExtFunc logFunc = null;

        void log(Level level, String message, Fmi2Builder.IntVariable<PStm> errorCode) {
            this.callVoid(logFunc, level == Level.Debug ? 1 : 0, message, errorCode);
        }

        enum Level {
            Info,
            Debug
        }
    }

    static class ExternalVariable implements Ext {

        @Override
        public void callVoid(ExtFunc func, Object... args) {

        }

        @Override
        public void call(ExtFunc func, Object... args) {

        }

        @Override
        public void destroy() {

        }
    }
}