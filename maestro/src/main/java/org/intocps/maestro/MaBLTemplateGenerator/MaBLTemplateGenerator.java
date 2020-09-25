package org.intocps.maestro.MaBLTemplateGenerator;

import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.API.FixedStepSizeAlgorithm;
import org.intocps.maestro.core.API.IStepAlgorithm;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MaBLTemplateGenerator {

    public static final String START_TIME_NAME = "START_TIME";
    public static final String END_TIME_NAME = "END_TIME";
    public static final String STEP_SIZE_NAME = "STEP_SIZE";
    public static final String MATH_MODULE_NAME = "Math";
    public static final String LOGGER_MODULE_NAME = "Logger";
    public static final String DATAWRITER_MODULE_NAME = "DataWriter";
    public static final String INITIALIZE_EXPANSION_FUNCTION_NAME = "initialize";
    public static final String FMI2COMPONENT_TYPE = "FMI2Component";

    public static ALocalVariableStm createRealVariable(String lexName, Double initializerValue) {
        return MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(new LexIdentifier(lexName, null), MableAstFactory.newARealNumericPrimitiveType(),
                        MableAstFactory.newAExpInitializer(MableAstFactory.newARealLiteralExp(initializerValue))));
    }

    public static String removeFmuKeyBraces(String fmuKey) {
        return fmuKey.substring(1, fmuKey.length() - 1);
    }

    public static PStm createInstanceMappingStms(Map.Entry<String, String> entryInstanceLextoInstanceName) {
        return MableAstFactory.newAInstanceMappingStm(MableAstFactory.newAIdentifier(entryInstanceLextoInstanceName.getKey()),
                entryInstanceLextoInstanceName.getValue());
    }

    public static String findInstanceLexName(String preferredName, Collection<String> invalidNames) {
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

    public static PStm createFMULoad(String fmuLexName, Map.Entry<String, ModelDescription> entry,
            URI uriFromFMUName) throws XPathExpressionException {
        return MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(MableAstFactory.newAIdentifier(fmuLexName), MableAstFactory.newANameType("FMI2"), MableAstFactory
                        .newAExpInitializer(MableAstFactory.newACallExp(MableAstFactory.newAIdentifier("load"),
                                Arrays.asList(MableAstFactory.newAStringLiteralExp("FMI2"),
                                        MableAstFactory.newAStringLiteralExp(entry.getValue().getGuid()),
                                        MableAstFactory.newAStringLiteralExp(uriFromFMUName.toString()))))));
    }

    public static PStm createFMUInstantiateStatement(String instanceName, String fmuLexName) {
        return MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(new LexIdentifier(instanceName, null), MableAstFactory.newANameType("FMI2Component"), MableAstFactory
                        .newAExpInitializer(MableAstFactory
                                .newACallExp(MableAstFactory.newAIdentifierExp(fmuLexName), MableAstFactory.newAIdentifier("instantiate"),
                                        Arrays.asList(MableAstFactory.newAStringLiteralExp(instanceName), MableAstFactory.newABoolLiteralExp(false),
                                                MableAstFactory.newABoolLiteralExp(false))))));

    }

    public static List<PStm> generateAlgorithmStms(IStepAlgorithm algorithm) {
        switch (algorithm.getType()) {
            case FIXEDSTEP:
                FixedStepSizeAlgorithm a = (FixedStepSizeAlgorithm) algorithm;
                return Arrays.asList(createRealVariable(STEP_SIZE_NAME, a.stepSize), createRealVariable(END_TIME_NAME, a.endTime));
            default:
                throw new IllegalArgumentException("Algorithm type is unknown.");
        }
    }

    public static ASimulationSpecificationCompilationUnit generateTemplate(
            MaBLTemplateConfiguration templateConfiguration) throws XPathExpressionException {

        ArrayList<PStm> statements = new ArrayList<>();
        statements.addAll(generateLoadUnloadStms(MaBLTemplateGenerator::createLoadStatement));

        UnitRelationship unitRelationShip = templateConfiguration.getUnitRelationship();

        // Create load statements
        HashMap<String, String> fmuNameToLexIdentifier = new HashMap<>();
        for (Map.Entry<String, ModelDescription> entry : unitRelationShip.getFmusWithModelDescriptions()) {
            String fmuLexName = removeFmuKeyBraces(entry.getKey());

            PStm fmuLoadStatement = createFMULoad(fmuLexName, entry, unitRelationShip.getUriFromFMUName(entry.getKey()));

            statements.add(fmuLoadStatement);
            fmuNameToLexIdentifier.put(entry.getKey(), fmuLexName);
        }

        // Create Instantiate Statements
        HashMap<String, String> instanceLexToInstanceName = new HashMap<>();
        Set<String> invalidNames = new HashSet<>(fmuNameToLexIdentifier.values());
        unitRelationShip.getInstances().forEach(entry -> {
            // Find parent lex
            String parentLex = fmuNameToLexIdentifier.get(entry.getValue().fmuIdentifier);
            // Get instanceName
            String instanceLexName = findInstanceLexName(entry.getKey(), invalidNames);
            invalidNames.add(instanceLexName);
            instanceLexToInstanceName.put(instanceLexName, entry.getKey());

            PStm instantiateStatement = createFMUInstantiateStatement(instanceLexName, parentLex);
            statements.add(instantiateStatement);
        });
        //Map from instance lex to instance name
        for (Map.Entry<String, String> entryIsntanceLextoInstanceName : instanceLexToInstanceName.entrySet()) {
            PStm mappingStm = createInstanceMappingStms(entryIsntanceLextoInstanceName);
            statements.add(mappingStm);
        }

        // Components Array
        statements.add(createComponentsArray("components", instanceLexToInstanceName.keySet()));


        statements.add(createRealVariable(START_TIME_NAME, 0.0));

        if (templateConfiguration.getInitialize()) {
            statements.add(createExpandInitialize("components", START_TIME_NAME, END_TIME_NAME));

        }

        if (templateConfiguration.getAlgorithm() != null) {
            statements.addAll(generateAlgorithmStms(templateConfiguration.getAlgorithm()));
        }

        statements.addAll(generateLoadUnloadStms(MaBLTemplateGenerator::createUnloadStatement));

        return MableAstFactory.newASimulationSpecificationCompilationUnit(null, MableAstFactory.newABlockStm(statements));
    }

    private static Collection<? extends PStm> generateUnloadStms() {
        return null;
    }

    private static PStm createComponentsArray(String lexName, Set<String> keySet) {
        return MableAstFactory.newALocalVariableStm(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier(lexName),
                MableAstFactory.newAArrayType(MableAstFactory.newANameType(FMI2COMPONENT_TYPE), keySet.size()),
                MableAstFactory.newAArrayInitializer(keySet.stream().map(x -> AIdentifierExpFromString(x)).collect(Collectors.toList()))));
    }

    private static PStm createUnloadStatement(String moduleName) {
        return MableAstFactory.newExpressionStm(MableAstFactory.newUnloadExp(Arrays.asList(MableAstFactory.newAStringLiteralExp(moduleName))));
    }

    private static Collection<? extends PStm> generateLoadUnloadStms(Function<String, PStm> function) {
        return Arrays.asList(MATH_MODULE_NAME, LOGGER_MODULE_NAME, DATAWRITER_MODULE_NAME).stream().map(x -> function.apply(x))
                .collect(Collectors.toList());
    }

    private static PStm createLoadStatement(String moduleName) {
        return MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(MableAstFactory.newAIdentifier(moduleName.toLowerCase()), MableAstFactory.newANameType(moduleName),
                        MableAstFactory
                                .newAExpInitializer(MableAstFactory.newALoadExp(Arrays.asList(MableAstFactory.newAStringLiteralExp(moduleName))))));
    }

    public static PStm createExpandInitialize(String componentsArrayLexName, String startTimeLexName, String endTimeLexName) {
        return MableAstFactory.newExpressionStm(MableAstFactory
                .newACallExp(MableAstFactory.newExpandToken(), MableAstFactory.newAIdentifier(INITIALIZE_EXPANSION_FUNCTION_NAME),
                        Arrays.asList(AIdentifierExpFromString(componentsArrayLexName), AIdentifierExpFromString(startTimeLexName),
                                AIdentifierExpFromString(endTimeLexName))));
    }

    public static AIdentifierExp AIdentifierExpFromString(String x) {
        return MableAstFactory.newAIdentifierExp(MableAstFactory.newAIdentifier(x));
    }

    enum LOADUNLOAD {
        LOAD,
        UNLOAD
    }
}
