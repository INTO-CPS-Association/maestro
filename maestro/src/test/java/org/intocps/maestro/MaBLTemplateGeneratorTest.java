package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.antlr.v4.runtime.CharStreams;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.api.FixedStepAlgorithm;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.plugin.JacobianStepConfig;
import org.intocps.maestro.template.MaBLTemplateConfiguration;
import org.intocps.maestro.template.MaBLTemplateGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MaBLTemplateGeneratorTest {

    @Test
    public void generateSingleWaterTankTemplate() throws Exception {
        final double endTime = 10.0;
        final double stepSize = 0.1;
        File configurationDirectory = Paths.get("src", "test", "resources", "specifications", "full", "initialize_singleWaterTank").toFile();

        Fmi2SimulationEnvironmentConfiguration simulationEnvironmentConfiguration =
                new ObjectMapper().readValue(new File(configurationDirectory, "env.json"), Fmi2SimulationEnvironmentConfiguration.class);

        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder b = new MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder();
        FixedStepAlgorithm stepSizeAlgorithm = new FixedStepAlgorithm(endTime, stepSize, 0.0);

        MaBLTemplateConfiguration mtc = b.useInitializer(true, "{}").setStepAlgorithm(stepSizeAlgorithm).setFramework(Framework.FMI2)
                .setFrameworkConfig(Framework.FMI2, simulationEnvironmentConfiguration).build();


        ASimulationSpecificationCompilationUnit aSimulationSpecificationCompilationUnit = MaBLTemplateGenerator.generateTemplate(mtc);
        System.out.println(PrettyPrinter.print(aSimulationSpecificationCompilationUnit));
    }

    @Test
    public void generateSingleWaterTankTemplateMEnv() throws Exception {
        final double endTime = 10.0;
        final double stepSize = 0.1;
        File configurationDirectory = Paths.get("src", "test", "resources", "specifications", "full", "initialize_singleWaterTank").toFile();

        Fmi2SimulationEnvironmentConfiguration simulationEnvironmentConfiguration =
                new ObjectMapper().readValue(new File(configurationDirectory, "env.json"), Fmi2SimulationEnvironmentConfiguration.class);

        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder b = new MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder();
        FixedStepAlgorithm stepSizeAlgorithm = new FixedStepAlgorithm(endTime, stepSize, 0.0);

        Map<String, Object> config = new HashMap<>();
        config.put("environmentParameters", Arrays.asList("{x1}.crtlInstance.maxlevel"));

        Map configData = new ObjectMapper().readValue(new File(configurationDirectory, "config.json"), Map.class);
        config.put("parameters", configData.get("parameters"));

        JacobianStepConfig algorithmConfig = new JacobianStepConfig();
        MaBLTemplateConfiguration mtc =
                b.useInitializer(true, new ObjectMapper().writeValueAsString(config)).setStepAlgorithm(stepSizeAlgorithm).setFramework(Framework.FMI2)
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

        new MableInterpreter(
                new DefaultExternalValueFactory(workingDir, IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8)))
                .execute(mabl.getMainSimulationUnit());

    }

    @Test
    public void generateSingleWaterTankFawultInjectTemplate() throws Exception {
        final double endTime = 10.0;
        final double stepSize = 0.1;
        File configurationDirectory = Paths.get("src", "test", "resources", "specifications", "full", "initialize_singleWaterTank").toFile();

        Fmi2SimulationEnvironmentConfiguration simulationEnvironmentConfiguration =
                new ObjectMapper().readValue(new File(configurationDirectory, "env.json"), Fmi2SimulationEnvironmentConfiguration.class);
        simulationEnvironmentConfiguration.faultInjectInstances = Map.of("crtlInstance", "constraintid");

        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder b = new MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder();
        FixedStepAlgorithm stepSizeAlgorithm = new FixedStepAlgorithm(endTime, stepSize, 0.0);

        MaBLTemplateConfiguration mtc = b.useInitializer(true, "{}").setStepAlgorithm(stepSizeAlgorithm).setFramework(Framework.FMI2)
                .setFrameworkConfig(Framework.FMI2, simulationEnvironmentConfiguration).build();


        ASimulationSpecificationCompilationUnit aSimulationSpecificationCompilationUnit = MaBLTemplateGenerator.generateTemplate(mtc);
        System.out.println(PrettyPrinter.print(aSimulationSpecificationCompilationUnit));
    }

}