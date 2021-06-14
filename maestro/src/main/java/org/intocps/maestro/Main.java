package org.intocps.maestro;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.cli.MaestroV1SimulationConfiguration;
import org.intocps.maestro.codegen.mabl2cpp.MablCppCodeGenerator;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.api.FixedStepAlgorithm;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.template.MaBLTemplateConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    static final Predicate<File> mableFileFilter = f -> f.getName().toLowerCase().endsWith(".mabl");
    static final Predicate<File> jsonFileFilter = f -> f.getName().toLowerCase().endsWith(".json");
    final static Logger logger = LoggerFactory.getLogger(Main.class);

    private static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("maestro", options);
    }

    public static String getVersion() {
        try {
            Properties prop = new Properties();
            InputStream coeProp = Main.class.getResourceAsStream("maestro.properties");
            prop.load(coeProp);
            return prop.getProperty("version");
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean argumentHandler(String[] args) throws Exception {
        Option helpOpt = Option.builder("h").longOpt("help").desc("Show this description").build();
        Option verboseOpt = Option.builder("v").longOpt("verbose").desc("Verbose").build();
        Option versionOpt = Option.builder("version").longOpt("version").desc("Version").build();
        Option generateSpecificationV1 =
                Option.builder("sg1").longOpt("spec-generate1").desc("Generate a Mabl specification from a Maestro V1 configuration").build();
        //        Option contextOpt = Option.builder("c").longOpt("config").desc("path to a plugin config JSON file").build();
        //        Option mablOpt =
        //                Option.builder("m").longOpt("mabl").desc("Path to Mabl files").hasArg().valueSeparator(' ').argName("path").required().build();
        Option interpretOpt = Option.builder("i").longOpt("interpret").desc("Interpret specification").build();
        Option dumpLocation = Option.builder("ds").longOpt("dump-simple").hasArg(true).argName("path")
                .desc("Path to a directory where the spec and runtime data will be " + "dumped").build();
        Option dumpIntermediateSpecs = Option.builder("di").longOpt("dump-intermediate").hasArg(true).argName("path")
                .desc("Path to a directory " + "where the intermediate specs will be dumped during expansion").build();
        Option expansionLimit = Option.builder("el").longOpt("expand-limit")
                .desc("Set the expansion limit. E.g. stop after X expansions, default is 0 i.e. no " + "expansion").hasArg(true).argName("limit")
                .build();

        Option verifyOpt = Option.builder("vi").longOpt("verify").hasArg(true).argName("framework")
                .desc("The framework to use for verification: " + Arrays.stream(Framework.values()).map(Enum::name).collect(Collectors.joining(", ")))
                .build();

        Option cgOpt =
                Option.builder("cg").longOpt("codegen").hasArg(true).argName("name").desc("One of the code generators supported: [cpp]").build();

        Option optimizationDisableOpt = Option.builder("nop").longOpt("disable-optimize").desc("Disable optimization").build();

        Options options = new Options();
        options.addOption(helpOpt);
        options.addOption(verboseOpt);
        options.addOption(versionOpt);
        options.addOption(generateSpecificationV1);
        options.addOption(interpretOpt);
        options.addOption(dumpLocation);
        options.addOption(dumpIntermediateSpecs);
        options.addOption(expansionLimit);
        options.addOption(verifyOpt);
        options.addOption(cgOpt);
        options.addOption(optimizationDisableOpt);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e1) {
            System.err.println("Parsing failed. Reason: " + e1.getMessage());
            showHelp(options);
            return false;
        }

        if (cmd.hasOption(helpOpt.getOpt())) {
            showHelp(options);
            return false;
        }

        if (cmd.hasOption(versionOpt.getOpt())) {
            System.out.println(getVersion());
            return false;
        }


        boolean verbose = cmd.hasOption(verboseOpt.getOpt());

        List<File> sourceFiles = cmd.getArgList().stream().map(File::new).collect(Collectors.toList());

        sourceFiles = Stream.concat(sourceFiles.stream().filter(File::isDirectory)
                        .flatMap(f -> Arrays.stream(f.listFiles(pathname -> mableFileFilter.test(pathname) || jsonFileFilter.test(pathname)))),
                sourceFiles.stream().filter(File::isFile)).collect(Collectors.toList());


        IErrorReporter reporter = new ErrorReporter();

        //used to file lookup
        File specificationDirectory = new File(".");
        File workingDirectory = new File(".");

        Mabl mabl = new Mabl(specificationDirectory,
                cmd.hasOption(dumpIntermediateSpecs.getOpt()) ? new File(cmd.getOptionValue(dumpIntermediateSpecs.getOpt())) : null);
        mabl.setReporter(reporter);
        mabl.setVerbose(verbose);
        mabl.getSettings().dumpIntermediateSpecs = cmd.hasOption(dumpIntermediateSpecs.getOpt());

        if (cmd.hasOption(generateSpecificationV1.getOpt())) {

            List<File> files = sourceFiles.stream().filter(jsonFileFilter).collect(Collectors.toList());

            if (!files.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                MaestroV1SimulationConfiguration config = new MaestroV1SimulationConfiguration();

                for (File jsonFile : files) {
                    ObjectReader updater = mapper.readerForUpdating(config);
                    config = updater.readValue(jsonFile);
                }
                try {
                    MaBLTemplateConfiguration templateConfig = generateTemplateSpecificationFromV1(config, reporter);
                    mabl.generateSpec(templateConfig);
                    mabl.setRuntimeEnvironmentVariables(config.parameters);
                } finally {


                    if (hasErrorAndPrintErrorsAndWarnings(verbose, reporter)) {
                        return false;
                    }
                }

            } else {
                System.err.println("Missing configuration file for " + generateSpecificationV1.getLongOpt() + ". Please specify a json file.");
                return false;
            }
        }

        sourceFiles = sourceFiles.stream().filter(mableFileFilter).collect(Collectors.toList());

        if (!sourceFiles.isEmpty()) {
            mabl.parse(sourceFiles);
        } else if (!cmd.hasOption(generateSpecificationV1.getOpt())) {
            System.err.println("Insufficient input data given: Either pass MaBL specification files or other arguments:");
            showHelp(options);
            return false;
        }

        if (!cmd.hasOption(expansionLimit.getOpt())) {
            mabl.expand();
        } else {
            System.out.println("Specific expansion limits not implemented. So not expanding");
        }


        if (hasErrorAndPrintErrorsAndWarnings(verbose, reporter)) {
            return false;
        }

        if (cmd.hasOption(dumpLocation.getOpt())) {
            mabl.dump(new File(cmd.getOptionValue(dumpLocation.getOpt())));
            workingDirectory = new File(cmd.getOptionValue(dumpLocation.getOpt()));
        }

        if (!cmd.hasOption(optimizationDisableOpt.getOpt())) {
            mabl.optimize();
        }

        Map.Entry<Boolean, Map<INode, PType>> typeCheckResult = mabl.typeCheck();
        if (!typeCheckResult.getKey()) {
            if (reporter.getErrorCount() > 0) {
                return !hasErrorAndPrintErrorsAndWarnings(verbose, reporter);
            }
            return false;
        }
        if (cmd.hasOption(verifyOpt.getOpt())) {
            Framework framework = Framework.valueOf(cmd.getOptionValue(verifyOpt.getOpt()));

            if (!mabl.verify(framework)) {
                return !hasErrorAndPrintErrorsAndWarnings(verbose, reporter);
            }

            // verify can be true but there can still be warnings.
            hasErrorAndPrintErrorsAndWarnings(verbose, reporter);

        }

        if (cmd.hasOption(cgOpt.getOpt())) {
            if (cmd.getOptionValue(cgOpt.getOpt()).equals("cpp")) {
                new MablCppCodeGenerator(new File(".")).generate(mabl.getMainSimulationUnit(), typeCheckResult.getValue());
            }
        }


        if (cmd.hasOption(interpretOpt.getOpt()) && !cmd.hasOption(expansionLimit.getOpt())) {
            new MableInterpreter(new DefaultExternalValueFactory(workingDirectory,
                    IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8))).execute(mabl.getMainSimulationUnit());
        }
        return true;
    }

    /**
     * Returns true if there are any errors
     *
     * @param verbose
     * @param reporter
     * @return
     */
    private static boolean hasErrorAndPrintErrorsAndWarnings(boolean verbose, IErrorReporter reporter) {
        if (reporter.getWarningCount() > 0) {
            if (verbose) {
                reporter.printWarnings(new PrintWriter(System.out, true));
            }
        }
        if (reporter.getErrorCount() > 0) {
            reporter.printErrors(new PrintWriter(System.err, true));
            return true;
        }

        return false;
    }

    public static void main(String[] args) throws Exception {
        int exitCode = 0;
        if (!argumentHandler(args)) {
            exitCode = 1;
        }
        System.exit(exitCode);
    }

    private static MaBLTemplateConfiguration generateTemplateSpecificationFromV1(MaestroV1SimulationConfiguration simulationConfiguration,
            IErrorReporter reporter) throws Exception {
        Fmi2SimulationEnvironment simulationEnvironment = Fmi2SimulationEnvironment.of(simulationConfiguration, reporter);
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
}



