package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.fmi.Fmi2ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ModelDescriptionContext {

    private final Fmi2ModelDescription modelDescription;
    public Map<String, Fmi2ModelDescription.ScalarVariable> nameToSv = new HashMap<>();
    public Map<Long, Fmi2ModelDescription.ScalarVariable> valRefToSv = new HashMap<>();
    public ModelDescriptionContext(
            Fmi2ModelDescription modelDescription) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        this.modelDescription = modelDescription;
        modelDescription.getScalarVariables().forEach((sv) -> {
            this.nameToSv.put(sv.name, sv);
            this.valRefToSv.put(sv.valueReference, sv);
        });
    }

    public Fmi2ModelDescription getModelDescription() {
        return modelDescription;
    }
}
