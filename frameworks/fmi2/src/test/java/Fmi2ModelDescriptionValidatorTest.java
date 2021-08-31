import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.core.EnvironmentException;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.ModelDescriptionValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class Fmi2ModelDescriptionValidatorTest {
    private ModelDescriptionValidator modelDescriptionValidator;
    private Fmi2ModelDescription md;
    private List<Fmi2ModelDescription.ScalarVariable> variables;

    @BeforeEach
    public void beforeEachTestMethod() throws Exception {
        InputStream multimodelJson = this.getClass().getResourceAsStream("watertankmultimodel.json");
        Fmi2SimulationEnvironment env = Fmi2SimulationEnvironment.of(multimodelJson, new IErrorReporter.SilentReporter());
        modelDescriptionValidator = new ModelDescriptionValidator();
        var iterator = env.getFmusWithModelDescriptions().iterator();
        iterator.next();
        md = iterator.next().getValue();
        variables = md.getScalarVariables();
    }

    @Test
    public void verifyVariabilityCausalityIllegalConfiguration1Throws() throws Exception {
        //setup illegal configuration
        variables.forEach(o -> {
            o.causality = Fmi2ModelDescription.Causality.Parameter;
            o.variability = Fmi2ModelDescription.Variability.Constant;
        });
        Assertions.assertThrows(EnvironmentException.class, () -> modelDescriptionValidator.verifyVariabilityCausality(variables));
    }

    @Test
    public void verifyVariabilityCausalityIllegalConfiguration2Throws() throws Exception {
        //setup illegal configuration
        variables.forEach(o -> {
            o.causality = Fmi2ModelDescription.Causality.Independent;
            o.variability = Fmi2ModelDescription.Variability.Constant;
        });
        Assertions.assertThrows(EnvironmentException.class, () -> modelDescriptionValidator.verifyVariabilityCausality(variables));
    }

    @Test
    public void verifyVariabilityCausalityIllegalConfiguration3Throws() throws Exception {
        //setup illegal configuration
        variables.forEach(o -> {
            o.causality = Fmi2ModelDescription.Causality.Output;
            o.variability = Fmi2ModelDescription.Variability.Fixed;
        });
        Assertions.assertThrows(EnvironmentException.class, () -> modelDescriptionValidator.verifyVariabilityCausality(variables));
    }

    @Test
    public void verifyVariabilityCausalityIllegalConfigurationInitial1Throws() throws Exception {
        //setup illegal configuration
        variables.forEach(o -> {
            o.initial = Fmi2ModelDescription.Initial.Exact;
            o.causality = Fmi2ModelDescription.Causality.Input;
        });
        Assertions.assertThrows(EnvironmentException.class, () -> modelDescriptionValidator.verifyVariabilityCausality(variables));
    }

    @Test
    public void verifyVariabilityCausalityIllegalConfigurationInitial2Throws() throws Exception {
        //setup illegal configuration
        variables.forEach(o -> {
            o.initial = Fmi2ModelDescription.Initial.Exact;
            o.causality = Fmi2ModelDescription.Causality.Independent;
        });
        Assertions.assertThrows(EnvironmentException.class, () -> modelDescriptionValidator.verifyVariabilityCausality(variables));
    }

    @Test
    public void verifyVariabilityCausalityLegalConfigurationPasses() throws Exception {
        modelDescriptionValidator.verifyVariabilityCausality(variables);
    }


    @Test
    public void addInitialModelDescription_AddInitialToAllFields() throws Exception {
        var decoratedvariables = modelDescriptionValidator.addInitialToModelDescription(variables);
        var variablesThatShouldHaveAnInitial = decoratedvariables.stream()
                .filter(o -> o.causality != Fmi2ModelDescription.Causality.Independent && o.causality != Fmi2ModelDescription.Causality.Input)
                .collect(Collectors.toList());
        var variablesThatShouldNotHaveAnInitial = decoratedvariables.stream()
                .filter(o -> o.causality == Fmi2ModelDescription.Causality.Independent || o.causality == Fmi2ModelDescription.Causality.Input)
                .collect(Collectors.toList());
        Assertions.assertTrue(variablesThatShouldHaveAnInitial.stream().allMatch(o -> o.initial != null));
        Assertions.assertTrue(variablesThatShouldNotHaveAnInitial.stream().allMatch(o -> o.initial == null));
    }

    //Tank.volume.initial and Valve.outflow.initial
    @Test
    public void addInitialModelDescription_ValveOutFlow() throws Exception {
        var valuesBefore = variables.stream().filter(o -> o.getValueReference() == 5 || o.getValueReference() == 6).collect(Collectors.toList());
        valuesBefore.forEach(o -> o.initial = null);
        Assertions.assertTrue(valuesBefore.stream().allMatch(o -> o.initial == null));

        var decoratedvariables = modelDescriptionValidator.addInitialToModelDescription(variables);
        var valuesAfter =
                decoratedvariables.stream().filter(o -> o.getValueReference() == 5 || o.getValueReference() == 6).collect(Collectors.toList());
        Assertions.assertTrue(valuesAfter.stream().allMatch(o -> o.initial != null));
    }

    //Make tests to test a couple of default values
}
