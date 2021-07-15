package org.intocps.maestro.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.dto.IAlgorithmConfig;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.framework.fmi2.FaultInject;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.plugin.IMaestroPlugin;
import org.intocps.maestro.plugin.JacobianStepConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;
import java.net.URI;
import java.util.*;
import java.util.function.BiFunction;
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
    public static final String BOOLEANLOGIC_MODULE_NAME = "BooleanLogic";
    public static final String LOGGER_MODULE_NAME = "Logger";
    public static final String DATAWRITER_MODULE_NAME = "DataWriter";
    public static final String FMI2_MODULE_NAME = "FMI2";
    public static final String TYPECONVERTER_MODULE_NAME = "TypeConverter";
    public static final String INITIALIZE_EXPANSION_FUNCTION_NAME = "initialize";
    public static final String INITIALIZE_EXPANSION_MODULE_NAME = "Initializer";
    public static final String FIXEDSTEP_FUNCTION_NAME = "fixedStepSize";
    public static final String VARIABLESTEP_FUNCTION_NAME = "variableStepSize";
    public static final String JACOBIANSTEP_EXPANSION_MODULE_NAME = "JacobianStepBuilder";
    public static final String ARRAYUTIL_EXPANSION_MODULE_NAME = "ArrayUtil";
    public static final String DEBUG_LOGGING_EXPANSION_FUNCTION_NAME = "enableDebugLogging";
    public static final String DEBUG_LOGGING_MODULE_NAME = "DebugLogging";
    public static final String FMI2COMPONENT_TYPE = "FMI2Component";
    public static final String COMPONENTS_ARRAY_NAME = "components";
    public static final String GLOBAL_EXECUTION_CONTINUE = IMaestroPlugin.GLOBAL_EXECUTION_CONTINUE;
    public static final String STATUS = IMaestroPlugin.FMI_STATUS_VARIABLE_NAME;
    public static final String LOGLEVELS_POSTFIX = "_log_levels";
    public static final String FAULT_INJECT_MODULE_NAME = "FaultInject";
    public static final String FAULT_INJECT_MODULE_VARIABLE_NAME = "faultInject";
    final static Logger logger = LoggerFactory.getLogger(MaBLTemplateGenerator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

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

    public static PStm createFMULoad(String fmuLexName, Map.Entry<String, Fmi2ModelDescription> entry,
            URI uriFromFMUName) throws XPathExpressionException {

        String path = uriFromFMUName.toString();
        if (uriFromFMUName.getScheme() != null && uriFromFMUName.getScheme().equals("file")) {
            path = uriFromFMUName.getPath();
        }
        return newVariable(fmuLexName, newANameType("FMI2"),
                call("load", newAStringLiteralExp("FMI2"), newAStringLiteralExp(entry.getValue().getGuid()), newAStringLiteralExp(path)));

    }

    public static PStm createFMUUnload(String fmuLexName) {
        return MableAstFactory.newExpressionStm(
                MableAstFactory.newACallExp(MableAstFactory.newAIdentifier("unload"), Arrays.asList(MableAstFactory.newAIdentifierExp(fmuLexName))));
    }

    public static List<PStm> createFMUInstantiateStatement(String instanceLexName, String instanceEnvironmentKey, String fmuLexName, boolean visible,
            boolean loggingOn) {
        return createFMUInstantiateStatement(instanceLexName, instanceEnvironmentKey, fmuLexName, visible, loggingOn, Optional.empty());
    }

    public static List<PStm> createFMUInstantiateStatement(String instanceLexName, String instanceEnvironmentKey, String fmuLexName, boolean visible,
            boolean loggingOn, Optional<FaultInject> faultInject) {
        List<PStm> statements = new ArrayList<>();
        String instanceLexName_ = instanceLexName;
        if (faultInject.isPresent()) {

            instanceLexName_ = instanceLexName + "_original";
        }

        AInstanceMappingStm mapping = newAInstanceMappingStm(newAIdentifier(instanceLexName_), instanceEnvironmentKey);
        statements.add(mapping);
        PStm var = newVariable(instanceLexName_, newANameType("FMI2Component"), newNullExp());
        statements.add(var);
        AIfStm ifAssign = newIf(newAIdentifierExp(GLOBAL_EXECUTION_CONTINUE), newABlockStm(
                newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(instanceLexName_)),
                        call(fmuLexName, "instantiate", newAStringLiteralExp(instanceEnvironmentKey), newABoolLiteralExp(visible),
                                newABoolLiteralExp(loggingOn))), checkNullAndStop(instanceLexName_)), null);
        statements.add(ifAssign);

        if (faultInject.isPresent()) {
            String faultInjectLexName = instanceLexName;
            PStm ficomp = newVariable(faultInjectLexName, newANameType("FMI2Component"), newNullExp());
            statements.add(ficomp);
            AIfStm stm = newIf(newAIdentifierExp(GLOBAL_EXECUTION_CONTINUE), newABlockStm(
                    newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(faultInjectLexName)),
                            newACallExp(newAIdentifierExp(FAULT_INJECT_MODULE_VARIABLE_NAME), newAIdentifier("faultInject"),
                                    Arrays.asList(newAIdentifierExp(fmuLexName), newAIdentifierExp(instanceLexName_),
                                            newAStringLiteralExp(faultInject.get().constraintId)))), checkNullAndStop(faultInjectLexName)), null);
            statements.add(stm);
        }


        return statements;
    }

    public static ExpandStatements generateAlgorithmStms(IAlgorithmConfig algorithmConfig) {
        PStm algorithmStm;

        switch (algorithmConfig.getAlgorithmType()) {
            case FIXEDSTEP:
                algorithmStm = MableAstFactory.newExpressionStm(MableAstFactory
                        .newACallExp(newExpandToken(), newAIdentifierExp(MableAstFactory.newAIdentifier(JACOBIANSTEP_EXPANSION_MODULE_NAME)),
                                MableAstFactory.newAIdentifier(FIXEDSTEP_FUNCTION_NAME),
                                Arrays.asList(aIdentifierExpFromString(COMPONENTS_ARRAY_NAME), aIdentifierExpFromString(STEP_SIZE_NAME),
                                        aIdentifierExpFromString(START_TIME_NAME), aIdentifierExpFromString(END_TIME_NAME))));
                break;

            case VARIABLESTEP:
                algorithmStm = MableAstFactory.newExpressionStm(MableAstFactory
                        .newACallExp(newExpandToken(), newAIdentifierExp(MableAstFactory.newAIdentifier(JACOBIANSTEP_EXPANSION_MODULE_NAME)),
                                MableAstFactory.newAIdentifier(VARIABLESTEP_FUNCTION_NAME),
                                Arrays.asList(aIdentifierExpFromString(COMPONENTS_ARRAY_NAME), aIdentifierExpFromString(STEP_SIZE_NAME),
                                        aIdentifierExpFromString(START_TIME_NAME), aIdentifierExpFromString(END_TIME_NAME))));
                break;

            default:
                throw new IllegalArgumentException("Algorithm type is unknown.");
        }

        return new ExpandStatements(Arrays.asList(createRealVariable(STEP_SIZE_NAME, algorithmConfig.getStepSize())), Arrays.asList(algorithmStm));
    }

    public static ASimulationSpecificationCompilationUnit generateTemplate(
            MaBLTemplateConfiguration templateConfiguration) throws XPathExpressionException, JsonProcessingException {

        // This variable determines whether an expansion should be wrapped in globalExecutionContinue or not.
        boolean wrapExpansionPluginInGlobalExecutionContinue = false;

        //TODO: mable builder

        StatementMaintainer stmMaintainer = new StatementMaintainer();
        stmMaintainer.add(createGlobalExecutionContinue());
        stmMaintainer.addAll(createStatusVariables());

        stmMaintainer.addAll(generateLoadUnloadStms(MaBLTemplateGenerator::createLoadStatement));

        Fmi2SimulationEnvironment unitRelationShip = templateConfiguration.getUnitRelationship();
        boolean faultInject =
                unitRelationShip.getInstances().stream().anyMatch(x -> x.getValue() != null && x.getValue().getFaultInject().isPresent());
        if (faultInject) {
            stmMaintainer.add(createLoadStatement(FAULT_INJECT_MODULE_NAME,
                    Arrays.asList(newAStringLiteralExp(unitRelationShip.getFaultInjectionConfigurationPath()))));
        }

        // Create FMU load statements
        List<PStm> unloadFmuStatements = new ArrayList<>();
        HashMap<String, String> fmuNameToLexIdentifier = new HashMap<>();
        for (Map.Entry<String, Fmi2ModelDescription> entry : unitRelationShip.getFmusWithModelDescriptions()) {
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
        List<PStm> terminateStatements = new ArrayList<>();
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
                    templateConfiguration.getLoggingOn(), entry.getValue().getFaultInject()));

            terminateStatements.add(createFMUTerminateStatement(instanceLexName, entry.getValue().getFaultInject()));
            freeInstanceStatements.add(createFMUFreeInstanceStatement(instanceLexName, parentLex, entry.getValue().getFaultInject()));
        });


        // Debug logging
        if (templateConfiguration.getLoggingOn()) {
            //            if (templateConfiguration.getLogLevels() != null) {
            stmMaintainer.addAll(createDebugLoggingStms(instaceNameToInstanceLex, templateConfiguration.getLogLevels()));
            stmMaintainer.wrapInIfBlock();
            //            }
        }


        // Components Array
        stmMaintainer.add(createComponentsArray(COMPONENTS_ARRAY_NAME, instanceLexToInstanceName.keySet()));

        // Generate the jacobian step algorithm expand statement. i.e. fixedStep or variableStep and variable statement for step-size.
        if (templateConfiguration.getStepAlgorithmConfig() == null) {
            throw new RuntimeException("No step algorithm config found");
        }
        JacobianStepConfig jacobianStepConfig = (JacobianStepConfig) templateConfiguration.getStepAlgorithmConfig();
        ExpandStatements algorithmStatements = generateAlgorithmStms(jacobianStepConfig.stepAlgorithm);
        if (algorithmStatements.variablesToTopOfMabl != null) {
            stmMaintainer.addAll(algorithmStatements.variablesToTopOfMabl);
        }

        // add variable statements for start time and end time.
        stmMaintainer.add(createRealVariable(START_TIME_NAME, jacobianStepConfig.startTime));
        stmMaintainer.add(createRealVariable(END_TIME_NAME, jacobianStepConfig.endTime));

        // Add the initializer expand stm
        if (templateConfiguration.getInitialize().getKey()) {
            if (templateConfiguration.getInitialize().getValue() != null) {
                stmMaintainer.add(new AConfigStm(StringEscapeUtils.escapeJava(templateConfiguration.getInitialize().getValue())));
            }

            stmMaintainer.add(createExpandInitialize(COMPONENTS_ARRAY_NAME, START_TIME_NAME, END_TIME_NAME));
        }

        // Add the algorithm expand stm
        if (algorithmStatements.body != null) {
            if (templateConfiguration.getStepAlgorithmConfig() != null) {
                stmMaintainer.add(new AConfigStm(
                        StringEscapeUtils.escapeJava(objectMapper.writeValueAsString(templateConfiguration.getStepAlgorithmConfig()))));
            }
            stmMaintainer.addAll(algorithmStatements.body);
        }

        // Terminate instances
        stmMaintainer.addAllCleanup(terminateStatements);

        // Free instances
        stmMaintainer.addAllCleanup(freeInstanceStatements);

        // Unload the FMUs
        stmMaintainer.addAllCleanup(unloadFmuStatements);
        stmMaintainer.addAllCleanup(generateLoadUnloadStms(x -> createUnloadStatement(StringUtils.uncapitalize(x))));
        if (faultInject) {
            stmMaintainer.addAllCleanup(Arrays.asList(createUnloadStatement(FAULT_INJECT_MODULE_VARIABLE_NAME)));
        }
        // Create the toplevel
        List<LexIdentifier> imports = new ArrayList<>(
                Arrays.asList(newAIdentifier(JACOBIANSTEP_EXPANSION_MODULE_NAME), newAIdentifier(INITIALIZE_EXPANSION_MODULE_NAME),
                        newAIdentifier(DEBUG_LOGGING_MODULE_NAME), newAIdentifier(TYPECONVERTER_MODULE_NAME), newAIdentifier(DATAWRITER_MODULE_NAME),
                        newAIdentifier(FMI2_MODULE_NAME), newAIdentifier(MATH_MODULE_NAME), newAIdentifier(ARRAYUTIL_EXPANSION_MODULE_NAME),
                        newAIdentifier(LOGGER_MODULE_NAME), newAIdentifier(BOOLEANLOGIC_MODULE_NAME), newAIdentifier("MEnv")));
        if (faultInject) {
            imports.add(newAIdentifier(FAULT_INJECT_MODULE_NAME));
        }

        ASimulationSpecificationCompilationUnit unit =
                newASimulationSpecificationCompilationUnit(imports, newABlockStm(stmMaintainer.getStatements()));
        unit.setFramework(Collections.singletonList(new LexIdentifier(templateConfiguration.getFramework().name(), null)));

        unit.setFrameworkConfigs(Arrays.asList(
                new AConfigFramework(new LexIdentifier(templateConfiguration.getFrameworkConfig().getKey().name(), null),
                        StringEscapeUtils.escapeJava(objectMapper.writeValueAsString(templateConfiguration.getFrameworkConfig().getValue())))));
        return unit;
    }


    public static Collection<? extends PStm> createStatusVariables() {
        List<PStm> list = new ArrayList<>();
        BiFunction<String, Integer, PStm> createStatusVariable_ = (name, value) -> newALocalVariableStm(
                newAVariableDeclaration(newLexIdentifier(name), newAIntNumericPrimitiveType(), newAExpInitializer(newAIntLiteralExp(value))));
        list.add(createStatusVariable_.apply("FMI_STATUS_OK", 0));
        list.add(createStatusVariable_.apply("FMI_STATUS_WARNING", 1));
        list.add(createStatusVariable_.apply("FMI_STATUS_DISCARD", 2));
        list.add(createStatusVariable_.apply("FMI_STATUS_ERROR", 3));
        list.add(createStatusVariable_.apply("FMI_STATUS_FATAL", 4));
        list.add(createStatusVariable_.apply("FMI_STATUS_PENDING", 5));
        list.add(MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(MableAstFactory.newAIdentifier(STATUS), MableAstFactory.newAIntNumericPrimitiveType(),
                        MableAstFactory.newAExpInitializer(MableAstFactory.newAIntLiteralExp(0)))));
        return list;
    }


    private static PStm checkNullAndStop(String identifier) {
        return newIf(newEqual(newAIdentifierExp(identifier), newNullExp()),
                newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier(GLOBAL_EXECUTION_CONTINUE)), newABoolLiteralExp(false)), null);
    }

    private static Collection<? extends PStm> createDebugLoggingStmsHelper(Map<String, String> instaceNameToInstanceLex, String instanceName,
            List<String> logLevels) {
        String instanceLexName = instaceNameToInstanceLex.get(instanceName);
        if (instanceLexName != null) {
            return createExpandDebugLogging(instanceLexName, logLevels);
        } else {
            logger.warn("Could not set log levels for " + instanceName);
            return Arrays.asList();
        }

    }

    private static Collection<? extends PStm> createDebugLoggingStms(Map<String, String> instaceNameToInstanceLex,
            Map<String, List<String>> logLevels) {
        List<PStm> stms = new ArrayList<>();

        // If no logLevels have defined, then call setDebugLogging for all instances
        if (logLevels == null) {
            for (Map.Entry<String, String> entry : instaceNameToInstanceLex.entrySet()) {
                stms.addAll(createDebugLoggingStmsHelper(instaceNameToInstanceLex, entry.getKey(), new ArrayList<>()));
            }
        } else {
            // If loglevels have been defined for some instances, then only call setDebugLogging for those instances.
            for (Map.Entry<String, List<String>> entry : logLevels.entrySet()) {
                // If the instance is available as key in loglevels but has an empty value, then call setDebugLogging with empty loglevels.
                if (entry.getValue().isEmpty()) {
                    stms.addAll(createDebugLoggingStmsHelper(instaceNameToInstanceLex, entry.getKey(), new ArrayList<>()));
                    continue;
                }
                // If the instance is available as key in loglevels and has nonempty value, then call setDebugLogging with the relevant values.
                stms.addAll(createDebugLoggingStmsHelper(instaceNameToInstanceLex, entry.getKey(), entry.getValue()));
            }
        }

        return stms;
    }

    private static List<PStm> createExpandDebugLogging(String instanceLexName, List<String> logLevels) {
        AArrayInitializer loglevelsArrayInitializer = null;
        String arrayName = instanceLexName + LOGLEVELS_POSTFIX;
        if (!logLevels.isEmpty()) {
            loglevelsArrayInitializer =
                    newAArrayInitializer(logLevels.stream().map(MableAstFactory::newAStringLiteralExp).collect(Collectors.toList()));
        }
        ALocalVariableStm arrayContent = MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(MableAstFactory.newAIdentifier(arrayName),
                        MableAstFactory.newAArrayType(MableAstFactory.newAStringPrimitiveType()), logLevels.size(), loglevelsArrayInitializer));

        AExpressionStm expandCall = MableAstFactory.newExpressionStm(MableAstFactory
                .newACallExp(newExpandToken(), newAIdentifierExp(MableAstFactory.newAIdentifier(DEBUG_LOGGING_MODULE_NAME)),
                        MableAstFactory.newAIdentifier(DEBUG_LOGGING_EXPANSION_FUNCTION_NAME),
                        Arrays.asList(MableAstFactory.newAIdentifierExp(instanceLexName), MableAstFactory.newAIdentifierExp(arrayName),
                                MableAstFactory.newAUIntLiteralExp(Long.valueOf(logLevels.size())))));

        return Arrays.asList(arrayContent, expandCall);

    }

    private static PStm createGlobalExecutionContinue() {
        return MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(MableAstFactory.newAIdentifier(GLOBAL_EXECUTION_CONTINUE), MableAstFactory.newABoleanPrimitiveType(),
                        MableAstFactory.newAExpInitializer(MableAstFactory.newABoolLiteralExp(true))));
    }

    private static PStm createFMUTerminateStatement(String instanceLexName, Optional<FaultInject> faultInject) {
        if (faultInject.isPresent()) {
            instanceLexName = instanceLexName + "_original";
        }
        return MableAstFactory.newExpressionStm(MableAstFactory
                .newACallExp(MableAstFactory.newAIdentifierExp(instanceLexName), MableAstFactory.newAIdentifier("terminate"), Arrays.asList()));
    }

    private static PStm createFMUFreeInstanceStatement(String instanceLexName, String fmuLexName, Optional<FaultInject> faultInject) {
        if (faultInject.isPresent()) {
            instanceLexName = instanceLexName + "_original";
        }
        return MableAstFactory.newExpressionStm(MableAstFactory
                .newACallExp(MableAstFactory.newAIdentifierExp(fmuLexName), MableAstFactory.newAIdentifier("freeInstance"),
                        Arrays.asList(MableAstFactory.newAIdentifierExp(instanceLexName))));
    }

    private static Collection<? extends PStm> generateUnloadStms() {
        return null;
    }

    private static PStm createComponentsArray(String lexName, Set<String> keySet) {
        return MableAstFactory.newALocalVariableStm(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier(lexName),
                MableAstFactory.newAArrayType(MableAstFactory.newANameType(FMI2COMPONENT_TYPE)), keySet.size(),
                MableAstFactory.newAArrayInitializer(keySet.stream().map(x -> aIdentifierExpFromString(x)).collect(Collectors.toList()))));
    }

    private static PStm createUnloadStatement(String moduleName) {
        return MableAstFactory.newExpressionStm(MableAstFactory.newUnloadExp(Arrays.asList(MableAstFactory.newAIdentifierExp(moduleName))));
    }

    private static Collection<? extends PStm> generateLoadUnloadStms(Function<String, PStm> function) {
        return Arrays.asList(MATH_MODULE_NAME, LOGGER_MODULE_NAME, DATAWRITER_MODULE_NAME, BOOLEANLOGIC_MODULE_NAME).stream()
                .map(x -> function.apply(x)).collect(Collectors.toList());
    }

    private static PStm createLoadStatement(String moduleName, List<PExp> pexp) {
        List<PExp> arguments = new ArrayList<>();
        arguments.add(MableAstFactory.newAStringLiteralExp(moduleName));
        if (pexp != null && pexp.size() > 0) {
            arguments.addAll(pexp);
        }
        return MableAstFactory.newALocalVariableStm(MableAstFactory
                .newAVariableDeclaration(MableAstFactory.newAIdentifier(StringUtils.uncapitalize(moduleName)),
                        MableAstFactory.newANameType(moduleName), MableAstFactory.newAExpInitializer(MableAstFactory.newALoadExp(arguments))));
    }

    private static PStm createLoadStatement(String moduleName) {
        return createLoadStatement(moduleName, null);
    }

    public static PStm createExpandInitialize(String componentsArrayLexName, String startTimeLexName, String endTimeLexName) {
        return MableAstFactory.newExpressionStm(MableAstFactory
                .newACallExp(newExpandToken(), newAIdentifierExp(MableAstFactory.newAIdentifier(INITIALIZE_EXPANSION_MODULE_NAME)),
                        MableAstFactory.newAIdentifier(INITIALIZE_EXPANSION_FUNCTION_NAME),
                        Arrays.asList(aIdentifierExpFromString(componentsArrayLexName), aIdentifierExpFromString(startTimeLexName),
                                aIdentifierExpFromString(endTimeLexName))));
    }

    public static AIdentifierExp aIdentifierExpFromString(String x) {
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
