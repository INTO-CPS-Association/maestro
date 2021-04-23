package org.intocps.maestro;

import org.apache.commons.io.IOUtils;
import org.intocps.fmi.Fmi2Status;
import org.intocps.fmi.FmuResult;
import org.intocps.fmi.IFmiComponent;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.framework.fmi2.FmuFactory;
import org.intocps.maestro.framework.fmi2.IFmuFactory;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.PortFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.FmuVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DerivativeEstimatorInterfaceTest {
    private final Path dirPath = Paths.get("src", "test", "resources", "derivativeEstimatorInterface");
    private final Path pumpPath = Paths.get(dirPath.toString(), "mocked_fmus", "pump_mocked.fmu");
    private final Path sinkPath = Paths.get(dirPath.toString(), "mocked_fmus", "sink_mocked.fmu");

    @Test
    public void testCalculateDerivativesFromMableInterface() throws Exception {
        //Arrange
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
                //		Fmi2Status terminate() throws FmuInvocationException;
                when(comp.terminate()).thenReturn(Fmi2Status.OK);
                //		boolean isValid();
                when(comp.isValid()).thenReturn(true);

                // Mock get or set derivatives function depending on the fmu.
                String modelDescriptionPath;
                modelDescriptionPath = Paths.get(dirPath.toString(), "pump_modelDescription.xml").toString();

                when(comp.getReal(any())).thenReturn(new FmuResult<>(Fmi2Status.OK, new double[]{0.0}));

                doReturn(new FmuResult<>(Fmi2Status.OK, new double[]{11, 12, 21, 22})).when(comp)
                        .getRealOutputDerivatives(new long[]{111111111, 111111111, 222222222, 222222222}, new int[]{1, 2, 1, 2});

                final InputStream md =
                        new ByteArrayInputStream(IOUtils.toByteArray(new File(modelDescriptionPath.replace('/', File.separatorChar)).toURI()));
                when(fmu.getModelDescription()).thenReturn(md);
                return fmu;
            }

        };

        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        settings.setGetDerivatives = true;
        MablApiBuilder builder = new MablApiBuilder(settings, true);
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
                    y.getPorts().stream().filter(p -> variablesOfInterest.stream().anyMatch(v -> v.equals(p.getLogScalarVariableName())))
                            .map(PortFmi2Api::getName).collect(Collectors.toSet());

            Map<PortFmi2Api, VariableFmi2Api<Object>> portsToShare = y.get(scalarVariablesToShare.toArray(String[]::new));

            y.share(portsToShare);
        });

        pumpFMU.unload();
        sinkFMU.unload();

        //Act


        //Assert

    }
}
