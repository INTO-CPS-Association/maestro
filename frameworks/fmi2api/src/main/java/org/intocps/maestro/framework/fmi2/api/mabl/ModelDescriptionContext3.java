package org.intocps.maestro.framework.fmi2.api.mabl;

import kotlin.UInt;
import org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ModelDescriptionContext3 {

    private final Fmi3ModelDescription modelDescription;
    public Map<String, Fmi3ModelDescription.Fmi3ScalarVariable> nameToSv = new HashMap<>();
    public Map<Long, Fmi3ModelDescription.Fmi3ScalarVariable> valRefToSv = new HashMap<>();
    public ModelDescriptionContext3(
            Fmi3ModelDescription modelDescription) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        this.modelDescription = modelDescription;
        modelDescription.getScalarVariables().forEach((sv) -> {
            this.nameToSv.put(sv.getVariable().getName(), sv);
            this.valRefToSv.put((long) sv.getVariable().getValueReferenceAsLong(), sv);
        });
    }
    public Fmi3ModelDescription getModelDescription() {
        return modelDescription;
    }
}
