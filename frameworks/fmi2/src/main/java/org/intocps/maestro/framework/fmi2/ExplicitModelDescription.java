package org.intocps.maestro.framework.fmi2;

import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.fmi.VdmSvChecker;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * Proxy interface to a model description that make all assumption about the scalar variables explicit
 */
public class ExplicitModelDescription extends ModelDescription {

    List<ScalarVariable> scalarVariables = null;
    List<ScalarVariable> outputs = null;
    List<ScalarVariable> derivatives = null;
    Map<ScalarVariable, ScalarVariable> derivativesMap;
    List<ScalarVariable> initialUnknowns = null;

    public ExplicitModelDescription(InputStream inputStream) throws IOException, SAXException, ParserConfigurationException {
        super(inputStream);
    }

    @Override
    public List<ScalarVariable> getScalarVariables() throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        if (scalarVariables == null) {
            try {
                scalarVariables = VdmSvChecker.validateModelVariables(super.getScalarVariables());
            } catch (VdmSvChecker.ScalarVariableConfigException e) {
                throw new RuntimeException(e);
            }
        }
        return scalarVariables;
    }


    @Override
    public List<ScalarVariable> getOutputs() throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        if (outputs == null) {
            try {
                outputs = VdmSvChecker.validateModelVariables(super.getOutputs());
            } catch (VdmSvChecker.ScalarVariableConfigException e) {
                throw new RuntimeException(e);
            }
        }
        return outputs;
    }

    @Override
    public List<ScalarVariable> getDerivatives() throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        if (derivatives == null) {
            derivatives = super.getDerivatives();
        }
        return derivatives;
    }

    @Override
    public Map<ScalarVariable, ScalarVariable> getDerivativesMap() throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        if (derivativesMap == null) {
            derivativesMap = super.getDerivativesMap();
        }
        return derivativesMap;
    }

    @Override
    public List<ScalarVariable> getInitialUnknowns() throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        if (initialUnknowns == null) {
            initialUnknowns = super.getInitialUnknowns();
        }
        return initialUnknowns;
    }
}
