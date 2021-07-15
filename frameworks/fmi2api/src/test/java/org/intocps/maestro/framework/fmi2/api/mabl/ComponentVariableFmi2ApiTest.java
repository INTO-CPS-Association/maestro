package org.intocps.maestro.framework.fmi2.api.mabl;

import org.apache.commons.io.IOUtils;
import org.intocps.fmi.Fmi2Status;
import org.intocps.fmi.IFmiComponent;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.fmi.ModelDescription;
import org.intocps.maestro.framework.fmi2.FmuFactory;
import org.intocps.maestro.framework.fmi2.IFmuFactory;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.FmuVariableFmi2Api;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ComponentVariableFmi2ApiTest extends BaseApiTest {

    @Test
    public void setDebugLoggingTest() throws Exception {

        //Arrange
        final Path dirPath = Paths.get("src", "test", "resources", "component_variable_fmi2_api_test");
        final String[] logCategories = new String[]{"logEvents", "logStatusError", "logStatusWarning", "logStatusPending"};
        final boolean enableLogging = true;


        // Setup the mock fmu factory before the test
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
                //Expect that setDebugLogging is called with enableLogging and logCategories
                when(comp.setDebugLogging(enableLogging, logCategories)).thenReturn(Fmi2Status.OK);
                when(comp.setReals(any(), any())).thenReturn(Fmi2Status.OK);
                when(comp.terminate()).thenReturn(Fmi2Status.OK);
                when(comp.isValid()).thenReturn(true);

                String modelDescriptionPath = Paths.get(dirPath.toString(), "modelDescription.xml").toString();

                final InputStream md =
                        new ByteArrayInputStream(IOUtils.toByteArray(new File(modelDescriptionPath.replace('/', File.separatorChar)).toURI()));
                when(fmu.getModelDescription()).thenReturn(md);
                return fmu;
            }
        };

        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        MablApiBuilder builder = new MablApiBuilder(settings, null);
        DynamicActiveBuilderScope dynamicScope = builder.getDynamicScope();

        FmuVariableFmi2Api tankFMU = dynamicScope
                .createFMU("tankFMU", new ModelDescription(Paths.get(dirPath.toString(), "modelDescription.xml").toFile()),
                        Paths.get(dirPath.toString(), "singlewatertank-20sim.fmu").toUri());

        ComponentVariableFmi2Api tank = tankFMU.instantiate("tank");

        //Act
        tank.setDebugLogging(List.of(logCategories), enableLogging);

        tank.terminate();

        tankFMU.unload();

        String spec = PrettyPrinter.print(builder.build());

        //Assert
        Assertions.assertDoesNotThrow(() -> check(spec, ""), "Exception can be caused by log categories not matching with mocked fmu");

        // Teardown the mock fmu factory after the test
        FmuFactory.customFactory = null;
    }
}
