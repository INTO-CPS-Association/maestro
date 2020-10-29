package org.intocps.maestro;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
        //        Option contextOpt = Option.builder("c").longOpt("config").desc("path to a plugin config JSON file").build();
        Option mablOpt =
                Option.builder("m").longOpt("mabl").desc("Path to Mabl files").hasArg().valueSeparator(' ').argName("path").required().build();
        Option interpretOpt = Option.builder("i").longOpt("interpret").desc("Interpret specification").build();
        Option dumpLocation = Option.builder("d").longOpt("dump").desc("Path to a directory where the spec and runtime data will be dumped").build();
        Option dumpIntermediateSpecs = Option.builder("di").longOpt("dump-intermediate").desc("Dump intermediate specs during expansion").build();

        Options options = new Options();
        options.addOption(helpOpt);
        options.addOption(mablOpt);
        options.addOption(verboseOpt);
        options.addOption(versionOpt);
        //        options.addOption(contextOpt);
        options.addOption(interpretOpt);
        options.addOption(dumpLocation);
        options.addOption(dumpIntermediateSpecs);

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


        boolean verbose = cmd.hasOption(verboseOpt.getOpt());

        List<File> sourceFiles = Arrays.stream(cmd.getOptionValues(mablOpt.getOpt())).map(File::new).collect(Collectors.toList());


        IErrorReporter reporter = new ErrorReporter();

        //used to file lookup
        File specificationDirectory = new File(".");
        File workingDirectory = new File(".");

        Mabl mabl = new Mabl(specificationDirectory, workingDirectory);
        mabl.setReporter(reporter);
        mabl.setVerbose(verbose);
        mabl.getSettings().dumpIntermediateSpecs = cmd.hasOption(dumpIntermediateSpecs.getOpt());

        if (!sourceFiles.isEmpty()) {
            mabl.parse(sourceFiles);
        }
        //        if (useTemplate) {
        //            mabl.generateSpec(testJsonObject.initialize, testJsonObject.simulate, testJsonObject.useLogLevels, new File(directory, "env.json"));
        //        }

        mabl.expand();


        if (reporter.getErrorCount() > 0) {
            reporter.printErrors(new PrintWriter(System.err, true));
            System.exit(1);
        }

        if (cmd.hasOption(dumpLocation.getOpt())) {
            mabl.dump(new File(cmd.getOptionValue(dumpLocation.getOpt())));
        }


        if (cmd.hasOption(interpretOpt.getOpt())) {
            new MableInterpreter(new DefaultExternalValueFactory(workingDirectory,
                    IOUtils.toInputStream(mabl.getRuntimeDataAsJsonString(), StandardCharsets.UTF_8))).execute(mabl.getMainSimulationUnit());
        }
    }
}



