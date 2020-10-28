package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.intocps.fmi.FmuInvocationException;
import org.intocps.fmi.FmuMissingLibraryException;
import org.intocps.fmi.IFmu;
import org.intocps.fmi.jnifmuapi.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class MaestroV1CliProxy {
    public final static Option verboseOpt = Option.builder("v").desc("Verbose").build();
    public final static Option versionOpt = Option.builder("version").longOpt("version").desc("Version").build();
    public final static Option extractOpt =
            Option.builder("x").longOpt("extract").hasArg().numberOfArgs(1).argName("type").desc("Extract values: 'script'").build();
    public final static Option portOpt =
            Option.builder("p").longOpt("port").desc("The port where the REST interface will be served").hasArg().numberOfArgs(1).argName("port")
                    .build();
    public final static Option oneShotOpt = Option.builder("o").longOpt("oneshot").desc("Run a single simulation and shutdown").build();
    public final static Option configOpt =
            Option.builder("c").longOpt("configuration").desc("Path to configuration file").hasArg().numberOfArgs(1).argName("path").build();
    public final static Option simulationConfigOpt =
            Option.builder("sc").longOpt("simulationconfiguration").desc("Path to simulation configuration file").hasArg().numberOfArgs(1)
                    .argName("path").build();
    public final static Option resultOpt =
            Option.builder("r").longOpt("result").desc("Path where the csv data should be writing to").hasArg().numberOfArgs(1).argName("path")
                    .build();
    public final static Option startTimeOpt =
            Option.builder("s").longOpt("starttime").desc("The start time of the simulation").hasArg().numberOfArgs(1).argName("time").build();
    public final static Option endTimeOpt =
            Option.builder("e").longOpt("endtime").desc("The start time of the simulation").hasArg().numberOfArgs(1).argName("time").build();
    public final static Option loadSingleFMUOpt =
            Option.builder("l").longOpt("load").desc("Attempt to load a single FMU").hasArg().numberOfArgs(1).argName("path").build();
    final static Logger logger = LoggerFactory.getLogger(MaestroV1CliProxy.class);
    final static Option helpOpt = Option.builder("h").longOpt("help").desc("Show this description").build();
    final static Options options = getOptions();

    static Options getOptions() {
        Options options = new Options();
        options.addOption(helpOpt);
        options.addOption(portOpt);
        options.addOption(oneShotOpt);
        options.addOption(configOpt);
        options.addOption(simulationConfigOpt);
        options.addOption(startTimeOpt);
        options.addOption(endTimeOpt);
        options.addOption(verboseOpt);
        options.addOption(resultOpt);
        options.addOption(extractOpt);
        options.addOption(versionOpt);
        options.addOption(loadSingleFMUOpt);
        return options;
    }

    private static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("maestro", options);
    }

    public static CommandLine parse(String[] args) throws InterruptedException, IOException {


        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e1) {
            System.err.println("Parsing failed. Reason: " + e1.getMessage());
            showHelp(options);
        }
        return cmd;
    }


    public static boolean process(CommandLine cmd, OneShotRunner oneShotRunner, WebServiceRunner webServiceRunner) throws IOException {


        if (cmd.hasOption(loadSingleFMUOpt.getOpt())) {
            File fmuFile = getFile(loadSingleFMUOpt, cmd);
            try {
                IFmu fmu = Factory.create(fmuFile);
                fmu.load();
                fmu.unLoad();
                System.out.println("Successfully loaded FMU");
            } catch (FmuInvocationException | FmuMissingLibraryException e) {
                System.out.println("Failed to load FMU:\n");
                e.printStackTrace();
                return false;
            }
            return true;
        }

        if (cmd.hasOption(helpOpt.getOpt())) {
            showHelp(options);
            return true;
        }

        if (cmd.hasOption(versionOpt.getOpt())) {
            System.out.println(getVersion());
            return true;
        }

        boolean verbose = cmd.hasOption(verboseOpt.getOpt());

        if (cmd.hasOption(extractOpt.getOpt())) {
            processExtract(cmd.getOptionValue(extractOpt.getOpt()));
            return true;
        }

        if (!checkNativeFmi()) {
            return true;
        }

        printVersion();

        if (cmd.hasOption(oneShotOpt.getOpt())) {
            Double startTime;
            Double endTime;
            File configFile;
            File simulationConfigFile;
            File outputFile = new File("output.csv");

            configFile = getFile(configOpt, cmd, true);

            startTime = getDouble(startTimeOpt, cmd, true);

            endTime = getDouble(endTimeOpt, cmd, true);

            simulationConfigFile = getFile(simulationConfigOpt, cmd, true);

            if (cmd.hasOption(resultOpt.getOpt())) {
                outputFile = getFile(resultOpt, cmd);
            }

            // If there is no simulation config file, then start and endtime has to be defined.
            if ((simulationConfigFile == null && (startTime == null || endTime == null)) || configFile == null || outputFile == null) {
                System.err.println(String.format(
                        "Missing an option for one shot mode.\n" + "One shot mode requires:\n" + "(-%c AND -%s) AND (-%s OR (-%s AND -%s)  ",
                        configOpt.getLongOpt(), resultOpt.getLongOpt(), simulationConfigOpt.getLongOpt(), startTimeOpt.getLongOpt(),
                        endTimeOpt.getLongOpt()));
                return false;
            }

            if (simulationConfigFile != null && simulationConfigFile.exists() && simulationConfigFile.isFile()) {
                startTime = new ObjectMapper().readTree(simulationConfigFile).get("startTime").asDouble();
                endTime = new ObjectMapper().readTree(simulationConfigFile).get("endTime").asDouble();
            }

            return oneShotRunner.run(verbose, configFile, simulationConfigFile, startTime, endTime, outputFile);
        } else {

            // Change port if requested
            int port = 8082;
            if (cmd.hasOption(portOpt.getOpt())) {
                port = Integer.parseInt(cmd.getOptionValue(portOpt.getOpt()));
            }

            webServiceRunner.run(port);
        }
        return true;
    }

    private static void processExtract(String optionValue) throws IOException {
        if ("script".equals(optionValue)) {
            File file = new File("client.py");
            System.out.println("Extracting script to: " + file.getName());
            InputStream in = Main.class.getResourceAsStream("/client.py");
            OutputStream out = new FileOutputStream(file);
            IOUtils.copy(in, out);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    private static Double getDouble(Option opt, CommandLine cmd) {
        return getDouble(opt, cmd, false);
    }

    private static Double getDouble(Option opt, CommandLine cmd, boolean quiet) {
        if (cmd.hasOption(opt.getOpt())) {
            try {
                return Double.parseDouble(cmd.getOptionValue(opt.getOpt()));
            } catch (NumberFormatException e) {
                System.err.println("Option " + opt.getLongOpt() + " must be a double");
                return null;
            }
        } else {
            if (!quiet) {
                System.err.println("Missing option --" + opt.getLongOpt());
            }
            return null;
        }
    }

    private static File getFile(Option opt, CommandLine cmd) {
        return getFile(opt, cmd, false);
    }

    private static File getFile(Option opt, CommandLine cmd, boolean quiet) {
        if (cmd.hasOption(opt.getOpt())) {
            return new File(cmd.getOptionValue(opt.getOpt()));
        } else {
            if (!quiet) {
                System.err.println("Missing option --" + opt.getLongOpt());
            }
            return null;
        }
    }

    //    private static boolean runOneShotSimulation(boolean verbose, File configFile, File simulationConfigFile, Double startTime, Double endTime,
    //            File outputFile) throws IOException, NanoHTTPD.ResponseException {
    //        String config = FileUtils.readFileToString(configFile, "UTF-8");
    //
    //        SingleSimMain.SimulationExecutionUtilStatusWriter simulationExecutionUtilStatusWriter =
    //                new SingleSimMain.SimulationExecutionUtilStatusWriter(verbose);
    //
    //        if (simulationConfigFile != null) {
    //            String simulationConfig = FileUtils.readFileToString(simulationConfigFile, "UTF-8");
    //            return simulationExecutionUtilStatusWriter.run(config, simulationConfig, outputFile);
    //        } else {
    //            return simulationExecutionUtilStatusWriter.run(config, startTime, endTime, outputFile);
    //        }
    //    }

    private static String getVersion() {
        try {
            Properties prop = new Properties();
            InputStream coeProp = Main.class.getResourceAsStream("maestro.properties");
            prop.load(coeProp);
            return prop.getProperty("version");
        } catch (Exception e) {
            return "";
        }
    }

    private static void printVersion() {
        try {
            System.out.println("Version: " + getVersion());
        } catch (Exception e) {
        }
    }

    //    private static void runHttpSerivce(int port) throws InterruptedException {
    //        SessionController sessionController = new SessionController(new ProdSessionLogicFactory());
    //        org.intocps.orchestration.coe.httpserver.RequestProcessors requestProcessors =
    //                new org.intocps.orchestration.coe.httpserver.RequestProcessors(sessionController);
    //        NanoHTTPD server = new NanoWSDImpl(port, new RequestHandler(sessionController, requestProcessors));
    //        System.out.println("Now running on port " + port);
    //
    //        try {
    //            server.start(0, false);
    //            while (server.isAlive()) {
    //                Thread.sleep(1000);
    //            }
    //        } catch (IOException ioe) {
    //            System.err.println("Couldn't start server:\n" + ioe);
    //            System.exit(-1);
    //        } finally {
    //            server.stop();
    //        }
    //    }

    private static boolean checkNativeFmi() {
        logger.debug("Checking native FMI support");
        try {
            Factory.checkApi();
        } catch (Throwable e) {
            System.err.println("Failed to load FMI API");
            logger.error("Failed to load FMI API", e);
            return false;
        }
        return true;
    }

    public interface WebServiceRunner {
        void run(int port);
    }

    public interface OneShotRunner {
        boolean run(boolean verbose, File configFile, File simulationConfigFile, Double startTime, Double endTime,
                File outputFile) throws IOException;
    }
}
