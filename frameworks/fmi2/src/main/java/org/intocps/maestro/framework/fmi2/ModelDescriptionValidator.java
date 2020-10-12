package org.intocps.maestro.framework.fmi2;

import org.intocps.maestro.framework.core.EnvironmentException;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

//This class is a validator that verifies that the modelDescription is valid with the regards to the FMI specification
public class ModelDescriptionValidator {
    private static Predicate<ModelDescription.ScalarVariable> NotAllowedConfigurations() {
        return ExactInitialA().or(CalculatedInitialB()).or(CalculatedInitialC()).or(InitialD()).or(InitialE()).negate();
    }

    private static Predicate<ModelDescription.ScalarVariable> ExactInitialA() {
        return o -> ((o.causality == ModelDescription.Causality.Parameter &&
                (o.variability == ModelDescription.Variability.Fixed || o.variability == ModelDescription.Variability.Tunable)) ||
                o.causality == ModelDescription.Causality.Output && o.variability == ModelDescription.Variability.Constant ||
                o.causality == ModelDescription.Causality.Local && o.variability == ModelDescription.Variability.Constant);
    }

    private static Predicate<ModelDescription.ScalarVariable> CalculatedInitialB() {
        return o -> (o.causality == ModelDescription.Causality.CalculatedParameter &&
                (o.variability == ModelDescription.Variability.Fixed || o.variability == ModelDescription.Variability.Tunable) ||
                o.causality == ModelDescription.Causality.Local &&
                        (o.variability == ModelDescription.Variability.Fixed || o.variability == ModelDescription.Variability.Tunable));
    }

    private static Predicate<ModelDescription.ScalarVariable> CalculatedInitialC() {
        return o -> (o.causality == ModelDescription.Causality.Output &&
                (o.variability == ModelDescription.Variability.Discrete || o.variability == ModelDescription.Variability.Continuous) ||
                o.causality == ModelDescription.Causality.Local &&
                        (o.variability == ModelDescription.Variability.Discrete || o.variability == ModelDescription.Variability.Continuous));
    }

    private static Predicate<ModelDescription.ScalarVariable> InitialD() {
        return o -> (o.causality == ModelDescription.Causality.Input &&
                (o.variability == ModelDescription.Variability.Discrete || o.variability == ModelDescription.Variability.Continuous));
    }

    private static Predicate<ModelDescription.ScalarVariable> InitialE() {
        return o -> (o.causality == ModelDescription.Causality.Independent && o.variability == ModelDescription.Variability.Continuous);
    }

    private static Predicate<ModelDescription.ScalarVariable> InitialNotAllowedToBeSpecified() {
        return o -> (o.causality == ModelDescription.Causality.Input || o.causality == ModelDescription.Causality.Independent);
    }

    public ModelDescription Verify(
            ModelDescription md) throws IllegalAccessException, XPathExpressionException, InvocationTargetException, EnvironmentException {
        var variables = md.getScalarVariables();
        verifyVariabilityCausality(variables);
        addInitialToModelDescription(variables);
        return md;
    }

    public void verifyVariabilityCausality(
            List<ModelDescription.ScalarVariable> variables) throws IllegalAccessException, XPathExpressionException, InvocationTargetException, EnvironmentException {
        if (variables.stream().anyMatch(NotAllowedConfigurations())) {
            var scalarVariablesWithWrongSettings = variables.stream().filter(NotAllowedConfigurations()).collect(Collectors.toList());
            throw new EnvironmentException("The following components are being initialized with the wrong settings for causality and variability");
        }
        //Make sure initial is not set on causality = "independent" or causality = "input"
        if (variables.stream().anyMatch(InitialNotAllowedToBeSpecified().and(o -> o.initial != null))) {
            throw new EnvironmentException(
                    "Some components with causality = \"independent\" or causality = \"input\" has also a specified initial " + "value");
        }

    }

    public List<ModelDescription.ScalarVariable> addInitialToModelDescription(
            List<ModelDescription.ScalarVariable> variables) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        //Add default value for initial if initial has not been specified in the modelDescription file
        var decoratedExact = variables.parallelStream().filter(ExactInitialA().and(o -> o.initial == null)).collect(Collectors.toList());
        decoratedExact.forEach(o -> o.initial = ModelDescription.Initial.Exact);
        var decoratedCalculated = variables.parallelStream().filter(CalculatedInitialB().or(CalculatedInitialC()).and(o -> o.initial == null))
                .collect(Collectors.toList());
        decoratedCalculated.forEach(o -> o.initial = ModelDescription.Initial.Calculated);

        return variables;
    }
}
