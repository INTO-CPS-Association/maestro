package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.antlr.v4.runtime.CharStreams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.ast.ABasicBlockStm;
import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptorQuestion;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.AInstanceMappingStm;
import org.intocps.maestro.ast.node.ALocalVariableStm;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.dto.FixedStepAlgorithmConfig;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.ExplicitModelDescription;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.plugin.JacobianStepConfig;
import org.intocps.maestro.template.MaBLTemplateConfiguration;
import org.intocps.maestro.template.MaBLTemplateGenerator;
import org.intocps.maestro.util.MablModuleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static org.intocps.maestro.template.MaBLTemplateGenerator.FAULTINJECT_POSTFIX;

public class MaBLTemplateGeneratorTest {

    /**
     * Fault injects the wtInstance and verifies that an instance mapping statement has been created above the wtInstance_m_fi variable
     *
     * @throws Exception
     */
    @Test
    public void verifyInstanceMappingStm() throws Exception {
        final double endTime = 10.0;
        final double stepSize = 0.1;
        File configurationDirectory = Paths.get("src", "test", "resources", "specifications", "full", "initialize_singleWaterTank").toFile();

        Fmi2SimulationEnvironmentConfiguration simulationEnvironmentConfiguration = Fmi2SimulationEnvironmentConfiguration.createFromJsonString(
                new String(Files.readAllBytes(Paths.get(new File(configurationDirectory, "env.json").getAbsolutePath()))));

        simulationEnvironmentConfiguration.faultInjectInstances = new HashMap<>();
        simulationEnvironmentConfiguration.faultInjectInstances.put("wtInstance", "fi");

        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder b = new MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder();

        JacobianStepConfig algorithmConfig = new JacobianStepConfig();
        algorithmConfig.startTime = 0.0;
        algorithmConfig.endTime = endTime;
        algorithmConfig.stepAlgorithm = new FixedStepAlgorithmConfig(stepSize);

        MaBLTemplateConfiguration mtc = b.useInitializer(true, "{}").setStepAlgorithmConfig(algorithmConfig).setFramework(Framework.FMI2)
                .setFrameworkConfig(Framework.FMI2, simulationEnvironmentConfiguration).build();

        AtomicBoolean foundCorrectMapping = new AtomicBoolean(false);
        ASimulationSpecificationCompilationUnit aSimulationSpecificationCompilationUnit = MaBLTemplateGenerator.generateTemplate(mtc);
        // Test that the instance mapping is found
        aSimulationSpecificationCompilationUnit.apply(new DepthFirstAnalysisAdaptorQuestion<>() {
            @Override
            public void caseAVariableDeclaration(AVariableDeclaration node, AtomicBoolean foundMapping) throws AnalysisException {
                super.caseAVariableDeclaration(node, foundMapping);
                if (foundMapping.get() == false) {
                    if (node.getName().getText().contains(FAULTINJECT_POSTFIX)) {
                        var parentStm = node.parent(); //This is a ALocalVariableStm
                        assert (parentStm instanceof ALocalVariableStm);
                        var parentBlockUntyped = parentStm.parent(); // This is a ABlockStm
                        assert parentBlockUntyped instanceof ABasicBlockStm;
                        // Find the stateent above
                        ABasicBlockStm parentBlockTyped = (ABasicBlockStm) parentBlockUntyped;
                        var parentStmIndex = parentBlockTyped.getBody().indexOf(parentStm);
                        var instanceMappingStmUntyped = parentBlockTyped.getBody().get(--parentStmIndex);
                        assert instanceMappingStmUntyped instanceof AInstanceMappingStm;
                        var instanceMappingStm = (AInstanceMappingStm) instanceMappingStmUntyped;
                        foundMapping.set(instanceMappingStm.getIdentifier().getText().contains(FAULTINJECT_POSTFIX) &&
                                instanceMappingStm.getName().equalsIgnoreCase("wtinstance"));

                    }
                }
            }
        }, foundCorrectMapping);
        Assertions.assertTrue(foundCorrectMapping.get());
        //        System.out.println(PrettyPrinter.print(aSimulationSpecificationCompilationUnit));

        File workingDir = Paths.get("target", "MaBLTemplateGeneratorTest", "verifyInstanceMappingStm").toFile();
        Mabl mabl = new Mabl(workingDir, workingDir);
        IErrorReporter reporter = new ErrorReporter();
        mabl.setReporter(reporter);
        mabl.setVerbose(true);
        mabl.parse(CharStreams.fromString(PrettyPrinter.print(aSimulationSpecificationCompilationUnit)));
        mabl.parse(CharStreams.fromString(MablModuleProvider.getFaultInjectMabl()));
        mabl.expand();
        mabl.typeCheck();
        mabl.verify(Framework.FMI2);
    }

    @Test
    public void generateSingleWaterTankTemplate() throws Exception {
        final double endTime = 10.0;
        final double stepSize = 0.1;
        File configurationDirectory = Paths.get("src", "test", "resources", "specifications", "full", "initialize_singleWaterTank").toFile();

        Fmi2SimulationEnvironmentConfiguration simulationEnvironmentConfiguration = Fmi2SimulationEnvironmentConfiguration.createFromJsonString(
                new String(Files.readAllBytes(Paths.get(new File(configurationDirectory, "env.json").getAbsolutePath()))));

        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder b = new MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder();

        JacobianStepConfig algorithmConfig = new JacobianStepConfig();
        algorithmConfig.startTime = 0.0;
        algorithmConfig.endTime = endTime;
        algorithmConfig.stepAlgorithm = new FixedStepAlgorithmConfig(stepSize);

        MaBLTemplateConfiguration mtc = b.useInitializer(true, "{}").setStepAlgorithmConfig(algorithmConfig).setFramework(Framework.FMI2)
                .setFrameworkConfig(Framework.FMI2, simulationEnvironmentConfiguration).build();


        ASimulationSpecificationCompilationUnit aSimulationSpecificationCompilationUnit = MaBLTemplateGenerator.generateTemplate(mtc);
        System.out.println(PrettyPrinter.print(aSimulationSpecificationCompilationUnit));
    }

    @Test
    public void unitCompatibleTestCheckNegativeTemplate() {

        BiFunction<Map<String, String>, Map<String, String>, Boolean> check = (levelUnitMapping, levelDeclaredTypeMapping) -> {
            boolean success = true;
            try {
                checkUnitTest(levelUnitMapping, levelDeclaredTypeMapping);
            } catch (Exception e) {
                success = false;
            }
            return success;
        };

        if (check.apply(new HashMap<>() {{
            put("{x1}", "length");
            put("{x2}", "bar");
        }}, new HashMap<>())) {
            Assertions.fail("Missing error for invalid unit combination");
        }

        if (check.apply(new HashMap<>() {{
            put("{x1}", "length");
        }}, new HashMap<>() {{

            put("{x2}", "My.Bar");
        }})) {
            Assertions.fail("Missing error for invalid unit combination");
        }
    }

    @Test
    public void unitCompatibleTestCheckPositiveTemplate() throws Exception {
        checkUnitTest(new HashMap<>() {{
            put("{x1}", "length");
            put("{x2}", "length");
        }}, new HashMap<>());

        checkUnitTest(new HashMap<>() {{
            put("{x1}", "length");
            put("{x2}", "");
        }}, new HashMap<>());


        checkUnitTest(new HashMap<>() {{
            put("{x1}", "length");
            put("{x2}", "");
        }}, new HashMap<>() {{
            put("{x2}", "My.Length");
        }});
    }

    private void checkUnitTest(Map<String, String> levelUnitMapping, Map<String, String> levelDeclaredTypeMapping) throws Exception {
        final double endTime = 10.0;
        final double stepSize = 0.1;
        File configurationDirectory = Paths.get("src", "test", "resources", "specifications", "full", "initialize_singleWaterTank").toFile();

        Fmi2SimulationEnvironmentConfiguration simulationEnvironmentConfiguration = Fmi2SimulationEnvironmentConfiguration.createFromJsonString(
                new String(Files.readAllBytes(Paths.get(new File(configurationDirectory, "env.json").getAbsolutePath()))));

        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder b = new MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder();

        JacobianStepConfig algorithmConfig = new JacobianStepConfig();
        algorithmConfig.startTime = 0.0;
        algorithmConfig.endTime = endTime;
        algorithmConfig.stepAlgorithm = new FixedStepAlgorithmConfig(stepSize);

        //we keep the connections as is but use two different model descriptions with unit definitions

        MaBLTemplateConfiguration mtc = b.useInitializer(true, "{}").setStepAlgorithmConfig(algorithmConfig).setFramework(Framework.FMI2)
                .setFrameworkConfig(Framework.FMI2, simulationEnvironmentConfiguration,
                        Fmi2SimulationEnvironment.of(simulationEnvironmentConfiguration, new IErrorReporter.SilentReporter(), (key, uri) -> {
                            try {

                                Path path = key.equals("{x1}") ? Paths.get("src", "test", "resources", "fmi2_unit", "watertankcontroller-c",
                                        "modelDescription" + ".xml") : Paths.get("src", "test", "resources", "fmi2_unit", "singlewatertank-20sim",
                                        "modelDescription" + ".xml");
                                byte[] buf = FileUtils.readFileToByteArray(path.toFile());
                                String md = new String(buf, StandardCharsets.UTF_8);

                                md = md.replace("REPLACE_UNIT", levelUnitMapping.getOrDefault(key, ""))
                                        .replace("REPLACE_DECLARED_TYPE", levelDeclaredTypeMapping.getOrDefault(key, ""));


                                return new ExplicitModelDescription(new ByteArrayInputStream(md.getBytes(StandardCharsets.UTF_8)));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        })).build();


        ASimulationSpecificationCompilationUnit aSimulationSpecificationCompilationUnit = MaBLTemplateGenerator.generateTemplate(mtc);
        System.out.println(PrettyPrinter.print(aSimulationSpecificationCompilationUnit));
    }

    @Test
    public void generateSingleWaterTankTemplateMEnv() throws Exception {
        final double endTime = 10.0;
        final double stepSize = 0.1;
        File configurationDirectory = Paths.get("src", "test", "resources", "specifications", "full", "initialize_singleWaterTank").toFile();

        Fmi2SimulationEnvironmentConfiguration simulationEnvironmentConfiguration = Fmi2SimulationEnvironmentConfiguration.createFromJsonString(
                new String(Files.readAllBytes(Paths.get(new File(configurationDirectory, "env.json").getAbsolutePath()))));


        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder b = new MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder();

        Map<String, Object> config = new HashMap<>();
        config.put("environmentParameters", Arrays.asList("{x1}.crtlInstance.maxlevel"));

        Map configData = new ObjectMapper().readValue(new File(configurationDirectory, "config.json"), Map.class);
        config.put("parameters", configData.get("parameters"));

        JacobianStepConfig algorithmConfig = new JacobianStepConfig();
        algorithmConfig.startTime = 0.0;
        algorithmConfig.endTime = endTime;
        algorithmConfig.stepAlgorithm = new FixedStepAlgorithmConfig(stepSize);
        MaBLTemplateConfiguration mtc = b.useInitializer(true, new ObjectMapper().writeValueAsString(config)).setFramework(Framework.FMI2)
                .setFrameworkConfig(Framework.FMI2, simulationEnvironmentConfiguration).setStepAlgorithmConfig(algorithmConfig).build();


        ASimulationSpecificationCompilationUnit aSimulationSpecificationCompilationUnit = MaBLTemplateGenerator.generateTemplate(mtc);
        System.out.println(PrettyPrinter.print(aSimulationSpecificationCompilationUnit));


        IErrorReporter reporter = new ErrorReporter();
        File workingDir = Paths.get("target", "MaBLTemplateGeneratorTest", "GenerateSingleWaterTankTemplateMEnv").toFile();
        Mabl mabl = new Mabl(workingDir, workingDir);
        mabl.setReporter(reporter);
        mabl.setVerbose(true);
        mabl.parse(CharStreams.fromString(PrettyPrinter.print(aSimulationSpecificationCompilationUnit)));
        mabl.expand();
        mabl.typeCheck();
        mabl.verify(Framework.FMI2);
        mabl.setRuntimeEnvironmentVariables((Map<String, Object>) configData.get("parameters"));
        mabl.dump(workingDir);


        if (reporter.getErrorCount() > 0) {
            reporter.printErrors(new PrintWriter(System.err, true));
            Assertions.fail();
        }
        if (reporter.getWarningCount() > 0) {
            reporter.printWarnings(new PrintWriter(System.out, true));
        }

        new MableInterpreter(new DefaultExternalValueFactory(workingDir,
                IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8))).execute(mabl.getMainSimulationUnit());

    }

    @Test
    public void generateSingleWaterTankFawultInjectTemplate() throws Exception {
        final double endTime = 10.0;
        final double stepSize = 0.1;
        File configurationDirectory = Paths.get("src", "test", "resources", "specifications", "full", "initialize_singleWaterTank").toFile();
        File faultInjectionFile =
                Paths.get("src", "test", "resources", "org", "into-cps", "maestro", "faultinjection", "dummyfaultinjectfile.xml").toFile();
        Fmi2SimulationEnvironmentConfiguration simulationEnvironmentConfiguration = Fmi2SimulationEnvironmentConfiguration.createFromJsonString(
                new String(Files.readAllBytes(Paths.get(new File(configurationDirectory, "env.json").getAbsolutePath()))));
        simulationEnvironmentConfiguration.faultInjectInstances = Map.of("crtlInstance", "constraintid");
        simulationEnvironmentConfiguration.faultInjectConfigurationPath = faultInjectionFile.toString();

        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder b = new MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder();

        JacobianStepConfig algorithmConfig = new JacobianStepConfig();
        algorithmConfig.startTime = 0.0;
        algorithmConfig.endTime = endTime;
        algorithmConfig.stepAlgorithm = new FixedStepAlgorithmConfig(stepSize);

        MaBLTemplateConfiguration mtc = b.useInitializer(true, "{}").setStepAlgorithmConfig(algorithmConfig).setFramework(Framework.FMI2)
                .setFrameworkConfig(Framework.FMI2, simulationEnvironmentConfiguration).build();


        ASimulationSpecificationCompilationUnit aSimulationSpecificationCompilationUnit = MaBLTemplateGenerator.generateTemplate(mtc);
        System.out.println(PrettyPrinter.print(aSimulationSpecificationCompilationUnit));
    }
}