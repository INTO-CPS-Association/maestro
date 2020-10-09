package org.intocps.maestro;

import org.apache.commons.cli.*;
import org.intocps.maestro.ast.ARootDocument;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.interpreter.DataStore;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Main {

    final static Logger logger = LoggerFactory.getLogger(Main.class);

    private static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("coe", options);
    }

    private static String getVersion() {
        try {
            Properties prop = new Properties();
            InputStream coeProp = Main.class.getResourceAsStream("/coe.properties");
            prop.load(coeProp);
            return prop.getProperty("version");
        } catch (Exception e) {
            return "";
        }
    }

    public static void main(String[] args) throws Exception {
        Option helpOpt = Option.builder("h").longOpt("help").desc("Show this description").build();
        Option verboseOpt = Option.builder("v").longOpt("verbose").desc("Verbose").build();
        Option versionOpt = Option.builder("version").longOpt("version").desc("Version").build();
        Option contextOpt = Option.builder("c").longOpt("config").desc("path to a plugin config JSON file").build();
        Option mablOpt =
                Option.builder("m").longOpt("mabl").desc("Path to Mabl files").hasArg().valueSeparator(' ').argName("path").required().build();
        Option frameworkOpt = Option.builder("f").longOpt("framework")
                .desc("Specify simulation framework: " + Arrays.stream(Framework.values()).map(Object::toString).collect(Collectors.joining(", ")))
                .hasArg().type(Framework.class).required().build();
        Option interpretOpt = Option.builder("i").longOpt("interpret").desc("Interpret specification").build();
        Option simulationEnvOpt =
                Option.builder("e").longOpt("env").desc("Path to an env file for the selected framework").hasArg().argName("path").build();

        Options options = new Options();
        options.addOption(helpOpt);
        options.addOption(mablOpt);
        options.addOption(verboseOpt);
        options.addOption(versionOpt);
        options.addOption(contextOpt);
        options.addOption(interpretOpt);
        options.addOption(frameworkOpt);
        options.addOption(simulationEnvOpt);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e1) {
            System.err.println("Parsing failed. Reason: " + e1.getMessage());
            showHelp(options);
            return;
        }

        if (cmd.hasOption(helpOpt.getOpt())) {
            showHelp(options);
            return;
        }

        if (cmd.hasOption(versionOpt.getOpt())) {
            System.out.println(getVersion());
            return;
        }

        Framework framework = Framework.valueOf(cmd.getOptionValue(frameworkOpt.getOpt()));

        boolean verbose = cmd.hasOption(verboseOpt.getOpt());

        List<File> sourceFiles = Arrays.stream(cmd.getOptionValues(mablOpt.getOpt())).map(File::new).collect(Collectors.toList());

        File configFile = null;
        if (cmd.hasOption(contextOpt.getOpt())) {
            configFile = new File(cmd.getOptionValue(contextOpt.getOpt()));
        }

        ISimulationEnvironment simulationEnvironment = null;

        switch (framework) {
            case FMI2:
                if (!cmd.hasOption(simulationEnvOpt.getOpt())) {
                    System.err.println("Missing required argument " + simulationEnvOpt.getLongOpt() + " for framework: " + framework);
                    return;
                }
                IErrorReporter reporter = new ErrorReporter();
                simulationEnvironment = FmiSimulationEnvironment.of(new File(cmd.getOptionValue(simulationEnvOpt.getOpt())), reporter);
                if (reporter.getErrorCount() > 0) {
                    reporter.printErrors(new PrintWriter(System.err));
                    return;
                }
                break;
            case Any:
                break;
        }

        try (InputStream configIs = configFile == null ? null : new FileInputStream(configFile)) {

            ARootDocument spec = new MableSpecificationGenerator(framework, verbose, simulationEnvironment).generate(sourceFiles, configIs);

            if (cmd.hasOption(interpretOpt.getOpt())) {
                DataStore.GetInstance().setSimulationEnvironment(simulationEnvironment);
                new MableInterpreter(new DefaultExternalValueFactory()).execute(spec);
            }
        } catch (AnalysisException e) {
            e.printStackTrace();
        }
    }


}
