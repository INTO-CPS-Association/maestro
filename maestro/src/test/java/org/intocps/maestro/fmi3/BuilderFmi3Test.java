package org.intocps.maestro.fmi3;

import org.antlr.v4.runtime.CharStreams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.fmi.FmuInvocationException;
import org.intocps.fmi.jnifmuapi.fmi3.Fmu3;
import org.intocps.maestro.Mabl;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.fmi.fmi3.Fmi3Causality;
import org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
import org.intocps.maestro.fmi.fmi3.Fmi3TypeEnum;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.PortFmi3Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IfMaBlScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.BooleanExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.parser.MablParserUtil;
import org.intocps.maestro.typechecker.TypeChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BuilderFmi3Test {
    @BeforeAll
    public static void before() throws IOException {
        Fmi3ModuleReferenceFmusTest.downloadReferenceFmus();
    }


    public static InstanceVariableFmi3Api createInstance(MablApiBuilder builder, String name, URI uri) throws Exception {

        Fmi3ModelDescription md = new Fmi3ModelDescription(new Fmu3(new File(uri)).getModelDescription());

        FmuVariableFmi3Api fmu = builder.getDynamicScope().createFMU(name + "Fmu", md, uri);

        boolean visible = true;
        boolean loggingOn = true;
        boolean eventModeUsed = true;
        boolean earlyReturnAllowed = true;
        ArrayVariableFmi2Api requiredIntermediateVariables = builder.getDynamicScope().store("requiredIntermediateVariables", new Long[]{1L});
        InstanceVariableFmi3Api instance =
                fmu.instantiate(name, visible, loggingOn, eventModeUsed, earlyReturnAllowed, requiredIntermediateVariables);

        return instance;
    }

    @Test
    public void test() throws Exception {
        MablApiBuilder builder = new MablApiBuilder();

        InstanceVariableFmi3Api fd = createInstance(builder, "fd",
                new File("target/Fmi3ModuleReferenceFmusTest/cache/Feedthrough.fmu").getAbsoluteFile().toURI());
        InstanceVariableFmi3Api sg = createInstance(builder, "sg",
                new File("src/test/resources/fmi3/reference/siggen-feedthrough/SignalGenerator.fmu").getAbsoluteFile().toURI());


//        fd.enterInitializationMode(false, 0.0, 0.0, true, 10.0);
//        sg.enterInitializationMode(false, 0.0, 0.0, true, 10.0);

        List<PortFmi3Api> sgOutputs = sg.getPorts().stream().filter(p -> p.scalarVariable.getVariable().getCausality() == Fmi3Causality.Output)
                .collect(Collectors.toList());

        sgOutputs.stream().map(PortFmi3Api::getName).forEach(System.out::println);

//        for (PortFmi3Api o : sgOutputs) {
//            sg.get(o);
//        }

        System.out.println("Linked ports");
        sg.getPorts().stream().filter(PortFmi3Api::isLinked).forEach(System.out::println);
        System.out.println("---Linked ports");
        sg.getPort("Int8_output").linkTo(fd.getPort("Int8_input"));
        sg.getPort("UInt8_output").linkTo(fd.getPort("UInt8_input"));
        System.out.println("Linked ports");
        sg.getPorts().stream().filter(PortFmi3Api::isLinked).forEach(System.out::println);
        System.out.println("---Linked ports");
        sg.getAndShare();
        fd.setLinked();

//        fd.exitInitializationMode();

        ASimulationSpecificationCompilationUnit program = builder.build();

//        String test = PrettyPrinter.print(program);

        System.out.println(PrettyPrinter.printLineNumbers(program));
    }

    @Test
    public void testClocks() throws Exception {
        MablApiBuilder builder = new MablApiBuilder();
        DynamicActiveBuilderScope scope = builder.getDynamicScope();

        InstanceVariableFmi3Api instance = createInstance(builder, "clocks",
                new File("target/Fmi3ModuleReferenceFmusTest/cache/Clocks.fmu").getAbsoluteFile().toURI());


//        fd.enterInitializationMode(false, 0.0, 0.0, true, 10.0);
//        sg.enterInitializationMode(false, 0.0, 0.0, true, 10.0);

        List<PortFmi3Api> sgOutputs = instance.getPorts().stream().filter(p -> p.scalarVariable.getVariable().getCausality() == Fmi3Causality.Output)
                .collect(Collectors.toList());

        List<PortFmi3Api> clocks = instance.getPorts().stream().filter(p -> p.getSourceObject().getVariable().getTypeIdentifier() == Fmi3TypeEnum.ClockType)
                .collect(Collectors.toList());

        sgOutputs.stream().map(PortFmi3Api::getName).forEach(System.out::println);

        instance.enterEventMode();

        FmiBuilder.IntVariable<PStm> nEventIndicators = builder.getDynamicScope().store(0);
        instance.getNumberOfEventIndicators(builder.getDynamicScope(), nEventIndicators);


        ArrayVariableFmi2Api<UIntVariableFmi2Api> eventIndicators = builder.getDynamicScope()
                .createArray("eventIndicators", UIntVariableFmi2Api.class, nEventIndicators);

//        FmiBuilder.ArrayVariable<PStm, Long> eventIndicators=builder.getDynamicScope().store(builder.n).storeInArray();
        instance.getEventIndicators(scope, eventIndicators, nEventIndicators);


        List<PortFmi3Api> outClocks = clocks.stream().filter(p -> p.scalarVariable.getVariable().getCausality() == Fmi3Causality.Output)
                .collect(Collectors.toList());
        FmiBuilder.IntVariable<PStm> nvr = scope.store("clock_get_nvr", outClocks.size());
        ArrayVariableFmi2Api<UIntVariableFmi2Api> vrs = scope.createArray("clock_get_vrs", UIntVariableFmi2Api.class, nvr);
        for (int i = 0; i < outClocks.size(); i++) {
            vrs.setValue(new IntExpressionValue(i), new IntExpressionValue((int) outClocks.get(i).scalarVariable.getVariable().getValueReferenceAsLong()));
        }
        ArrayVariableFmi2Api<BooleanVariableFmi2Api> triggeredClocks = scope.createArray("clock_get_vrs", BooleanVariableFmi2Api.class, nvr);
        instance.getClock(vrs, nvr, triggeredClocks);


        List<PortFmi3Api> inClocks = clocks.stream().filter(p -> p.scalarVariable.getVariable().getCausality() == Fmi3Causality.Input)
                .collect(Collectors.toList());
        FmiBuilder.IntVariable<PStm> clock_set_nvr = scope.store("clock_in_nvr", inClocks.size());
        ArrayVariableFmi2Api<UIntVariableFmi2Api> clock_set_vrs = scope.createArray("clock_in_vrs", UIntVariableFmi2Api.class, nvr);
        ArrayVariableFmi2Api<BooleanVariableFmi2Api> clock_set_Clocks = scope.createArray("clock_in_vrs", BooleanVariableFmi2Api.class, nvr);
        for (int i = 0; i < inClocks.size(); i++) {
            clock_set_vrs.setValue(new IntExpressionValue(i),
                    new IntExpressionValue((int) inClocks.get(i).scalarVariable.getVariable().getValueReferenceAsLong()));
            clock_set_Clocks.setValue(new IntExpressionValue(i), new BooleanExpressionValue(true));
        }

        instance.setClock(clock_set_vrs, clock_set_nvr, clock_set_Clocks);


        FmiBuilder.DoubleVariable<PStm> currentCommunicationPoint = scope.store("time", 0d);
        FmiBuilder.DoubleVariable<PStm> stepSize = scope.store("step", 0.1d);
        Map.Entry<FmiBuilder.BoolVariable<PStm>, InstanceVariableFmi3Api.StepResult> stepRes = instance.step(scope, currentCommunicationPoint,
                stepSize, new ABoolLiteralExp(false));

//        stepRes.getValue().getLastSuccessfulTime()

//        instance.g
//        for (PortFmi3Api o : sgOutputs) {
//            sg.get(o);
//        }

//        System.out.println("Linked ports");
//        sg.getPorts().stream().filter(PortFmi3Api::isLinked).forEach(System.out::println);
//        System.out.println("---Linked ports");
//        sg.getPort("Int8_output").linkTo(fd.getPort("Int8_input"));
//        sg.getPort("UInt8_output").linkTo(fd.getPort("UInt8_input"));
//        System.out.println("Linked ports");
//        sg.getPorts().stream().filter(PortFmi3Api::isLinked).forEach(System.out::println);
//        System.out.println("---Linked ports");
//        sg.getAndShare();
//        fd.setLinked();

//        fd.exitInitializationMode();

        ASimulationSpecificationCompilationUnit program = builder.build();

//        String test = PrettyPrinter.print(program);

        System.out.println(PrettyPrinter.printLineNumbers(program));
    }

    @Test
    @Disabled
    public void testSimulateClocks() throws Exception {
        MablApiBuilder builder = new MablApiBuilder();
        DynamicActiveBuilderScope scope = builder.getDynamicScope();

        InstanceVariableFmi3Api instance = createInstance(builder, "clocks",
                new File("src/test/resources/fmi3/sinewave_array.fmu").getAbsoluteFile().toURI());


//        fd.enterInitializationMode(false, 0.0, 0.0, true, 10.0);
//        sg.enterInitializationMode(false, 0.0, 0.0, true, 10.0);

        List<PortFmi3Api> sgOutputs = instance.getPorts().stream().filter(p -> p.scalarVariable.getVariable().getCausality() == Fmi3Causality.Output)
                .collect(Collectors.toList());

        List<PortFmi3Api> clocks = instance.getPorts().stream().filter(p -> p.getSourceObject().getVariable().getTypeIdentifier() == Fmi3TypeEnum.ClockType)
                .collect(Collectors.toList());

        sgOutputs.stream().map(PortFmi3Api::getName).forEach(System.out::println);

        instance.enterEventMode();

        FmiBuilder.IntVariable<PStm> nEventIndicators = builder.getDynamicScope().store(0);
        instance.getNumberOfEventIndicators(builder.getDynamicScope(), nEventIndicators);


        ArrayVariableFmi2Api<UIntVariableFmi2Api> eventIndicators = builder.getDynamicScope()
                .createArray("eventIndicators", UIntVariableFmi2Api.class, nEventIndicators);

//        FmiBuilder.ArrayVariable<PStm, Long> eventIndicators=builder.getDynamicScope().store(builder.n).storeInArray();
        instance.getEventIndicators(scope, eventIndicators, nEventIndicators);


        List<PortFmi3Api> outClocks = clocks.stream().filter(p -> p.scalarVariable.getVariable().getCausality() == Fmi3Causality.Output)
                .collect(Collectors.toList());
        FmiBuilder.IntVariable<PStm> nvr = scope.store("clock_get_nvr", outClocks.size());
        ArrayVariableFmi2Api<UIntVariableFmi2Api> vrs = scope.createArray("clock_get_vrs", UIntVariableFmi2Api.class, nvr);
        for (int i = 0; i < outClocks.size(); i++) {
            vrs.setValue(new IntExpressionValue(i), new IntExpressionValue((int) outClocks.get(i).scalarVariable.getVariable().getValueReferenceAsLong()));
        }
        ArrayVariableFmi2Api<BooleanVariableFmi2Api> triggeredClocks = scope.createArray("clock_get_vrs", BooleanVariableFmi2Api.class, nvr);
        instance.getClock(vrs, nvr, triggeredClocks);


        List<PortFmi3Api> inClocks = clocks.stream().filter(p -> p.scalarVariable.getVariable().getCausality() == Fmi3Causality.Input)
                .collect(Collectors.toList());
        FmiBuilder.IntVariable<PStm> clock_set_nvr = scope.store("clock_in_nvr", inClocks.size());
        ArrayVariableFmi2Api<UIntVariableFmi2Api> clock_set_vrs = scope.createArray("clock_in_vrs", UIntVariableFmi2Api.class, nvr);
        ArrayVariableFmi2Api<BooleanVariableFmi2Api> clock_set_Clocks = scope.createArray("clock_in_vrs", BooleanVariableFmi2Api.class, nvr);
        for (int i = 0; i < inClocks.size(); i++) {
            clock_set_vrs.setValue(new IntExpressionValue(i),
                    new IntExpressionValue((int) inClocks.get(i).scalarVariable.getVariable().getValueReferenceAsLong()));
            clock_set_Clocks.setValue(new IntExpressionValue(i), new BooleanExpressionValue(true));
        }

        instance.setClock(clock_set_vrs, clock_set_nvr, clock_set_Clocks);


        FmiBuilder.DoubleVariable<PStm> currentCommunicationPoint = scope.store("time", 0d);
        FmiBuilder.DoubleVariable<PStm> stepSize = scope.store("step", 0.1d);
        Map.Entry<FmiBuilder.BoolVariable<PStm>, InstanceVariableFmi3Api.StepResult> stepRes = instance.step(scope, currentCommunicationPoint,
                stepSize, new ABoolLiteralExp(false));

        IfMaBlScope eventHandlingScope = scope.enterIf(
                stepRes.getValue().getEventHandlingNeeded().toPredicate());

        instance.enterEventMode();


//handle events
        BooleanVariableFmi2Api discreteStatesNeedUpdate = scope.store("discreteStatesNeedUpdate",false);
        BooleanVariableFmi2Api terminateSimulation = scope.store("terminateSimulation",false);
        BooleanVariableFmi2Api nominalsOfContinuousStatesChanged = scope.store("nominalsOfContinuousStatesChanged",false);
        BooleanVariableFmi2Api valuesOfContinuousStatesChanged = scope.store("valuesOfContinuousStatesChanged",false);
        BooleanVariableFmi2Api nextEventTimeDefined = scope.store("nextEventTimeDefined",false);
        DoubleVariableFmi2Api nextEventTime = scope.store("nextEventTime",0d);
        instance.updateDiscreteStates(scope, discreteStatesNeedUpdate, terminateSimulation, nominalsOfContinuousStatesChanged, valuesOfContinuousStatesChanged,
                nextEventTimeDefined, nextEventTime);

        instance.enterStepMode();


//        stepRes.getValue().getLastSuccessfulTime()

//        instance.g
//        for (PortFmi3Api o : sgOutputs) {
//            sg.get(o);
//        }

//        System.out.println("Linked ports");
//        sg.getPorts().stream().filter(PortFmi3Api::isLinked).forEach(System.out::println);
//        System.out.println("---Linked ports");
//        sg.getPort("Int8_output").linkTo(fd.getPort("Int8_input"));
//        sg.getPort("UInt8_output").linkTo(fd.getPort("UInt8_input"));
//        System.out.println("Linked ports");
//        sg.getPorts().stream().filter(PortFmi3Api::isLinked).forEach(System.out::println);
//        System.out.println("---Linked ports");
//        sg.getAndShare();
//        fd.setLinked();

//        fd.exitInitializationMode();

        ASimulationSpecificationCompilationUnit program = builder.build();

//        String test = PrettyPrinter.print(program);

        System.out.println(PrettyPrinter.printLineNumbers(program));

        File workingDirectory=getWorkingDirectory(null,this.getClass());
        File specFile = new File(workingDirectory, "spec.mabl");
        FileUtils.write(specFile,PrettyPrinter.print(program),StandardCharsets.UTF_8);

        IErrorReporter reporter = new ErrorReporter();
        Mabl mabl = new Mabl(workingDirectory, workingDirectory);
        mabl.setReporter(reporter);
//        mabl.setVerbose(getMablVerbose());
mabl.parse(Collections.singletonList(specFile));
        mabl.expand();
        var tcRes = mabl.typeCheck();
        mabl.verify(Framework.FMI2);


        if (mabl.getReporter().getErrorCount() > 0) {
            mabl.getReporter().printErrors(new PrintWriter(System.err, true));
            Assertions.fail();
        }
        if (mabl.getReporter().getWarningCount() > 0) {
            mabl.getReporter().printWarnings(new PrintWriter(System.out, true));
        }

        mabl.dump(workingDirectory);
        Map<INode, PType> types = tcRes.getValue();

        new MableInterpreter(new DefaultExternalValueFactory(workingDirectory, name -> TypeChecker.findModule(types, name),
                IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8))).execute(MablParserUtil.parse(CharStreams.fromString(PrettyPrinter.print(program))));
    }
    static File getWorkingDirectory(File base, Class cls) throws IOException {
        String s = Paths.get("target", cls.getSimpleName()).toString() + File.separatorChar + (base==null?"":base.getAbsolutePath().substring(
                base.getAbsolutePath().replace(File.separatorChar, '/').indexOf("src/test/resources/") + ("src" + "/test" + "/resources/").length()));

        File workingDir = new File(s.replace('/', File.separatorChar));
        if (workingDir.exists()) {
            FileUtils.deleteDirectory(workingDir);
        }
        if (!workingDir.exists()) {
            workingDir.mkdirs();
        }
        return workingDir;
    }
}