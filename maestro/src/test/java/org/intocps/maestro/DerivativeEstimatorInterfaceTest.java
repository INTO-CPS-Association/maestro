package org.intocps.maestro;

import org.apache.commons.io.IOUtils;
import org.intocps.fmi.Fmi2Status;
import org.intocps.fmi.IFmiComponent;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.PInitializer;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.fmi2.FmuFactory;
import org.intocps.maestro.framework.fmi2.IFmuFactory;
import org.intocps.maestro.framework.fmi2.api.DerivativeEstimator;
import org.intocps.maestro.framework.fmi2.api.mabl.BaseApiTest;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DerivativeEstimatorInterfaceTest extends BaseApiTest {
    private final Path dirPath = Paths.get("src", "test", "resources", "derivative_estimator_interface");

    /**
     * Tests the mabl interface for the derivative estimator by calling estimate. The values used and expected values are from
     * 'testCalculateDerivatives' in 'DerivativeEstimatorTests' which is originates from Maestro 1 tests of the derivative estimator
     */
    @Test
    @Order(0)
    public void testEstimateDerivatives() throws Exception {
        //Arrange
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
                //		Fmi2Status setDebugLogging(boolean var1, String[] var2) throws FmuInvocationException;
                when(comp.setDebugLogging(anyBoolean(), any())).thenReturn(Fmi2Status.OK);
                //		Fmi2Status setReals(long[] var1, double[] var2) throws InvalidParameterException, FmiInvalidNativeStateException;
                when(comp.setReals(any(), any())).thenReturn(Fmi2Status.OK);
                //		Fmi2Status terminate() throws FmuInvocationException;
                when(comp.terminate()).thenReturn(Fmi2Status.OK);
                //		boolean isValid();
                when(comp.isValid()).thenReturn(true);

                // Mock get or set derivatives function depending on the fmu.
                String modelDescriptionPath = Paths.get(dirPath.toString(), "sink_modelDescription.xml").toString();

                doReturn(Fmi2Status.OK).when(comp).setRealInputDerivatives(new long[]{1, 1, 2}, new int[]{1, 2, 1}, new double[]{10.0, 2.0, 8.0});

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

        FmuVariableFmi2Api sinkFMU = dynamicScope
                .createFMU("sinkFMU", new Fmi2ModelDescription(Paths.get(dirPath.toString(), "sink_modelDescription.xml").toFile()),
                        Paths.get(dirPath.toString(), "sink_mocked.fmu").toUri());

        ComponentVariableFmi2Api sink = sinkFMU.instantiate("sink");

        double x1 = 4.0, x2 = 9.0, x3 = 25.0, y1 = 4.0, y2 = 9.0, y3 = 25.0;
        int providedDerOrderForX = 0, providedDerOrderForY = 0, indexOfX = 0, indexOfY = 2, derOrderOfX = 2, derOrderOfY = 1;
        Double[][] sharedDers = {{0.0, 0.0}, {0.0, 0.0}, {0.0, 0.0}};

        ArrayVariableFmi2Api<Double> sharedDataStep1 = dynamicScope.store("sharedDataStep1", new Double[]{x1, -1.0, y1});
        ArrayVariableFmi2Api<Double> sharedDataStep2 = dynamicScope.store("sharedDataStep2", new Double[]{x2, -1.0, y2});
        ArrayVariableFmi2Api<Double> sharedDataStep3 = dynamicScope.store("sharedDataStep3", new Double[]{x3, -1.0, y3});
        ArrayVariableFmi2Api<Integer> derivativeOrders = dynamicScope.store("derivativeOrders", new Integer[]{derOrderOfX, derOrderOfY});
        ArrayVariableFmi2Api<Integer> indicesOfInterest = dynamicScope.store("indicesOfInterest", new Integer[]{indexOfX, indexOfY});
        ArrayVariableFmi2Api<Integer> providedDerivativeOrders =
                dynamicScope.store("providedDerivativeOrders", new Integer[]{providedDerOrderForX, providedDerOrderForY});
        DoubleVariableFmi2Api expectedXDotDot = dynamicScope.store("expectedXDotDot", 2.0);
        DoubleVariableFmi2Api expectedXDot = dynamicScope.store("expectedXDot", 10.0);
        DoubleVariableFmi2Api expectedYDot = dynamicScope.store("expectedYDot", 8.0);
        DoubleVariableFmi2Api stepTime1 = dynamicScope.store("stepTime1", 1.0);
        DoubleVariableFmi2Api stepTime2 = dynamicScope.store("stepTime2", 1.0);
        DoubleVariableFmi2Api stepTime3 = dynamicScope.store("stepTime3", 2.0);
        ArrayVariableFmi2Api<Double[]> sharedDataDerivatives = dynamicScope.store("sharedDataDerivatives", sharedDers);
        ArrayVariableFmi2Api<Object> derOrderInBuf = dynamicScope.store("derOrderInBuf", new Integer[]{1, 2, 1});
        ArrayVariableFmi2Api<Object> derRefInBuf = dynamicScope.store("derRefInBuf",
                new Integer[]{sink.getPort("fake_in1").getPortReferenceValue().intValue(),
                        sink.getPort("fake_in1").getPortReferenceValue().intValue(), sink.getPort("fake_in2").getPortReferenceValue().intValue()});

        //Act
        DerivativeEstimator derivativeEstimator = builder.getDerivativeEstimator();
        DerivativeEstimator.DerivativeEstimatorInstance derivativeEstimatorInstance = derivativeEstimator.createDerivativeEstimatorInstance();
        derivativeEstimatorInstance.initialize(indicesOfInterest, derivativeOrders, providedDerivativeOrders);

        derivativeEstimatorInstance.estimate(stepTime1, sharedDataStep1, sharedDataDerivatives);
        derivativeEstimatorInstance.estimate(stepTime2, sharedDataStep2, sharedDataDerivatives);
        derivativeEstimatorInstance.estimate(stepTime3, sharedDataStep3, sharedDataDerivatives);

        List<VariableFmi2Api<Double>> derVals =
                (List<VariableFmi2Api<Double>>) List.of(sharedDataDerivatives.items().get(0), sharedDataDerivatives.items().get(2)).stream()
                        .flatMap(v -> ((ArrayVariableFmi2Api) v).items().stream()).limit(3).collect(Collectors.toList());
        PInitializer initializer = newAArrayInitializer(derVals.stream().map(VariableFmi2Api::getReferenceExp).collect(Collectors.toList()));
        PStm variableStm = newALocalVariableStm(
                newAVariableDeclaration(newAIdentifier("derValInBuf"), newARealNumericPrimitiveType(), derVals.size(), initializer));
        dynamicScope.add(variableStm);

        ArrayVariableFmi2Api<Double> derValInBuf = new ArrayVariableFmi2Api<>(variableStm, newAUIntNumericPrimitiveType(), dynamicScope, dynamicScope,
                newAIdentifierStateDesignator(newAIdentifier("derValInBuf")), newAIdentifierExp("derValInBuf"), derVals);

        sink.setDerivatives(derValInBuf, derOrderInBuf, derRefInBuf, dynamicScope);

        BaseApiTest.MDebugAssert mDebugAssert = BaseApiTest.MDebugAssert.create(builder);
        mDebugAssert.assertEquals(expectedXDotDot, (VariableFmi2Api) ((ArrayVariableFmi2Api) sharedDataDerivatives.items().get(0)).items().get(1));
        mDebugAssert.assertEquals(expectedXDot, (VariableFmi2Api) ((ArrayVariableFmi2Api) sharedDataDerivatives.items().get(0)).items().get(0));
        mDebugAssert.assertEquals(expectedYDot, (VariableFmi2Api) ((ArrayVariableFmi2Api) sharedDataDerivatives.items().get(2)).items().get(0));

        sinkFMU.unload();

        String spec = PrettyPrinter.print(builder.build());

        //Assert
        check(spec, "");

        // Teardown the mock fmu factory after the test
        FmuFactory.customFactory = null;
    }

    /**
     * Tests the mabl interface for the derivative estimator by calling rollback. The values used and expected values are from
     * 'testRollBackFromMableInterface' in 'DerivativeEstimatorTests' which is originates from Maestro 1 tests of the derivative estimator
     */
    @Test
    @Order(1)
    public void testRollbackDerivatives() throws Exception {
        //Arrange
        double x = 1.0, providedXDot = 2.0, y = 10.0;

        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = false;
        MablApiBuilder builder = new MablApiBuilder(settings);
        DynamicActiveBuilderScope dynamicScope = builder.getDynamicScope();

        ArrayVariableFmi2Api<Double> sharedDataStep1 = dynamicScope.store("sharedDataStep1", new Double[]{x, -1.0, y});

        Double[][] sharedDers = {{providedXDot, 0.0}, {0.0, 0.0}, {0.0, 0.0}};
        ArrayVariableFmi2Api<Double[]> sharedDataDerivatives = dynamicScope.store("sharedDataDerivatives", sharedDers);
        DoubleVariableFmi2Api providedXDotVar = dynamicScope.store("providedXDotVar", providedXDot);

        ArrayVariableFmi2Api<Integer> derivativeOrders = dynamicScope.store("derivativeOrders", new Integer[]{2, 1});
        ArrayVariableFmi2Api<Integer> indicesOfInterest = dynamicScope.store("indicesOfInterest", new Integer[]{0, 2});
        ArrayVariableFmi2Api<Integer> providedDerivativeOrders = dynamicScope.store("providedDerivativeOrders", new Integer[]{1, 0});


        DerivativeEstimator derivativeEstimator = builder.getDerivativeEstimator();
        DerivativeEstimator.DerivativeEstimatorInstance derivativeEstimatorInstance = derivativeEstimator.createDerivativeEstimatorInstance();
        derivativeEstimatorInstance.initialize(indicesOfInterest, derivativeOrders, providedDerivativeOrders);


        DoubleVariableFmi2Api stepTime1 = dynamicScope.store("stepTime1", 1.0);
        derivativeEstimatorInstance.estimate(stepTime1, sharedDataStep1, sharedDataDerivatives);
        derivativeEstimatorInstance.rollback(sharedDataDerivatives);

        BaseApiTest.MDebugAssert mDebugAssert = BaseApiTest.MDebugAssert.create(builder);

        mDebugAssert.assertEquals(providedXDotVar, (VariableFmi2Api) ((ArrayVariableFmi2Api) sharedDataDerivatives.items().get(0)).items().get(0));

        String spec = PrettyPrinter.print(builder.build());

        //Assert
        check(spec, "");
    }
}
