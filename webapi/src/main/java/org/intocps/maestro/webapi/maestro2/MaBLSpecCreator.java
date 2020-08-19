package org.intocps.maestro.webapi.maestro2;

import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.maestro.plugin.env.fmi2.ComponentInfo;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MaBLSpecCreator {

    public static String removeFmuKeyBraces(String fmuKey) {
        return fmuKey.substring(1, fmuKey.length() - 1);
    }

    public static String createMaBLSpec(Maestro2SimulationController.SimulateRequestBody simulationConfig, UnitRelationship simulationEnvironment,
            double stepSize, boolean withWs, File rootDirectory) throws XPathExpressionException {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
                .append("simulation\n" + "import FixedStep;\n" + "import TypeConverter;\n" + "import Initializer;\n" + "{\n" + "real START_TIME = " +
                        simulationConfig.getStartTime() + ";\n" + "real END_TIME = " + simulationConfig.getEndTime() + ";\n" + "real STEP_SIZE = " +
                        stepSize + ";\n");

        // Load the fmus
        Set<Map.Entry<String, ModelDescription>> fmusToModelDescriptions = simulationEnvironment.getFmusWithModelDescriptions();
        for (Map.Entry<String, ModelDescription> entry : fmusToModelDescriptions) {
            stringBuilder.append(String
                    .format("FMI2 %s = load(\"FMI2\", \"%s\", \"%s\")" + ";\n", removeFmuKeyBraces(entry.getKey()), entry.getValue().getGuid(),
                            simulationEnvironment.getFmuToUri().stream().filter(l -> l.getKey() == entry.getKey()).findFirst().get().getValue()));
        }

        // Load the logger
        stringBuilder.append("Logger logger = load(\"Logger\");\n");

        // Load the data writer
        stringBuilder.append("DataWriter dataWriter = load(\"DataWriter\");\n");

        Set<Map.Entry<String, ComponentInfo>> instances = simulationEnvironment.getInstances();
        //Instantiate the instances
        for (Map.Entry<String, ComponentInfo> entry : instances) {
            stringBuilder.append(String.format("FMI2Component %s = %s.instantiate(\"%s\",false,false);\n", entry.getKey(),
                    removeFmuKeyBraces(entry.getValue().fmuIdentifier), entry.getKey()));
        }
        // Create the components
        stringBuilder.append(String.format("IFmuComponent components[%d]={%s};\n", instances.size(),
                instances.stream().map(l -> l.getKey()).collect(Collectors.joining(","))));

        stringBuilder.append("bool global_execution_continue = true;\n");

        stringBuilder.append("expand initialize(components,START_TIME, END_TIME);\n");

        stringBuilder.append("expand fixedStep(components,STEP_SIZE,START_TIME,END_TIME);\n");

        for (Map.Entry<String, ComponentInfo> entry : instances) {
            stringBuilder.append(String.format("%s.terminate();\n", entry.getKey()));
            stringBuilder.append(String.format("%s.freeInstance(%s);\n", removeFmuKeyBraces(entry.getValue().fmuIdentifier), entry.getKey()));
        }

        for (Map.Entry<String, ModelDescription> entry : fmusToModelDescriptions) {
            stringBuilder.append(String.format("unload(%s);\n", removeFmuKeyBraces(entry.getKey())));
        }

        stringBuilder.append("unload(dataWriter);\n");

        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}