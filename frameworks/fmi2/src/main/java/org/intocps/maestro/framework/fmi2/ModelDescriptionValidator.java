package org.intocps.maestro.framework.fmi2;

import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.core.EnvironmentException;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

//This class is a validator that verifies that the modelDescription is valid with the regards to the FMI specification
public class ModelDescriptionValidator {
    private static Predicate<Fmi2ModelDescription.ScalarVariable> notAllowedConfigurations() {
        return exactInitialA().or(calculatedInitialB()).or(calculatedInitialC()).or(initialD()).or(initialE()).negate();
    }

    private static Predicate<Fmi2ModelDescription.ScalarVariable> exactInitialA() {
        return o -> ((o.causality == Fmi2ModelDescription.Causality.Parameter &&
                (o.variability == Fmi2ModelDescription.Variability.Fixed || o.variability == Fmi2ModelDescription.Variability.Tunable)) ||
                o.causality == Fmi2ModelDescription.Causality.Output && o.variability == Fmi2ModelDescription.Variability.Constant ||
                o.causality == Fmi2ModelDescription.Causality.Local && o.variability == Fmi2ModelDescription.Variability.Constant);
    }

    private static Predicate<Fmi2ModelDescription.ScalarVariable> calculatedInitialB() {
        return o -> (o.causality == Fmi2ModelDescription.Causality.CalculatedParameter &&
                (o.variability == Fmi2ModelDescription.Variability.Fixed || o.variability == Fmi2ModelDescription.Variability.Tunable) ||
                o.causality == Fmi2ModelDescription.Causality.Local &&
                        (o.variability == Fmi2ModelDescription.Variability.Fixed || o.variability == Fmi2ModelDescription.Variability.Tunable));
    }

    private static Predicate<Fmi2ModelDescription.ScalarVariable> calculatedInitialC() {
        return o -> (o.causality == Fmi2ModelDescription.Causality.Output &&
                (o.variability == Fmi2ModelDescription.Variability.Discrete || o.variability == Fmi2ModelDescription.Variability.Continuous) ||
                o.causality == Fmi2ModelDescription.Causality.Local &&
                        (o.variability == Fmi2ModelDescription.Variability.Discrete || o.variability == Fmi2ModelDescription.Variability.Continuous));
    }

    private static Predicate<Fmi2ModelDescription.ScalarVariable> initialD() {
        return o -> (o.causality == Fmi2ModelDescription.Causality.Input &&
                (o.variability == Fmi2ModelDescription.Variability.Discrete || o.variability == Fmi2ModelDescription.Variability.Continuous));
    }

    private static Predicate<Fmi2ModelDescription.ScalarVariable> initialE() {
        return o -> (o.causality == Fmi2ModelDescription.Causality.Independent && o.variability == Fmi2ModelDescription.Variability.Continuous);
    }

    private static Predicate<Fmi2ModelDescription.ScalarVariable> initialNotAllowedToBeSpecified() {
        return o -> (o.causality == Fmi2ModelDescription.Causality.Input || o.causality == Fmi2ModelDescription.Causality.Independent);
    }

    public Fmi2ModelDescription verify(
            Fmi2ModelDescription md) throws IllegalAccessException, XPathExpressionException, InvocationTargetException, EnvironmentException {
        var variables = md.getScalarVariables();
        verifyVariabilityCausality(variables);
        addInitialToModelDescription(variables);
        return md;
    }

    public void verifyVariabilityCausality(
            List<Fmi2ModelDescription.ScalarVariable> variables) throws IllegalAccessException, XPathExpressionException, InvocationTargetException, EnvironmentException {
        if (variables.stream().anyMatch(notAllowedConfigurations())) {
            var scalarVariablesWithWrongSettings = variables.stream().filter(notAllowedConfigurations()).collect(Collectors.toList());
            throw new EnvironmentException("The following components are being initialized with the wrong settings for causality and variability");
        }
        //Make sure initial is not set on causality = "independent" or causality = "input"
        if (variables.stream().anyMatch(initialNotAllowedToBeSpecified().and(o -> o.initial != null))) {
            throw new EnvironmentException(
                    "Some components with causality = \"independent\" or causality = \"input\" has also a specified initial " + "value");
        }

    }

    public List<Fmi2ModelDescription.ScalarVariable> addInitialToModelDescription(
            List<Fmi2ModelDescription.ScalarVariable> variables) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        //Add default value for initial if initial has not been specified in the modelDescription file
        var decoratedExact = variables.parallelStream().filter(exactInitialA().and(o -> o.initial == null)).collect(Collectors.toList());
        decoratedExact.forEach(o -> o.initial = Fmi2ModelDescription.Initial.Exact);
        var decoratedCalculated = variables.parallelStream().filter(calculatedInitialB().or(calculatedInitialC()).and(o -> o.initial == null))
                .collect(Collectors.toList());
        decoratedCalculated.forEach(o -> o.initial = Fmi2ModelDescription.Initial.Calculated);

        return variables;
    }
}
