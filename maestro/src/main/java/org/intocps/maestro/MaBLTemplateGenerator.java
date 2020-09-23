package org.intocps.maestro;

import org.intocps.maestro.ast.ALocalVariableStm;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.PStm;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.net.URI;
import java.util.*;

public class MaBLTemplateGenerator {

    public static final String START_TIME_NAME = "START_TIME";
    public static final String END_TIME_NAME = "END_TIME";
    public static final String STEP_SIZE_NAME = "STEP_SIZE";

    private static ALocalVariableStm createRealVariable(String lexName, Double initializerValue) {
        return MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(new LexIdentifier(lexName, null), MableAstFactory.newARealNumericPrimitiveType(),
                        MableAstFactory.newAExpInitializer(MableAstFactory.newARealLiteralExp(initializerValue))));
    }

    public static String removeFmuKeyBraces(String fmuKey) {
        return fmuKey.substring(1, fmuKey.length() - 1);
    }

    public static List<PStm> generateTemplate(UnitRelationship unitRelationship) throws XPathExpressionException {
        List<PStm> statements = new ArrayList<>();
        // Skipping start, end time and step size for now
        // Set up start time
        // Set up end time
        // Set up step size
        //        ALocalVariableStm start_time = createRealVariable(START_TIME_NAME, 0.0);
        //        ALocalVariableStm end_time = createRealVariable(END_TIME_NAME, message.endTime);
        //        ALocalVariableStm step_size = createRealVariable(STEP_SIZE_NAME, message.stepSize);
        //        statements.addAll(Arrays.asList(start_time, end_time, step_size));

        // Create load statements
        HashMap<String, String> fmuNameToLexIdentifier = new HashMap<>();
        for (Map.Entry<String, ModelDescription> entry : unitRelationship.getFmusWithModelDescriptions()) {
            String fmuLexName = removeFmuKeyBraces(entry.getKey());

            PStm fmuLoadStatement = createFMULoad(fmuLexName, entry, unitRelationship.getUriFromFMUName(entry.getKey()));

            statements.add(fmuLoadStatement);
            fmuNameToLexIdentifier.put(entry.getKey(), fmuLexName);
        }

        // Create Instantiate Statements
        HashMap<String, String> instanceLexToInstanceName = new HashMap<>();
        Set<String> invalidNames = new HashSet<>(fmuNameToLexIdentifier.values());
        unitRelationship.getInstances().forEach(entry -> {
            // Find parent lex
            String parentLex = fmuNameToLexIdentifier.get(entry.getValue().fmuIdentifier);
            // Get instanceName
            String instanceLexName = findInstanceLexName(entry.getKey(), invalidNames);
            invalidNames.add(instanceLexName);
            instanceLexToInstanceName.put(instanceLexName, entry.getKey());

            PStm instantiateStatement = createFMUInstantiateStatement(instanceLexName, parentLex);
            statements.add(instantiateStatement);
        });
        //Todo: Create map from lex to instanceName


        return statements;


    }

    private static String findInstanceLexName(String preferredName, Collection<String> invalidNames) {
        // Remove dots
        String preferredNameNoDots = preferredName.replace('.', '_');
        String proposedName = preferredNameNoDots;
        int addition = 1;
        while (invalidNames.contains(proposedName)) {
            proposedName = preferredNameNoDots + "_" + addition;
            addition++;
        }
        return proposedName;
    }

    private static PStm createFMULoad(String fmuLexName, Map.Entry<String, ModelDescription> entry,
            URI uriFromFMUName) throws XPathExpressionException {
        return MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(MableAstFactory.newAIdentifier(fmuLexName), MableAstFactory.newANameType("FMI2"), MableAstFactory
                        .newAExpInitializer(MableAstFactory.newACallExp(MableAstFactory.newAIdentifier("load"),
                                Arrays.asList(MableAstFactory.newAStringLiteralExp("FMI2"),
                                        MableAstFactory.newAStringLiteralExp(entry.getValue().getGuid()),
                                        MableAstFactory.newAStringLiteralExp(uriFromFMUName.toString()))))));
    }

    private static PStm createFMUInstantiateStatement(String instanceName, String fmuLexName) {
        return MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(new LexIdentifier(instanceName, null), MableAstFactory.newANameType("FMI2Component"), MableAstFactory
                        .newAExpInitializer(MableAstFactory
                                .newACallExp(MableAstFactory.newAIdentifierExp(fmuLexName), MableAstFactory.newAIdentifier("instantiate"),
                                        Arrays.asList(MableAstFactory.newAStringLiteralExp(instanceName), MableAstFactory.newABoolLiteralExp(false),
                                                MableAstFactory.newABoolLiteralExp(false))))));

    }
}
