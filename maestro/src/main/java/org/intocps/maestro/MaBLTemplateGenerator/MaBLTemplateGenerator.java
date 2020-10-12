package org.intocps.maestro.MaBLTemplateGenerator;

import org.apache.commons.lang3.StringUtils;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.api.FixedStepSizeAlgorithm;
import org.intocps.maestro.core.api.IStepAlgorithm;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.plugin.IMaestroPlugin;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.call;
import static org.intocps.maestro.ast.MableBuilder.newVariable;

public class MaBLTemplateGenerator {

    public static final String START_TIME_NAME = "START_TIME";
    public static final String END_TIME_NAME = "END_TIME";
    public static final String STEP_SIZE_NAME = "STEP_SIZE";
    public static final String MATH_MODULE_NAME = "Math";
    public static final String LOGGER_MODULE_NAME = "Logger";
    public static final String DATAWRITER_MODULE_NAME = "DataWriter";
    public static final String INITIALIZE_EXPANSION_FUNCTION_NAME = "initialize";
    public static final String FIXEDSTEP_EXPANSION_FUNCTION_NAME = "fixedStep";
    public static final String DEBUG_LOGGING_EXPANSION_FUNCTION_NAME = "enableDebugLogging";
    public static final String FMI2COMPONENT_TYPE = "FMI2Component";
    public static final String COMPONENTS_ARRAY_NAME = "components";
    public static final String GLOBAL_EXECUTION_CONTINUE = IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE;
    public static final String LOGLEVELS_POSTFIX = "_log_levels";
    public static final String IMPORT_DEBUGLOGGING = "DebugLogging";
    public static final String IMPORT_FIXEDSTEP = "FixedStep";
    public static final String IMPORT_TYPECONVERTER = "TypeConverter";
    public static final String IMPORT_INITIALIZER = "Initializer";


    public static ALocalVariableStm createRealVariable(String lexName, Double initializerValue) {
        return MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(new LexIdentifier(lexName, null), MableAstFactory.newARealNumericPrimitiveType(),
                        MableAstFactory.newAExpInitializer(MableAstFactory.newARealLiteralExp(initializerValue))));
    }

    public static String removeFmuKeyBraces(String fmuKey) {
        return fmuKey.substring(1, fmuKey.length() - 1);
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
        return newVariable(fmuLexName, newANameType("FMI2"),
                call("load", newAStringLiteralExp("FMI2"), newAStringLiteralExp(entry.getValue().getGuid()),
                        newAStringLiteralExp(uriFromFMUName.toString())));

    }

    public static PStm createFMUUnload(String fmuLexName) {
        return MableAstFactory.newExpressionStm(
                MableAstFactory.newACallExp(MableAstFactory.newAIdentifier("unload"), Arrays.asList(MableAstFactory.newAIdentifierExp(fmuLexName))));
    }

    public static List<PStm> createFMUInstantiateStatement(String instanceLexName, String instanceEnvironmentKey, String fmuLexName, boolean visible,
            boolean loggingOn) {
        AInstanceMappingStm mapping = newAInstanceMappingStm(newAIdentifier(instanceLexName), instanceEnvironmentKey);
        PStm var = newVariable(instanceLexName, newANameType("FMI2Component"), newNullExp());


        AIfStm ifAssign = newIf(newAIdentifierExp(GLOBAL_EXECUTION_CONTINUE), newABlockStm(
                newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(instanceLexName)),
                        call(fmuLexName, "instantiate", newAStringLiteralExp(instanceEnvironmentKey), newABoolLiteralExp(visible),
                                newABoolLiteralExp(loggingOn))), checkNullAndStop(instanceLexName)), null);
        return Arrays.asList(mapping, var, ifAssign);
    }

    public static ExpandStatements generateAlgorithmStms(IStepAlgorithm algorithm) {
        switch (algorithm.getType()) {
            case FIXEDSTEP:
                FixedStepSizeAlgorithm a = (FixedStepSizeAlgorithm) algorithm;
                return new ExpandStatements(
                        Arrays.asList(createRealVariable(STEP_SIZE_NAME, a.stepSize), createRealVariable(END_TIME_NAME, a.endTime)),
                        Arrays.asList(createExpandFixedStep(COMPONENTS_ARRAY_NAME, STEP_SIZE_NAME, START_TIME_NAME, END_TIME_NAME)));
            default:
                throw new IllegalArgumentException("Algorithm type is unknown.");
        }
    }

    public static ASimulationSpecificationCompilationUnit generateTemplate(
            MaBLTemplateConfiguration templateConfiguration) throws XPathExpressionException {

        // This variable determines whether an expansion should be wrapped in globalExecutionContinue or not.
        boolean wrapExpansionPluginInGlobalExecutionContinue = false;

        StatementMaintainer stmMaintainer = new StatementMaintainer();
        stmMaintainer.add(createGlobalExecutionContinue());

        stmMaintainer.addAll(generateLoadUnloadStms(MaBLTemplateGenerator::createLoadStatement));

        FmiSimulationEnvironment unitRelationShip = templateConfiguration.getUnitRelationship();

        // Create FMU load statements
        List<PStm> unloadFmuStatements = new ArrayList<>();
        HashMap<String, String> fmuNameToLexIdentifier = new HashMap<>();
        for (Map.Entry<String, ModelDescription> entry : unitRelationShip.getFmusWithModelDescriptions()) {
            String fmuLexName = removeFmuKeyBraces(entry.getKey());

            stmMaintainer.add(createFMULoad(fmuLexName, entry, unitRelationShip.getUriFromFMUName(entry.getKey())));
            stmMaintainer.add(checkNullAndStop(fmuLexName));
            unloadFmuStatements.add(createFMUUnload(fmuLexName));

            fmuNameToLexIdentifier.put(entry.getKey(), fmuLexName);
        }

        // Create Instantiate Statements
        HashMap<String, String> instanceLexToInstanceName = new HashMap<>();
        Set<String> invalidNames = new HashSet<>(fmuNameToLexIdentifier.values());
        List<PStm> freeInstanceStatements = new ArrayList<>();
        Map<String, String> instaceNameToInstanceLex = new HashMap<>();
        unitRelationShip.getInstances().forEach(entry -> {
            // Find parent lex
            String parentLex = fmuNameToLexIdentifier.get(entry.getValue().fmuIdentifier);
            // Get instanceName
            String instanceLexName = findInstanceLexName(entry.getKey(), invalidNames);
            invalidNames.add(instanceLexName);
            instanceLexToInstanceName.put(instanceLexName, entry.getKey());
            instaceNameToInstanceLex.put(entry.getKey(), instanceLexName);

            stmMaintainer.addAll(createFMUInstantiateStatement(instanceLexName, entry.getKey(), parentLex, templateConfiguration.getVisible(),
                    templateConfiguration.getLoggingOn()));
            freeInstanceStatements.add(createFMUFreeInstanceStatement(instanceLexName, parentLex));
        });


        // Debug logging
        if (templateConfiguration.getLogLevels() != null) {
            stmMaintainer.addAll(createDebugLoggingStms(instaceNameToInstanceLex, templateConfiguration.getLogLevels()));
            stmMaintainer.wrapInIfBlock();
        }

        // Components Array
        stmMaintainer.add(createComponentsArray(COMPONENTS_ARRAY_NAME, instanceLexToInstanceName.keySet()));

        stmMaintainer.add(createRealVariable(START_TIME_NAME, 0.0));

        // Generate variable statements related to the given algorithm. I.e. the variable step size for fixed step.
        ExpandStatements algorithmStatements = null;
        if (templateConfiguration.getAlgorithm() != null) {
            algorithmStatements = generateAlgorithmStms(templateConfiguration.getAlgorithm());
            if (algorithmStatements.variablesToTopOfMabl != null) {
                stmMaintainer.addAll(algorithmStatements.variablesToTopOfMabl);
            }
        }

        // Add the initializer expand stm
        if (templateConfiguration.getInitialize()) {
            stmMaintainer.add(createExpandInitialize(COMPONENTS_ARRAY_NAME, START_TIME_NAME, END_TIME_NAME));
        }

        // Add the algorithm expand stm
        if (algorithmStatements.body != null) {
            stmMaintainer.addAll(algorithmStatements.body);
        }

        // Free instances
        stmMaintainer.addAllCleanup(freeInstanceStatements);

        // Unload the FMUs
        stmMaintainer.addAllCleanup(unloadFmuStatements);
        stmMaintainer.addAllCleanup(generateLoadUnloadStms(x -> createUnloadStatement(StringUtils.uncapitalize(x))));

        return MableAstFactory.newASimulationSpecificationCompilationUnit(
                Arrays.asList(MableAstFactory.newAIdentifier(IMPORT_FIXEDSTEP), MableAstFactory.newAIdentifier(IMPORT_TYPECONVERTER),
                        MableAstFactory.newAIdentifier(IMPORT_INITIALIZER), MableAstFactory.newAIdentifier(IMPORT_DEBUGLOGGING)),
                MableAstFactory.newABlockStm(stmMaintainer.getStatements()));
    }


    private static PStm checkNullAndStop(String identifier) {
        return newIf(newEqual(newAIdentifierExp(identifier), newNullExp()),
                newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(GLOBAL_EXECUTION_CONTINUE)), newABoolLiteralExp(false)), null);
    }

    private static Collection<? extends PStm> createDebugLoggingStms(Map<String, String> instaceNameToInstanceLex,
            Map<String, List<String>> logLevels) {
        List<PStm> stms = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : logLevels.entrySet()) {
            stms.addAll(createExpandDebugLogging(instaceNameToInstanceLex.get(entry.getKey()), entry.getValue()));
        }

        return stms;
    }

    private static List<PStm> createExpandDebugLogging(String instanceLexName, List<String> logLevels) {

        String arrayName = instanceLexName + LOGLEVELS_POSTFIX;
        List<PExp> stringLiterals = logLevels.stream().map(MableAstFactory::newAStringLiteralExp).collect(Collectors.toList());
        ALocalVariableStm arrayContent = MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(MableAstFactory.newAIdentifier(arrayName),
                        MableAstFactory.newAArrayType(MableAstFactory.newAStringPrimitiveType(), logLevels.size()),
                        MableAstFactory.newAArrayInitializer(stringLiterals)));

        AExpressionStm expandCall = MableAstFactory.newExpressionStm(MableAstFactory
                .newACallExp(MableAstFactory.newExpandToken(), MableAstFactory.newAIdentifier(DEBUG_LOGGING_EXPANSION_FUNCTION_NAME),
                        Arrays.asList(MableAstFactory.newAIdentifierExp(instanceLexName), MableAstFactory.newAIdentifierExp(arrayName),
                                MableAstFactory.newAUIntLiteralExp(Long.valueOf(logLevels.size())))));

        return Arrays.asList(arrayContent, expandCall);

    }

    private static PStm createGlobalExecutionContinue() {
        return MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(MableAstFactory.newAIdentifier(GLOBAL_EXECUTION_CONTINUE), MableAstFactory.newABoleanPrimitiveType(),
                        MableAstFactory.newAExpInitializer(MableAstFactory.newABoolLiteralExp(true))));
    }

    private static PStm createFMUFreeInstanceStatement(String instanceLexName, String fmuLexName) {
        return MableAstFactory.newExpressionStm(MableAstFactory
                .newACallExp(MableAstFactory.newAIdentifierExp(fmuLexName), MableAstFactory.newAIdentifier("freeInstance"),
                        Arrays.asList(MableAstFactory.newAIdentifierExp(instanceLexName))));
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
        return MableAstFactory.newExpressionStm(MableAstFactory.newUnloadExp(Arrays.asList(MableAstFactory.newAIdentifierExp(moduleName))));
    }

    private static Collection<? extends PStm> generateLoadUnloadStms(Function<String, PStm> function) {
        return Arrays.asList(MATH_MODULE_NAME, LOGGER_MODULE_NAME, DATAWRITER_MODULE_NAME).stream().map(x -> function.apply(x))
                .collect(Collectors.toList());
    }

    private static PStm createLoadStatement(String moduleName) {
        return MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(MableAstFactory.newAIdentifier(StringUtils.uncapitalize(moduleName)),
                        MableAstFactory.newANameType(moduleName), MableAstFactory
                                .newAExpInitializer(MableAstFactory.newALoadExp(Arrays.asList(MableAstFactory.newAStringLiteralExp(moduleName))))));
    }

    public static PStm createExpandInitialize(String componentsArrayLexName, String startTimeLexName, String endTimeLexName) {
        return MableAstFactory.newExpressionStm(MableAstFactory
                .newACallExp(MableAstFactory.newExpandToken(), MableAstFactory.newAIdentifier(INITIALIZE_EXPANSION_FUNCTION_NAME),
                        Arrays.asList(AIdentifierExpFromString(componentsArrayLexName), AIdentifierExpFromString(startTimeLexName),
                                AIdentifierExpFromString(endTimeLexName))));
    }

    public static PStm createExpandFixedStep(String componentsArrayLexName, String stepSizeLexName, String startTimeLexName, String endTimeLexName) {
        return MableAstFactory.newExpressionStm(MableAstFactory
                .newACallExp(MableAstFactory.newExpandToken(), MableAstFactory.newAIdentifier(FIXEDSTEP_EXPANSION_FUNCTION_NAME),
                        Arrays.asList(AIdentifierExpFromString(componentsArrayLexName), AIdentifierExpFromString(stepSizeLexName),
                                AIdentifierExpFromString(startTimeLexName), AIdentifierExpFromString(endTimeLexName))));
    }

    public static AIdentifierExp AIdentifierExpFromString(String x) {
        return MableAstFactory.newAIdentifierExp(MableAstFactory.newAIdentifier(x));
    }

    public static class ExpandStatements {
        public List<PStm> variablesToTopOfMabl;
        public List<PStm> body;

        public ExpandStatements(List<PStm> variablesToTopOfMabl, List<PStm> body) {
            this.variablesToTopOfMabl = variablesToTopOfMabl;
            this.body = body;
        }
    }

    public static class StatementMaintainer {
        List<PStm> statements = new ArrayList<>();
        List<PStm> ifBlock;
        List<PStm> cleanup = new ArrayList<>();
        boolean wrapInIfBlock = false;

        public void addCleanup(PStm stm) {
            cleanup.add(stm);
        }

        public void addAllCleanup(Collection<? extends PStm> stms) {
            cleanup.addAll(stms);
        }

        public List<PStm> getStatements() {
            List<PStm> stms = new ArrayList<>();
            stms.addAll(statements);
            if (wrapInIfBlock) {
                stms.add(newIf(newAIdentifierExp(IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE), newABlockStm(ifBlock), null));
            }
            stms.addAll(cleanup);

            return stms;
        }

        public void wrapInIfBlock() {
            this.wrapInIfBlock = true;
            this.ifBlock = new ArrayList<>();
        }

        public void add(PStm stm) {
            if (wrapInIfBlock) {
                this.ifBlock.add(stm);
            } else {
                this.statements.add(stm);
            }
        }

        public void addAll(Collection<? extends PStm> stms) {
            if (wrapInIfBlock) {
                this.ifBlock.addAll(stms);
            } else {
                this.statements.addAll(stms);
            }
        }
    }
}
