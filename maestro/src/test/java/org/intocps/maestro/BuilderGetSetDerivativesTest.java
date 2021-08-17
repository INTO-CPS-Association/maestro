package org.intocps.maestro;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.fmi.Fmi2Status;
import org.intocps.fmi.FmuResult;
import org.intocps.fmi.IFmiComponent;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.fmi2.FmuFactory;
import org.intocps.maestro.framework.fmi2.IFmuFactory;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.PortFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.FmuVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.typechecker.TypeChecker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.ThrowingSupplier;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BuilderGetSetDerivativesTest {

    private final Path dirPath = Paths.get("src", "test", "resources", "builder_get_set_derivatives");
    private final Path pumpPath = Paths.get(dirPath.toString(), "mocked_fmus", "pump_mocked.fmu");
    private final Path sinkPath = Paths.get(dirPath.toString(), "mocked_fmus", "sink_mocked.fmu");

    @DisplayName("Get derivatives using the builder generates the expected MaBL spec.")
    @Test
    @Order(1)
    public void getDerivativesTest() throws Exception {
        // Arrange
        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        settings.setGetDerivatives = true;
        MablApiBuilder builder = new MablApiBuilder(settings);
        DynamicActiveBuilderScope dynamicScope = builder.getDynamicScope();

        FmuVariableFmi2Api pumpFMU = dynamicScope.createFMU("pumpFMU", "FMI2", pumpPath.toUri().toASCIIString());
        FmuVariableFmi2Api sinkFMU = dynamicScope.createFMU("sinkFMU", "FMI2", sinkPath.toUri().toASCIIString());

        ComponentVariableFmi2Api pump = pumpFMU.instantiate("pump");
        ComponentVariableFmi2Api sink = sinkFMU.instantiate("sink");

        pump.getPort("fake_out1").linkTo(sink.getPort("fake_in1"));
        pump.getPort("fake_out2").linkTo(sink.getPort("fake_in2"));

        Map<String, ComponentVariableFmi2Api> fmuInstances = new HashMap<>() {{
            put(pump.getName(), pump);
            put(sink.getName(), sink);
        }};

        List<String> variablesOfInterest = Arrays.asList("pumpFMU.pump.fake_out1", "pumpFMU.pump.fake_out2");

        int expected_derValOutSum = 2;
        int expected_derOrderOutSum = 6;
        int expected_derRefOutSum = 6;

        // Act
        // Get all ports
        fmuInstances.forEach((x, y) -> {
            Set<String> scalarVariablesToShare =
                    y.getPorts().stream().filter(p -> variablesOfInterest.stream().anyMatch(v -> v.equals(p.getMultiModelScalarVariableName())))
                            .map(PortFmi2Api::getName).collect(Collectors.toSet());

            y.get(scalarVariablesToShare.toArray(String[]::new));
        });
        ASimulationSpecificationCompilationUnit program = builder.build();

        // Assert
        List<String> specAsList =
                Arrays.stream(PrettyPrinter.print(program).split("[\n\t]+")).filter(s -> !s.matches("[' '{}]")).collect(Collectors.toList());
        int derValOutSum = specAsList.stream().mapToInt(s -> s.toLowerCase().contains("dval_out") ? 1 : 0).sum();
        int derOrderOutSum = specAsList.stream().mapToInt(s -> s.toLowerCase().contains("dorder_out") ? 1 : 0).sum();
        int derRefOutSum = specAsList.stream().mapToInt(s -> s.toLowerCase().contains("dref_out") ? 1 : 0).sum();
        boolean setDerFuncIsPresent =
                specAsList.stream().filter(s -> s.contains("pump.getRealOutputDerivatives")).collect(Collectors.toList()).size() == 1;

        Assertions.assertEquals(expected_derValOutSum, derValOutSum);
        Assertions.assertEquals(expected_derOrderOutSum, derOrderOutSum);
        Assertions.assertEquals(expected_derRefOutSum, derRefOutSum);
        Assertions.assertTrue(setDerFuncIsPresent);
    }

    @DisplayName("Share derivatives using the builder generates the expected MaBL spec.")
    @Test
    @Order(2)
    public void shareDerivativesTest() throws Exception {
        // Arrange
        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        settings.setGetDerivatives = true;
        MablApiBuilder builder = new MablApiBuilder(settings);
        DynamicActiveBuilderScope dynamicScope = builder.getDynamicScope();

        FmuVariableFmi2Api pumpFMU = dynamicScope.createFMU("pumpFMU", "FMI2", pumpPath.toUri().toASCIIString());
        FmuVariableFmi2Api sinkFMU = dynamicScope.createFMU("sinkFMU", "FMI2", sinkPath.toUri().toASCIIString());

        ComponentVariableFmi2Api pump = pumpFMU.instantiate("pump");
        ComponentVariableFmi2Api sink = sinkFMU.instantiate("sink");

        pump.getPort("fake_out1").linkTo(sink.getPort("fake_in1"));
        pump.getPort("fake_out2").linkTo(sink.getPort("fake_in2"));

        Map<String, ComponentVariableFmi2Api> fmuInstances = new HashMap<>() {{
            put(pump.getName(), pump);
            put(sink.getName(), sink);
        }};

        List<String> variablesOfInterest = Arrays.asList("pumpFMU.pump.fake_out1", "pumpFMU.pump.fake_out2", "pumpFMU.pump.fake_out3");

        int expected_derShareSum = 5;

        // Act
        // Get all ports and share them
        fmuInstances.forEach((x, y) -> {
            Set<String> scalarVariablesToShare =
                    y.getPorts().stream().filter(p -> variablesOfInterest.stream().anyMatch(v -> v.equals(p.getMultiModelScalarVariableName())))
                            .map(PortFmi2Api::getName).collect(Collectors.toSet());

            Map<PortFmi2Api, VariableFmi2Api<Object>> portsToShare = y.get(scalarVariablesToShare.toArray(String[]::new));

            y.share(portsToShare);
        });

        ASimulationSpecificationCompilationUnit program = builder.build();

        // Assert
        List<String> specAsList =
                Arrays.stream(PrettyPrinter.print(program).split("[\n\t]+")).filter(s -> !s.matches("[' '{}]")).collect(Collectors.toList());
        int derShareSum = specAsList.stream().mapToInt(s -> s.toLowerCase().contains("dershare") ? 1 : 0).sum();

        Assertions.assertEquals(expected_derShareSum, derShareSum);
    }


    @DisplayName("Set derivatives using the builder generates the expected MaBL spec.")
    @Test
    @Order(3)
    public void setDerivativesTest() throws Exception {
        // Arrange
        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        settings.setGetDerivatives = true;
        MablApiBuilder builder = new MablApiBuilder(settings);
        DynamicActiveBuilderScope dynamicScope = builder.getDynamicScope();

        FmuVariableFmi2Api pumpFMU = dynamicScope.createFMU("pumpFMU", "FMI2", pumpPath.toUri().toASCIIString());
        FmuVariableFmi2Api sinkFMU = dynamicScope.createFMU("sinkFMU", "FMI2", sinkPath.toUri().toASCIIString());

        ComponentVariableFmi2Api pump = pumpFMU.instantiate("pump");
        ComponentVariableFmi2Api sink = sinkFMU.instantiate("sink");

        pump.getPort("fake_out1").linkTo(sink.getPort("fake_in1"));
        pump.getPort("fake_out2").linkTo(sink.getPort("fake_in2"));

        Map<String, ComponentVariableFmi2Api> fmuInstances = new HashMap<>() {{
            put(pump.getName(), pump);
            put(sink.getName(), sink);
        }};

        List<String> variablesOfInterest = Arrays.asList("pumpFMU.pump.fake_out1", "pumpFMU.pump.fake_out2", "pumpFMU.pump.fake_out3");

        int expected_derValOutSum = 6;
        int expected_derOrderOutSum = 6;
        int expected_derRefOutSum = 6;

        // Act
        // Get all ports and share them
        fmuInstances.forEach((x, y) -> {
            Set<String> scalarVariablesToShare =
                    y.getPorts().stream().filter(p -> variablesOfInterest.stream().anyMatch(v -> v.equals(p.getMultiModelScalarVariableName())))
                            .map(PortFmi2Api::getName).collect(Collectors.toSet());

            Map<PortFmi2Api, VariableFmi2Api<Object>> portsToShare = y.get(scalarVariablesToShare.toArray(String[]::new));

            y.share(portsToShare);
        });

        // Set all linked ports
        fmuInstances.forEach((x, y) -> {
            if (y.getPorts().stream().anyMatch(p -> p.getSourcePort() != null)) {
                y.setLinked();
            }
        });
        ASimulationSpecificationCompilationUnit program = builder.build();

        // Assert
        List<String> specAsList =
                Arrays.stream(PrettyPrinter.print(program).split("[\n\t]+")).filter(s -> !s.matches("[' '{}]")).collect(Collectors.toList());
        int derValOutSum = specAsList.stream().mapToInt(s -> s.toLowerCase().contains("sinkrealdval_in") ? 1 : 0).sum();
        int derOrderOutSum = specAsList.stream().mapToInt(s -> s.toLowerCase().contains("sinkintdorder_in") ? 1 : 0).sum();
        int derRefOutSum = specAsList.stream().mapToInt(s -> s.toLowerCase().contains("sinkuintdref_in") ? 1 : 0).sum();
        boolean setDerFuncIsPresent =
                specAsList.stream().filter(s -> s.toLowerCase().contains("sink.setrealinputderivatives")).collect(Collectors.toList()).size() == 1;

        Assertions.assertEquals(expected_derValOutSum, derValOutSum);
        Assertions.assertEquals(expected_derOrderOutSum, derOrderOutSum);
        Assertions.assertEquals(expected_derRefOutSum, derRefOutSum);
        Assertions.assertTrue(setDerFuncIsPresent);
    }

    @Nested
    @DisplayName("Test of automatically get/set derivatives using the MaBL interpreter")
    class getSetDerivativesInMabl {
        private final Path pumpMDPath = Paths.get(dirPath.toString(), "mocked_fmus", "pump_modelDescription.xml");
        private final Path sinkMDPath = Paths.get(dirPath.toString(), "mocked_fmus", "sink_modelDescription.xml");

        private File getWorkingDirectory(File base) throws IOException {
            String s = "target/" + base.getAbsolutePath().substring(
                    base.getAbsolutePath().replace(File.separatorChar, '/').indexOf("src/test/resources/") +
                            ("src" + "/test" + "/resources/").length());

            File workingDir = new File(s.replace('/', File.separatorChar));
            if (workingDir.exists()) {
                FileUtils.deleteDirectory(workingDir);
            }
            if (!workingDir.exists()) {
                workingDir.mkdirs();
            }
            return workingDir;
        }

        @BeforeEach
        void beforeEach() {
            // Setup the mock before the test
            FmuFactory.customFactory = new IFmuFactory() {
                @Override
                public boolean accept(URI uri) {
                    return true;
                }

                @Override
                public IFmu instantiate(File sessionRoot, URI uri) throws Exception {
                    IFmu fmu = mock(IFmu.class);
                    when(fmu.isValid()).thenReturn(true);

                    IFmiComponent comp = mock(IFmiComponent.class);
                    when(fmu.instantiate(anyString(), anyString(), anyBoolean(), anyBoolean(), any())).thenReturn(comp);

                    when(comp.getFmu()).thenReturn(fmu);
                    //		Fmi2Status setDebugLogging(boolean var1, String[] var2) throws FmuInvocationException;
                    when(comp.setDebugLogging(anyBoolean(), any())).thenReturn(Fmi2Status.OK);
                    //		Fmi2Status setReals(long[] var1, double[] var2) throws InvalidParameterException, FmiInvalidNativeStateException;
                    when(comp.setReals(any(), any())).thenReturn(Fmi2Status.OK);
                    //		Fmi2Status terminate() throws FmuInvocationException;
                    when(comp.terminate()).thenReturn(Fmi2Status.OK);
                    //		boolean isValid();
                    when(comp.isValid()).thenReturn(true);

                    // Mock get or set derivatives function depending on the fmu.
                    String modelDescriptionPath;
                    if (uri.toASCIIString().contains("pump_mocked")) {
                        modelDescriptionPath = "src/test/resources/builder_get_set_derivatives/mocked_fmus/pump_modelDescription.xml";

                        when(comp.getReal(any())).thenReturn(new FmuResult<>(Fmi2Status.OK, new double[]{0.0}));

                        doReturn(new FmuResult<>(Fmi2Status.OK, new double[]{11, 12, 21, 22})).when(comp)
                                .getRealOutputDerivatives(new long[]{111111111, 111111111, 222222222, 222222222}, new int[]{1, 2, 1, 2});

                    } else {
                        modelDescriptionPath = "src/test/resources/builder_get_set_derivatives/mocked_fmus/sink_modelDescription.xml";

                        doReturn(Fmi2Status.OK).when(comp)
                                .setRealInputDerivatives(new long[]{1, 1, 2, 2}, new int[]{1, 2, 1, 2}, new double[]{11, 12, 21, 22});
                    }

                    final InputStream md =
                            new ByteArrayInputStream(IOUtils.toByteArray(new File(modelDescriptionPath.replace('/', File.separatorChar)).toURI()));
                    when(fmu.getModelDescription()).thenReturn(md);
                    return fmu;
                }

            };
        }

        @AfterEach
        void afterEach() {
            FmuFactory.customFactory = null;
        }

        @Test
        public void test() throws Exception {
            // Arrange
            Path directory = Paths.get("src", "test", "resources", "builder_get_set_derivatives");

            File workingDirectory = getWorkingDirectory(directory.toFile());

            MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
            settings.fmiErrorHandlingEnabled = false;
            settings.setGetDerivatives = true;
            MablApiBuilder builder = new MablApiBuilder(settings);
            IMablScope scope = builder.getDynamicScope();
            FmuVariableFmi2Api pumpFMU = scope.createFMU("pumpFMU", new Fmi2ModelDescription(pumpMDPath.toFile()), pumpPath.toUri());
            FmuVariableFmi2Api sinkFMU = scope.createFMU("sinkFMU", new Fmi2ModelDescription(sinkMDPath.toFile()), sinkPath.toUri());

            ComponentVariableFmi2Api pump = pumpFMU.instantiate("pump");
            ComponentVariableFmi2Api sink = sinkFMU.instantiate("sink");

            pump.getPort("fake_out1").linkTo(sink.getPort("fake_in1"));
            pump.getPort("fake_out2").linkTo(sink.getPort("fake_in2"));

            Map<String, ComponentVariableFmi2Api> fmuInstances = new HashMap<>() {{
                put(pump.getName(), pump);
                put(sink.getName(), sink);
            }};

            List<String> variablesOfInterest = Arrays.asList("pumpFMU.pump.fake_out1", "pumpFMU.pump.fake_out2");

            // Act
            // Get all ports and share them
            fmuInstances.forEach((x, y) -> {
                Set<String> scalarVariablesToShare =
                        y.getPorts().stream().filter(p -> variablesOfInterest.stream().anyMatch(v -> v.equals(p.getMultiModelScalarVariableName())))
                                .map(PortFmi2Api::getName).collect(Collectors.toSet());

                Map<PortFmi2Api, VariableFmi2Api<Object>> portsToShare = y.get(scalarVariablesToShare.toArray(String[]::new));

                y.share(portsToShare);
            });

            // Set all linked ports
            fmuInstances.forEach((x, y) -> {
                if (y.getPorts().stream().anyMatch(p -> p.getSourcePort() != null)) {
                    y.setLinked();
                }
            });

            pumpFMU.unload();
            sinkFMU.unload();

            // Setup mabl
            ASimulationSpecificationCompilationUnit program = builder.build();

            File specFolder = new File(workingDirectory, "specs");
            specFolder.mkdirs();

            FileUtils.writeStringToFile(Paths.get(specFolder.toString(), "spec.mabl").toFile(), program.toString(), StandardCharsets.UTF_8);

            FileUtils.copyInputStreamToFile(
                    Objects.requireNonNull(TypeChecker.class.getResourceAsStream("/org/intocps/maestro/typechecker/FMI2.mabl")),
                    new File(specFolder, "FMI2.mabl"));

            IErrorReporter reporter = new ErrorReporter();
            Mabl mabl = new Mabl(directory.toFile(), workingDirectory);
            mabl.setReporter(reporter);
            mabl.setVerbose(true);

            // Assert
            Assertions.assertDoesNotThrow(() -> mabl
                    .parse(Arrays.stream(Objects.requireNonNull(specFolder.listFiles((file, s) -> s.toLowerCase().endsWith(".mabl"))))
                            .collect(Collectors.toList())));

            Assertions.assertDoesNotThrow((ThrowingSupplier<Map.Entry<Boolean, Map<INode, PType>>>) mabl::typeCheck);
            Assertions.assertDoesNotThrow(() -> mabl.verify(Framework.FMI2));

            if (reporter.getErrorCount() > 0) {
                reporter.printErrors(new PrintWriter(System.err, true));
                Assertions.fail();
            }

            Assertions.assertDoesNotThrow(() -> new MableInterpreter(new DefaultExternalValueFactory(workingDirectory,
                    IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8))).execute(mabl.getMainSimulationUnit()));
        }
    }
}
