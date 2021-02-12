package org.intocps.maestro;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.values.BooleanExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.DoubleExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.FmuVariableFmi2Api;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WatertankApiTest {

    @Test
    public void test() throws Exception {
        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = true;
        MablApiBuilder builder = new MablApiBuilder(settings);

        DynamicActiveBuilderScope ds = builder.getDynamicScope();

        FmuVariableFmi2Api controllerFmu =
                ds.createFMU("controller", "FMI2", Paths.get("src", "test", "resources", "watertankcontroller-c.fmu").toUri().toASCIIString());

        FmuVariableFmi2Api tankFmu =
                ds.createFMU("tank", "FMI2", Paths.get("src", "test", "resources", "singlewatertank-20sim.fmu").toUri().toASCIIString());


        ComponentVariableFmi2Api controller = controllerFmu.instantiate("controller");
        ComponentVariableFmi2Api tank = tankFmu.instantiate("tank");

        controller.getPort("valve").linkTo(tank.getPort("valvecontrol"));
        tank.getPort("level").linkTo(controller.getPort("level"));

        //interpreter issue 0 does not work for real
        tank.set(tank.getPort("level"), IntExpressionValue.of(0));
        tank.set(tank.getPort("level"), DoubleExpressionValue.of(0.0));
        controller.set(controller.getPort("valve"), BooleanExpressionValue.of(false));

        List<ComponentVariableFmi2Api> ins = Arrays.asList(tank, controller);

        ins.forEach(i -> i.setupExperiment(0d, 11d, null));

        ins.forEach(ComponentVariableFmi2Api::enterInitializationMode);

        tank.getAndShare();
        controller.getAndShare();
        ins.forEach(ComponentVariableFmi2Api::exitInitializationMode);

        DoubleVariableFmi2Api time = ds.store(0d);
        DoubleVariableFmi2Api step = ds.store(0.1);
        ScopeFmi2Api whileScope = ds.enterWhile(time.toMath().lessThan(DoubleExpressionValue.of(10d))).activate();

        tank.setLinked();
        controller.setLinked();
        tank.step(time, step);
        controller.step(time, step);

        tank.getAndShare();
        controller.getAndShare();
        time.setValue(time.toMath().addition(step));
        whileScope.leave();

        //How to write controller != null
        //  ds.enterIf(controller.toMath().)
        controllerFmu.unload();
        tankFmu.unload();

        check(builder.build());
    }

    private void check(ASimulationSpecificationCompilationUnit program) throws Exception {
        String test = PrettyPrinter.print(program);

        System.out.println(PrettyPrinter.printLineNumbers(program));

        File workingDirectory = new File("target/" + this.getClass().getSimpleName());
        workingDirectory.mkdirs();
        File specFile = new File(workingDirectory, "m.mabl");
        FileUtils.write(specFile, test, StandardCharsets.UTF_8);
        Mabl mabl = new Mabl(workingDirectory, workingDirectory);
        IErrorReporter reporter = new ErrorReporter();
        mabl.setReporter(reporter);
        mabl.setVerbose(true);

        mabl.parse(Collections.singletonList(specFile));

        mabl.typeCheck();
        mabl.verify(Framework.FMI2);
        if (reporter.getErrorCount() > 0) {
            reporter.printErrors(new PrintWriter(System.err, true));
            Assert.fail();
        }
        mabl.dump(workingDirectory);
        new MableInterpreter(
                new DefaultExternalValueFactory(workingDirectory, IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8)))
                .execute(mabl.getMainSimulationUnit());
    }
}
