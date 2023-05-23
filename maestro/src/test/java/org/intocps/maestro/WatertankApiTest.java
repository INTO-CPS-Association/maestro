package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.codegen.mabl2cpp.MablCppCodeGenerator;
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
import org.intocps.maestro.typechecker.TypeChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

public class WatertankApiTest {

    @Test
    public void test() throws Exception {
        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = true;
        MablApiBuilder builder = new MablApiBuilder(settings);

        DynamicActiveBuilderScope ds = builder.getDynamicScope();

        FmuVariableFmi2Api controllerFmu = ds.createFMU("controllerFmu", "FMI2",
                Paths.get("src", "test", "resources", "watertankcontroller-c.fmu").toUri().toASCIIString());

        FmuVariableFmi2Api tankFmu = ds.createFMU("tankFmu", "FMI2",
                Paths.get("src", "test", "resources", "singlewatertank-20sim.fmu").toUri().toASCIIString());


        ComponentVariableFmi2Api controller = controllerFmu.instantiate("controller");
        ComponentVariableFmi2Api tank = tankFmu.instantiate("tank");

        controller.getPort("valve").linkTo(tank.getPort("valvecontrol"));
        tank.getPort("level").linkTo(controller.getPort("level"));

        //interpreter issue 0 does not work for real
        tank.set(tank.getPort("level"), IntExpressionValue.of(0));
        tank.set(tank.getPort("level"), DoubleExpressionValue.of(0.0));
        controller.set(controller.getPort("valve"), BooleanExpressionValue.of(false));

        controller.set(controller.getPort("minlevel"), builder.getExecutionEnvironment().getInt("lowerlevel"));
        controller.set(controller.getPort("maxlevel"), builder.getExecutionEnvironment().getReal("upperlevel"));


        List<ComponentVariableFmi2Api> ins = Arrays.asList(tank, controller);

        ins.forEach(i -> i.setupExperiment(0d, 11d, null));

        ins.forEach(ComponentVariableFmi2Api::enterInitializationMode);

        tank.getAndShare();
        controller.getAndShare();
        ins.forEach(ComponentVariableFmi2Api::exitInitializationMode);

        DoubleVariableFmi2Api time = ds.store("time", 0d);
        DoubleVariableFmi2Api step = ds.store("step_size", 0.1);

        ScopeFmi2Api whileScope = ds.enterWhile(time.toMath().addition(step).lessThan(DoubleExpressionValue.of(10d))).activate();

        tank.setLinked();
        controller.setLinked();
        tank.step(time, step);
        controller.step(time, step);

        tank.getAndShare();
        controller.getAndShare();
        time.setValue(time.toMath().addition(step));
        whileScope.leave();


        Map<String, Object> data = new HashMap<>();

        data.put("DataWriter", Arrays.asList(new HashMap<>() {
            {
                put("type", "CSV");
                put("filename", "output.csv");
            }
        }));

        data.put("environment_variables", new HashMap() {
            {
                put("lowerlevel", 1);
                put("upperlevel", 3.3);
            }
        });

        check(builder.build(), new ObjectMapper().writeValueAsString(data));
    }

    private void check(ASimulationSpecificationCompilationUnit program, String runtimeData) throws Exception {
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

        Map.Entry<Boolean, Map<INode, PType>> tcRef = mabl.typeCheck();
        mabl.verify(Framework.FMI2);
        if (reporter.getErrorCount() > 0) {
            reporter.printErrors(new PrintWriter(System.err, true));
            Assertions.fail();
        }
        mabl.dump(workingDirectory);

        new MableInterpreter(new DefaultExternalValueFactory(workingDirectory, name -> TypeChecker.findModule(tcRef.getValue(), name),
                IOUtils.toInputStream(runtimeData, StandardCharsets.UTF_8))).execute(mabl.getMainSimulationUnit());

        MablCppCodeGenerator cppCodeGenerator = new MablCppCodeGenerator(workingDirectory);
        cppCodeGenerator.generate(mabl.getMainSimulationUnit(), tcRef.getValue());
    }
}
