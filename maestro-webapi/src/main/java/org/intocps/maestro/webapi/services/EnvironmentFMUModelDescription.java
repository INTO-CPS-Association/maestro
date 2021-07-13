package org.intocps.maestro.webapi.services;

import org.intocps.maestro.fmi.Fmi2ModelDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EnvironmentFMUModelDescription {
    public static String createEnvironmentFMUModelDescription(List<Fmi2ModelDescription.ScalarVariable> inputs,
            List<Fmi2ModelDescription.ScalarVariable> outputs, String fmuName) {
        return "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" + "<fmiModelDescription \tfmiVersion=\"2.0\"\n" + "\t\t\t\t\t\tmodelName=\"" +
                fmuName + "\"\n" + "\t\t\t\t\t\tguid=\"{abb4bff1-d423-4e02-90d9-011f519869ff}\"\n" +
                "\t\t\t\t\t\tvariableNamingConvention=\"flat\"\n" + "\t\t\t\t\t\tnumberOfEventIndicators=\"0\">\n" +
                "\t<CoSimulation \tmodelIdentifier=\"" + fmuName + "\"\n" + "\t\t\t\t\tneedsExecutionTool=\"false\"\n" +
                "\t\t\t\t\tcanHandleVariableCommunicationStepSize=\"true\"\n" + "\t\t\t\t\tcanInterpolateInputs=\"false\"\n" +
                "\t\t\t\t\tmaxOutputDerivativeOrder=\"0\"\n" + "\t\t\t\t\tcanRunAsynchronuously=\"false\"\n" +
                "\t\t\t\t\tcanBeInstantiatedOnlyOncePerProcess=\"true\"\n" + "\t\t\t\t\tcanNotUseMemoryManagementFunctions=\"true\"\n" +
                "\t\t\t\t\tcanGetAndSetFMUstate=\"false\"\n" + "\t\t\t\t\tcanSerializeFMUstate=\"false\"\n" +
                "\t\t\t\t\tprovidesDirectionalDerivative=\"false\">\n" + "\t\t\t\t\t\n" + "\t\t</CoSimulation>\n" + "\n" + "\t<ModelVariables>\n" +
                (outputs != null ? createScalarVariables(outputs) : "") + (inputs != null ? createScalarVariables(inputs) : "") +
                //                "\t\t<ScalarVariable name=\"backwardRotate\" valueReference=\"1\" causality=\"parameter\" variability=\"fixed\"  initial=\"exact\" ><Real start=\"0.1\" /></ScalarVariable>\n" +
                //                "    \n" +
                //                "\t\t<ScalarVariable name=\"forwardRotate\" valueReference=\"2\" causality=\"parameter\" variability=\"fixed\"  initial=\"exact\" ><Real start=\"0.5\" /></ScalarVariable>\n" +
                //                "    \n" +
                //                "\t\t<ScalarVariable name=\"forwardSpeed\" valueReference=\"3\" causality=\"parameter\" variability=\"fixed\"  initial=\"exact\" ><Real start=\"0.4\" /></ScalarVariable>\n" +
                //                "    \n" +
                //                "\t\t<ScalarVariable name=\"lfLeftVal\" valueReference=\"4\" causality=\"input\" variability=\"continuous\" ><Real start=\"0\" /></ScalarVariable>\n" +
                //                "    \n" +
                //                "\t\t<ScalarVariable name=\"lfRightVal\" valueReference=\"5\" causality=\"input\" variability=\"continuous\" ><Real start=\"0\" /></ScalarVariable>\n" +
                //                "    \n" +
                //                "\t\t<ScalarVariable name=\"servoLeftVal\" valueReference=\"6\" causality=\"output\" variability=\"continuous\" ><Real  /></ScalarVariable>\n" +
                //                "    \n" +
                //                "\t\t<ScalarVariable name=\"servoRightVal\" valueReference=\"7\" causality=\"output\" variability=\"continuous\" ><Real  /></ScalarVariable>\n" +
                "    </ModelVariables>\n" + "\n" + "\t<ModelStructure>\n" + createOutputs(outputs, 1) + "\n" + "\t</ModelStructure>\n" +
                "</fmiModelDescription>\n";

    }

    public static String createOutputs(List<Fmi2ModelDescription.ScalarVariable> outputs, int startIndex) {
        String returnString = "";

        if (outputs != null && outputs.size() > 0) {
            returnString = "\t<Outputs>\n" + createEmptyDependencies(outputs.size(), 1) + "            \n" + "\t</Outputs>\n";
        }

        return returnString;
    }

    public static String createEmptyDependencies(int size, int startIndex) {
        ArrayList<String> emptyDependencies = new ArrayList<>();
        for (int i = startIndex; i < startIndex + size; i++) {
            emptyDependencies.add(String.format("\t\t<Unknown index=\"%d\" dependencies=\"\"/>", i));
        }
        return String.join("\n\n", emptyDependencies);
    }

    public static String createScalarVariables(List<Fmi2ModelDescription.ScalarVariable> scalarVariables) {
        String svsString = scalarVariables.stream().map(sv -> createScalarVariable(sv)).collect(Collectors.joining("\n\n"));
        return svsString;
    }

    /**
     * Create the XML representation of a Scalar Variable.
     * NOTE: SKIPS THE INITIAL FIELD
     *
     * @param sv
     * @return
     */
    public static String createScalarVariable(Fmi2ModelDescription.ScalarVariable sv) {
        return String
                .format("<ScalarVariable " + "name=\"%s\" " + "valueReference=\"%s\" " + "causality=\"%s\" " + "variability=\"%s\"" + "%s>" + "%s" +
                                "</ScalarVariable>", sv.name, sv.valueReference, causalityToString(sv.causality), variabilityToString(sv.variability),
                        initialToString(sv.initial), typeDefinitionToString(sv.type));


    }

    private static String initialToString(Fmi2ModelDescription.Initial initial) {
        String initialString = "";

        if (initial != null) {
            switch (initial) {
                case Exact:
                    initialString = " initial=\"exact\"";
                    break;
                case Approx:
                    initialString = " initial=\"approx\"";
                    break;
                case Calculated:
                    initialString = " initial=\"calculated\"";
                    break;
            }
        }
        return initialString;
    }

    public static String typeDefinitionToString(Fmi2ModelDescription.Type type) {
        StringBuilder typeDefinitionBuilder = new StringBuilder();
        typeDefinitionBuilder.append(String.format("<%s ", type.type.toString()));
        if (type.start != null) {
            typeDefinitionBuilder.append(String.format(" start=\"%s\"", type.start.toString()));
        }
        typeDefinitionBuilder.append("/>");
        return typeDefinitionBuilder.toString();
    }

    public static String variabilityToString(Fmi2ModelDescription.Variability variability) {
        switch (variability) {
            case Constant:
                return "constant";
            case Fixed:
                return "fixed";
            case Tunable:
                return "tunable";
            case Discrete:
                return "discrete";
            case Continuous:
                return "continuous";
        }
        return null;
    }

    public static String causalityToString(Fmi2ModelDescription.Causality causality) {
        switch (causality) {
            case Parameter:
                return "parameter";
            case CalculatedParameter:
                return "calculatedParameter";
            case Input:
                return "input";
            case Output:
                return "output";
            case Local:
                return "local";
            case Independent:
                return "independent";
        }
        return null;
    }
}
