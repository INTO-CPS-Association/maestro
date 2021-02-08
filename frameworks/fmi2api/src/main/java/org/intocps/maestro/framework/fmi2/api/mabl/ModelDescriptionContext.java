package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ModelDescriptionContext {

    private final ModelDescription modelDescription;
    public Map<String, ModelDescription.ScalarVariable> nameToSv = new HashMap<>();
    public Map<Long, ModelDescription.ScalarVariable> valRefToSv = new HashMap<>();
    public ModelDescriptionContext(
            ModelDescription modelDescription) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        this.modelDescription = modelDescription;
        modelDescription.getScalarVariables().forEach((sv) -> {
            this.nameToSv.put(sv.name, sv);
            this.valRefToSv.put(sv.valueReference, sv);
        });
    }

    public ModelDescription getModelDescription() {
        return modelDescription;
    }
}
