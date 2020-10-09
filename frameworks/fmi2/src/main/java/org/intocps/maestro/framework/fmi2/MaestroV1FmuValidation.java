package org.intocps.maestro.framework.fmi2;

import org.intocps.fmi.IFmu;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.orchestration.coe.FmuFactory;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.fmi.VdmSvChecker;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MaestroV1FmuValidation implements IFmuValidator {
    @Override
    public boolean validate(String id, URI path, IErrorReporter reporter) {
        try {
            IFmu fmu = FmuFactory.create(null, path);

            ModelDescription md = new ModelDescription(fmu.getModelDescription());
            validateModelDescription(md);
            VdmSvChecker.validateModelVariables(md.getScalarVariables());
            return true;
        } catch (Exception e) {
            reporter.report(0, e.getMessage(), null);
            return false;
        }
    }


    /**
     * Validate model description overall structure
     */
    void validateModelDescription(
            ModelDescription description) throws IllegalAccessException, XPathExpressionException, InvocationTargetException, VdmSvChecker.ScalarVariableConfigException {

        List<ModelDescription.ScalarVariable> outputs =
                description.getScalarVariables().stream().filter(sv -> sv.causality == ModelDescription.Causality.Output)
                        .collect(Collectors.toList());
        List<ModelDescription.ScalarVariable> declaredOutputs = description.getOutputs();

        List<ModelDescription.ScalarVariable> invalidDeclaredOutputs =
                declaredOutputs.stream().filter(sv -> sv.causality != ModelDescription.Causality.Output).collect(Collectors.toList());
        if (!invalidDeclaredOutputs.isEmpty()) {
            throw new VdmSvChecker.ScalarVariableConfigException(
                    "Declared outputs in model description model structure contains scalar variables that has Causality != Output: " +
                            invalidDeclaredOutputs);
        }

        if (!outputs.isEmpty()) {
            if (declaredOutputs == null || declaredOutputs.size() != outputs.size() || !declaredOutputs.containsAll(outputs)) {
                throw new VdmSvChecker.ScalarVariableConfigException(
                        "The model description does not declare the following outputs in the model " + "structure: " +
                                outputs.stream().filter(x -> !declaredOutputs.contains(x)).map(Objects::toString));
            }
        }
    }
}
