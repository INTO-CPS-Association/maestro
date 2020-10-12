import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.EnvironmentException;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.framework.fmi2.ModelDescriptionValidator;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class ModelDescriptionValidatorTest {
    private ModelDescriptionValidator modelDescriptionValidator;
    private ModelDescription md;
    private List<ModelDescription.ScalarVariable> variables;

    @Before
    public void beforeEachTestMethod() throws Exception {
        InputStream multimodelJson = this.getClass().getResourceAsStream("watertankmultimodel.json");
        FmiSimulationEnvironment env = FmiSimulationEnvironment.of(multimodelJson, new IErrorReporter.SilentReporter());
        modelDescriptionValidator = new ModelDescriptionValidator();
        var iterator = env.getFmusWithModelDescriptions().iterator();
        iterator.next();
        md = iterator.next().getValue();
        variables = md.getScalarVariables();
    }

    @Test
    public void VerifyVariabilityCausality_IllegalConfiguration1_Throws() throws Exception {
        //setup illegal configuration
        variables.forEach(o -> {
            o.causality = ModelDescription.Causality.Parameter;
            o.variability = ModelDescription.Variability.Constant;
        });
        Assert.assertThrows(EnvironmentException.class, () -> modelDescriptionValidator.verifyVariabilityCausality(variables));
    }

    @Test
    public void VerifyVariabilityCausality_IllegalConfiguration2_Throws() throws Exception {
        //setup illegal configuration
        variables.forEach(o -> {
            o.causality = ModelDescription.Causality.Independent;
            o.variability = ModelDescription.Variability.Constant;
        });
        Assert.assertThrows(EnvironmentException.class, () -> modelDescriptionValidator.verifyVariabilityCausality(variables));
    }

    @Test
    public void VerifyVariabilityCausality_IllegalConfiguration3_Throws() throws Exception {
        //setup illegal configuration
        variables.forEach(o -> {
            o.causality = ModelDescription.Causality.Output;
            o.variability = ModelDescription.Variability.Fixed;
        });
        Assert.assertThrows(EnvironmentException.class, () -> modelDescriptionValidator.verifyVariabilityCausality(variables));
    }

    @Test
    public void VerifyVariabilityCausality_IllegalConfigurationInitial1_Throws() throws Exception {
        //setup illegal configuration
        variables.forEach(o -> {
            o.initial = ModelDescription.Initial.Exact;
            o.causality = ModelDescription.Causality.Input;
        });
        Assert.assertThrows(EnvironmentException.class, () -> modelDescriptionValidator.verifyVariabilityCausality(variables));
    }

    @Test
    public void VerifyVariabilityCausality_IllegalConfigurationInitial2_Throws() throws Exception {
        //setup illegal configuration
        variables.forEach(o -> {
            o.initial = ModelDescription.Initial.Exact;
            o.causality = ModelDescription.Causality.Independent;
        });
        Assert.assertThrows(EnvironmentException.class, () -> modelDescriptionValidator.verifyVariabilityCausality(variables));
    }

    @Test
    public void VerifyVariabilityCausality_LegalConfiguration_Passes() throws Exception {
        modelDescriptionValidator.verifyVariabilityCausality(variables);
    }


    @Test
    public void addInitialModelDescription_AddInitialToAllFields() throws Exception {
        var decoratedvariables = modelDescriptionValidator.addInitialToModelDescription(variables);
        var variablesThatShouldHaveAnInitial = decoratedvariables.stream()
                .filter(o -> o.causality != ModelDescription.Causality.Independent && o.causality != ModelDescription.Causality.Input)
                .collect(Collectors.toList());
        var variablesThatShouldNotHaveAnInitial = decoratedvariables.stream()
                .filter(o -> o.causality == ModelDescription.Causality.Independent || o.causality == ModelDescription.Causality.Input)
                .collect(Collectors.toList());
        Assert.assertTrue(variablesThatShouldHaveAnInitial.stream().allMatch(o -> o.initial != null));
        Assert.assertTrue(variablesThatShouldNotHaveAnInitial.stream().allMatch(o -> o.initial == null));
    }

    //Tank.volume.initial and Valve.outflow.initial
    @Test
    public void addInitialModelDescription_ValveOutFlow() throws Exception {
        var valuesBefore = variables.stream().filter(o -> o.getValueReference() == 5 || o.getValueReference() == 6).collect(Collectors.toList());
        valuesBefore.forEach(o -> o.initial = null);
        Assert.assertTrue(valuesBefore.stream().allMatch(o -> o.initial == null));

        var decoratedvariables = modelDescriptionValidator.addInitialToModelDescription(variables);
        var valuesAfter =
                decoratedvariables.stream().filter(o -> o.getValueReference() == 5 || o.getValueReference() == 6).collect(Collectors.toList());
        Assert.assertTrue(valuesAfter.stream().allMatch(o -> o.initial != null));
    }

    //Make tests to test a couple of default values
}
