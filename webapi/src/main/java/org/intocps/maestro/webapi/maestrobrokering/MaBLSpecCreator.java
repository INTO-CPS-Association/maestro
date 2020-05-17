package org.intocps.maestro.webapi.maestrobrokering;

import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.maestro.plugin.env.fmi2.ComponentInfo;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MaBLSpecCreator {

    public static String RemoveFmuKeyBraces(String fmuKey) {
        return fmuKey.substring(1, fmuKey.length() - 1);
    }

    public static String CreateMaBLSpec(UnitRelationship simulationEnvironment) throws XPathExpressionException {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("simulation\n" + "import FixedStep;\n" + "import TypeConverter;\n" + "import InitializerUsingCOE;\n" + "{\n" +
                "real START_TIME = 10.0;\n" + "real END_TIME = 10.0;\n" + "real STEP_SIZE = 0.1;\n");

        // Load the fmus
        Set<Map.Entry<String, ModelDescription>> fmusToModelDescriptions = simulationEnvironment.getFmusWithModelDescriptions();
        for (Map.Entry<String, ModelDescription> entry : fmusToModelDescriptions) {
            stringBuilder.append(String
                    .format("FMI2 %s = load(\"FMI2\", \"%s\", \"%s\")" + ";\n", RemoveFmuKeyBraces(entry.getKey()), entry.getValue().getGuid(),
                            simulationEnvironment.getFmuToUri().stream().filter(l -> l.getKey() == entry.getKey()).findFirst().get().getValue()));
        }
        Set<Map.Entry<String, ComponentInfo>> instances = simulationEnvironment.getInstances();
        //Instantiate the instances
        for (Map.Entry<String, ComponentInfo> entry : instances) {
            stringBuilder.append(String.format("FMI2Component %s = %s.instantiate(\"%s\",false,false);\n", entry.getKey(),
                    RemoveFmuKeyBraces(entry.getValue().fmuIdentifier), entry.getKey()));
        }
        // Create the components
        stringBuilder.append(String.format("IFmuComponent components[%d]={%s};\n", instances.size(),
                instances.stream().map(l -> l.getKey()).collect(Collectors.joining(","))));

        stringBuilder.append("external initialize(components,START_TIME, END_TIME);\n");
        stringBuilder.append("external fixedStep(components,STEP_SIZE,0.0,END_TIME);\n");

        for (Map.Entry<String, ComponentInfo> entry : instances) {
            stringBuilder.append(String.format("%s.terminate();\n", entry.getKey()));
            stringBuilder.append(String.format("%s.freeInstance(%s);\n", RemoveFmuKeyBraces(entry.getValue().fmuIdentifier), entry.getKey()));
        }

        for (Map.Entry<String, ModelDescription> entry : fmusToModelDescriptions) {
            stringBuilder.append(String.format("unload(%s);\n", RemoveFmuKeyBraces(entry.getKey())));
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}