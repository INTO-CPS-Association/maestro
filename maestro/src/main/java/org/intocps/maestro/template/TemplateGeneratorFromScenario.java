package org.intocps.maestro.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import core.MasterModel;
import core.ScenarioLoader;
import org.apache.commons.text.StringEscapeUtils;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.intocps.maestro.plugin.ScenarioVerifier;
import org.intocps.maestro.plugin.ScenarioVerifierConfig;
import scala.jdk.javaapi.CollectionConverters;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.template.MaBLTemplateGenerator.removeFmuKeyBraces;

public class TemplateGeneratorFromScenario {
    private static final String START_TIME_NAME = "start_time";
    private static final String END_TIME_NAME = "end_time";
    private static final String STEP_SIZE_NAME = "step_size";
    private static final String SCENARIO_MODEL_FMU_INSTANCE_DELIMITER = "_";
    private static final String SCENARIOVERIFIER_EXPANSION_MODULE_NAME = "ScenarioVerifier";
    private static final String FRAMEWORK_MODULE_NAME = "FMI2";
    private static final String COMPONENTS_ARRAY_NAME = "components";

    public static ASimulationSpecificationCompilationUnit generateTemplate(ScenarioConfiguration configuration) throws Exception {
        //TODO: A Scenario and a multi-model does not agree on the format of identifying a FMU/instance.
        // E.g: FMU in a multi-model is defined as: "{<fmu-name>}" where in a scenario no curly braces are used i.e. "<fmu-name>". Furthermore an
        // instance in a multi-model is uniquely identified as: "{<fmu-name>}.<instance-name>" where instances are not really considered in scenarios
        // but is currently expected to be expressed as: "<fmu-name>_<instance_name>".
        // This is not optimal and should be changed to the same format.

        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(configuration.getMasterModel().getBytes()));

        // Generate MaBL spec
        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        //TODO: Error handling on or off -> settings flag?
        settings.fmiErrorHandlingEnabled = false;
        MablApiBuilder builder = new MablApiBuilder(settings, false);
        DynamicActiveBuilderScope dynamicScope = builder.getDynamicScope();

        List<FmuVariableFmi2Api> fmus = configuration.getSimulationEnvironment().getFmusWithModelDescriptions().stream()
                .filter(entry -> configuration.getSimulationEnvironment().getUriFromFMUName(entry.getKey()) != null).map(entry -> {
                    try {
                        return dynamicScope.createFMU(removeFmuKeyBraces(entry.getKey()), entry.getValue(),
                                configuration.getSimulationEnvironment().getUriFromFMUName(entry.getKey()));
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to create FMU variable: " + e);
                    }
                }).collect(Collectors.toList());

        // Generate fmu instances with identifying names from the mater model.
        Map<String, ComponentVariableFmi2Api> fmuInstances =
                CollectionConverters.asJava(masterModel.scenario().fmus()).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    Optional<FmuVariableFmi2Api> fmuFromScenario = fmus.stream().filter(fmu -> fmu.getName()
                            .contains(entry.getKey().split(SCENARIO_MODEL_FMU_INSTANCE_DELIMITER)[0].toLowerCase(Locale.ROOT))).findAny();
                    if (fmuFromScenario.isEmpty()) {
                        throw new RuntimeException("Unable to match fmu from multi model with fmu from master model");
                    }
                    return fmuFromScenario.get().instantiate(entry.getKey().split(ScenarioVerifier.MASTER_MODEL_FMU_INSTANCE_DELIMITER)[1]);
                }));

        // Store variables to be used by the scenario verifier
        dynamicScope.store(STEP_SIZE_NAME, configuration.getExecutionParameters().getStepSize());
        dynamicScope.store(START_TIME_NAME, configuration.getExecutionParameters().getStartTime());
        dynamicScope.store(END_TIME_NAME, configuration.getExecutionParameters().getEndTime());
        dynamicScope.storeInArray(COMPONENTS_ARRAY_NAME, fmuInstances.values().toArray(new ComponentVariableFmi2Api[0]));

        // Setup and add scenario verifier config
        ScenarioVerifierConfig expansionConfig = new ScenarioVerifierConfig();
        expansionConfig.masterModel = configuration.getMasterModel();
        expansionConfig.parameters = configuration.getParameters();
        expansionConfig.relTol = configuration.getExecutionParameters().getConvergenceRelativeTolerance();
        expansionConfig.absTol = configuration.getExecutionParameters().getConvergenceAbsoluteTolerance();
        expansionConfig.convergenceAttempts = configuration.getExecutionParameters().getConvergenceAttempts();

        AConfigStm configStm = new AConfigStm(StringEscapeUtils.escapeJava((new ObjectMapper()).writeValueAsString(expansionConfig)));
        dynamicScope.add(configStm);

        // Add scenario verifier expansion plugin
        PStm algorithmStm = MableAstFactory.newExpressionStm(MableAstFactory
                .newACallExp(newExpandToken(), newAIdentifierExp(MableAstFactory.newAIdentifier(SCENARIOVERIFIER_EXPANSION_MODULE_NAME)),
                        MableAstFactory.newAIdentifier(ScenarioVerifier.EXECUTEALGORITHM_FUNCTION_NAME),
                        Arrays.asList(AIdentifierExpFromString(COMPONENTS_ARRAY_NAME), AIdentifierExpFromString(STEP_SIZE_NAME),
                                AIdentifierExpFromString(START_TIME_NAME), AIdentifierExpFromString(END_TIME_NAME))));
        dynamicScope.add(algorithmStm);

        // Terminate instances, free instances, unload FMUs
        fmuInstances.values().forEach(ComponentVariableFmi2Api::terminate);
        fmus.forEach(fmu -> {
            fmuInstances.values().stream().filter(instance -> instance.getOwner().getFmuIdentifier().equals(fmu.getFmuIdentifier())).findAny()
                    .ifPresent(fmu::freeInstance);
            fmu.unload(dynamicScope);
        });

        // Build unit
        ASimulationSpecificationCompilationUnit unit = builder.build();

        // Add imports
        unit.setImports(List.of(newAIdentifier(SCENARIOVERIFIER_EXPANSION_MODULE_NAME), newAIdentifier(FRAMEWORK_MODULE_NAME)));

        // Setup framework
        unit.setFramework(Collections.singletonList(new LexIdentifier(configuration.getFrameworkConfig().getLeft().toString(), null)));
        unit.setFrameworkConfigs(Collections.singletonList(new AConfigFramework(new LexIdentifier(configuration.getFrameworkConfig().getKey().toString(),
                null),
                StringEscapeUtils.escapeJava((new ObjectMapper()).writeValueAsString(configuration.getFrameworkConfig().getValue())))));
        PrettyPrinter.print(unit);
        return unit;
    }

    private static AIdentifierExp AIdentifierExpFromString(String x) {
        return MableAstFactory.newAIdentifierExp(MableAstFactory.newAIdentifier(x));
    }
}
