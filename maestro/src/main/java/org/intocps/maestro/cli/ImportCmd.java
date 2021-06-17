package org.intocps.maestro.cli;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.intocps.maestro.Mabl;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.api.FixedStepAlgorithm;
import org.intocps.maestro.template.MaBLTemplateConfiguration;
import picocli.CommandLine;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(name = "import", description = "Created a specification from various import types. Remember to place all \" +\n" +
        "        \"necessary plugin extensions in the classpath", mixinStandardHelpOptions = true)
public class ImportCmd implements Callable<Integer> {
    static final Predicate<File> jsonFileFilter = f -> f.getName().toLowerCase().endsWith(".json");
    @CommandLine.Parameters(index = "0", description = "The valid import formats: ${COMPLETION-CANDIDATES}")
    ImportType type;
    @CommandLine.Option(names = {"-di", "--dump-intermediate"}, description = "Dump all intermediate expansions", negatable = true)
    boolean dumpIntermediate;

    //    @CommandLine.Option(names = {"-el", "--expansion-limit"}, description = "Stop expansion after this amount of loops")
    //    int expansionLimit;
    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose")
    boolean verbose;
    @CommandLine.Option(names = {"-vi", "--verify"},
            description = "Verify the spec according to the following verifier groups: ${COMPLETION-CANDIDATES}")
    Framework verify;
    @CommandLine.Option(names = {"-nop", "--disable-optimize"}, description = "Disable spec optimization", negatable = true)
    boolean disableOptimize;
    @CommandLine.Option(names = {"-pa", "--preserve-annotations"}, description = "Preserve annotations", negatable = true)
    boolean preserveAnnotations;
    @CommandLine.Option(names = {"-if", "--inline-framework-config"}, description = "Inline all framework configs", negatable = true)

    boolean inlineFrameworkConfig;
    @CommandLine.Option(names = {"-i", "--interpret"}, description = "Interpret spec after import")
    boolean interpret;
    @CommandLine.Parameters(index = "1..*", description = "One or more specification files")
    List<File> files;
    @CommandLine.Option(names = "-output", description = "Path to a directory where the imported spec will be stored")
    File output;
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    private static MaBLTemplateConfiguration generateTemplateSpecificationFromV1(
            MaestroV1SimulationConfiguration simulationConfiguration) throws Exception {
        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder builder = MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder.getBuilder();

        if (simulationConfiguration.logLevels != null) {
            // Loglevels from app consists of {key}.instance: [loglevel1, loglevel2,...] but have to be: instance: [loglevel1, loglevel2,...].
            Map<String, List<String>> removedFMUKeyFromLogLevels = simulationConfiguration.logLevels.entrySet().stream().collect(Collectors
                    .toMap(entry -> MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder.getFmuInstanceFromFmuKeyInstance(entry.getKey()),
                            Map.Entry::getValue));
            builder.setLogLevels(removedFMUKeyFromLogLevels);
        }

        Map<String, Object> initialize = new HashMap<>();
        initialize.put("parameters", simulationConfiguration.parameters);
        initialize.put("environmentParameters", simulationConfiguration.environmentParameters);

        builder.setFrameworkConfig(Framework.FMI2, simulationConfiguration).useInitializer(true, new ObjectMapper().writeValueAsString(initialize))
                .setFramework(Framework.FMI2).setVisible(simulationConfiguration.visible).setLoggingOn(simulationConfiguration.loggingOn)
                .setStepAlgorithm(new FixedStepAlgorithm(simulationConfiguration.endTime,
                        ((MaestroV1SimulationConfiguration.FixedStepAlgorithmConfig) simulationConfiguration.algorithm).getSize(), 0.0));


        return builder.build();
    }

    @Override
    public Integer call() throws Exception {

        Mabl.MableSettings settings = new Mabl.MableSettings();
        settings.dumpIntermediateSpecs = dumpIntermediate;
        settings.preserveFrameworkAnnotations = preserveAnnotations;
        settings.inlineFrameworkConfig = inlineFrameworkConfig;

        MablCliUtil util = new MablCliUtil(output, output, settings);
        util.setVerbose(verbose);

        List<File> sourceFiles = Stream.concat(
                files.stream().filter(File::isDirectory).flatMap(f -> Arrays.stream(Objects.requireNonNull(f.listFiles(jsonFileFilter::test)))),
                files.stream().filter(File::isFile).filter(jsonFileFilter)).collect(Collectors.toList());

        if (type == ImportType.Sg1) {
            if (!importSg1(util, sourceFiles)) {
                return 1;
            }
        } else {
            throw new IllegalStateException("Unexpected value: " + type);
        }

        if (!util.expand()) {
            return 1;
        }

        if (output != null) {
            util.mabl.dump(output);
        }

        if (!disableOptimize) {
            util.mabl.optimize();
        }

        if (output != null) {
            util.mabl.dump(output);
        }

        if (!util.typecheck()) {
            return 1;
        }

        if (verify != null) {
            if (!util.verify(verify)) {
                return 1;
            }
        }

        if (interpret) {
            util.interpret();
        }
        return 0;
    }

    private boolean importSg1(MablCliUtil util, List<File> files) throws Exception {
        if (!files.isEmpty()) {
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            MaestroV1SimulationConfiguration config = new MaestroV1SimulationConfiguration();

            for (File jsonFile : files) {
                ObjectReader updater = mapper.readerForUpdating(config);
                config = updater.readValue(jsonFile);
            }
            MaBLTemplateConfiguration templateConfig = generateTemplateSpecificationFromV1(config);
            util.mabl.generateSpec(templateConfig);
            util.mabl.setRuntimeEnvironmentVariables(config.parameters);

            return !MablCliUtil.hasErrorAndPrintErrorsAndWarnings(util.verbose, util.reporter);

        } else {
            System.err.println("Missing configuration file for " + spec.name() + ". Please specify a json file.");
            return false;
        }
    }

    enum ImportType {
        Sg1
    }
}
