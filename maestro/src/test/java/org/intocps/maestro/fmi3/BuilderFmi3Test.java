package org.intocps.maestro.fmi3;

import org.intocps.fmi.FmuInvocationException;
import org.intocps.fmi.jnifmuapi.fmi3.Fmu3;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.fmi.fmi3.Fmi3Causality;
import org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.PortFmi3Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ArrayVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.FmuVariableFmi3Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.InstanceVariableFmi3Api;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public class BuilderFmi3Test {
    @BeforeAll
    public static void before() throws IOException {
        Fmi3ModuleReferenceFmusTest.downloadReferenceFmus();
    }


    public static InstanceVariableFmi3Api createInstance(MablApiBuilder builder,String name, URI uri) throws Exception {

        Fmi3ModelDescription md = new Fmi3ModelDescription(new Fmu3(new File(uri)).getModelDescription());

        FmuVariableFmi3Api fmu = builder.getDynamicScope().createFMU(name+"Fmu", md, uri);

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

        InstanceVariableFmi3Api fd = createInstance(builder,"fd",new File("target/Fmi3ModuleReferenceFmusTest/cache/Feedthrough.fmu").getAbsoluteFile().toURI());
        InstanceVariableFmi3Api sg = createInstance(builder,"sg",new File("src/test/resources/fmi3/reference/siggen-feedthrough/SignalGenerator.fmu").getAbsoluteFile().toURI());


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

}
