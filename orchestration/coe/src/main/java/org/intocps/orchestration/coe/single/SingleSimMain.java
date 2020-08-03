/*
 * This file is part of the INTO-CPS toolchain.
 *
 * Copyright (c) 2017-CurrentYear, INTO-CPS Association,
 * c/o Professor Peter Gorm Larsen, Department of Engineering
 * Finlandsgade 22, 8200 Aarhus N.
 *
 * All rights reserved.
 *
 * THIS PROGRAM IS PROVIDED UNDER THE TERMS OF GPL VERSION 3 LICENSE OR
 * THIS INTO-CPS ASSOCIATION PUBLIC LICENSE VERSION 1.0.
 * ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS PROGRAM CONSTITUTES
 * RECIPIENT'S ACCEPTANCE OF THE OSMC PUBLIC LICENSE OR THE GPL
 * VERSION 3, ACCORDING TO RECIPIENTS CHOICE.
 *
 * The INTO-CPS toolchain  and the INTO-CPS Association Public License
 * are obtained from the INTO-CPS Association, either from the above address,
 * from the URLs: http://www.into-cps.org, and in the INTO-CPS toolchain distribution.
 * GNU version 3 is obtained from: http://www.gnu.org/copyleft/gpl.html.
 *
 * This program is distributed WITHOUT ANY WARRANTY; without
 * even the implied warranty of  MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE, EXCEPT AS EXPRESSLY SET FORTH IN THE
 * BY RECIPIENT SELECTED SUBSIDIARY LICENSE CONDITIONS OF
 * THE INTO-CPS ASSOCIATION.
 *
 * See the full INTO-CPS Association Public License conditions for more details.
 */

/*
 * Author:
 *		Kenneth Lausdahl
 *		Casper Thule
 */
package org.intocps.orchestration.coe.single;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.intocps.fmi.IFmu;
import org.intocps.orchestration.coe.FmuFactory;
import org.intocps.orchestration.coe.json.InitializationMsgJson;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.orchestration.coe.util.SimulationExecutionUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by kel on 17/12/16.
 */
public class SingleSimMain {

    public static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("fmu-import-export", options);
    }

    static void writeFailedStatus() {
        writeFailedStatus("");
    }

    static void writeFailedStatus(String extra) {
        try {
            String log = "";
            try {
                log = StringUtils.join(FileUtils.readLines(new File("coe.log"), "UTF-8"), "\n");
            } catch (Exception e) {
            }
            log += "\n" + extra + "\n";
            System.err.println("Simulation failed " + (extra != null ? extra : ""));
            FileUtils.writeStringToFile(new File("failed"), log);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class SimulationExecutionUtilStatusWriter
            extends SimulationExecutionUtil {

        public SimulationExecutionUtilStatusWriter(boolean verbose) {
            super(verbose);
        }

        @Override
        protected void handleInitializationError(
                String initializeResponseData) {
            writeFailedStatus(initializeResponseData);
        }

        @Override
        protected boolean handleSimulateError(
                String simulateResponseData, NanoHTTPD.Response response) {
            writeFailedStatus(simulateResponseData);
            return false;
        }

        @Override
        protected void handleDestroyError(NanoHTTPD.Response response) {
            try {
                writeFailedStatus(IOUtils.toString(response.getData()));
            } catch (IOException e) {
                writeFailedStatus();
            }
        }
    }

    public static void main(String[] args) throws Exception {

        removeOldLog();

        Options options = new Options();
        Option helpOpt = Option.builder("h").longOpt("help").desc("Show this description").build();

        Option fmuOpt = Option.builder("fmu").desc("FMU Path").hasArg().numberOfArgs(1).argName("path").required().build();
        Option optOpt = Option.builder("opt").desc("Option file Path").hasArg().numberOfArgs(1).argName("path").build();
        Option createCrossPlotScriptOpt = Option.builder("xplot").desc("Output Cross Plot script").build();
        Option refOpt = Option.builder("ref").desc("Output Cross Plot Ref").hasArg().numberOfArgs(1).argName("path").build();
        Option inOpt = Option.builder("in").desc("Input file").hasArg().numberOfArgs(1).argName("path").build();
        Option logLevelOpt = Option.builder("level").desc("Log level: DEBUG, INFO, WARN, ERROR, FATAL, OFF, TRACE").hasArg().numberOfArgs(1).argName("level").build();
        Option createReadmeOpt = Option.builder("readme").desc("Create cross check readme").build();
        Option verboseOpt = Option.builder("v").desc("Verbose").build();
        Option minStepSizeOpt = Option.builder("min").longOpt("min-stepsize").desc("Minimum step size").hasArg().numberOfArgs(1).argName("size").build();

        options.addOption(helpOpt);
        options.addOption(fmuOpt);
        options.addOption(optOpt);
        options.addOption(createCrossPlotScriptOpt);
        options.addOption(refOpt);
        options.addOption(inOpt);
        options.addOption(logLevelOpt);
        options.addOption(createReadmeOpt);
        options.addOption(verboseOpt);
        options.addOption(minStepSizeOpt);

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

        if (cmd.hasOption(createReadmeOpt.getOpt())) {
            createCrossCheckReadme(args);
        }

        String fmuPath = cmd.getOptionValue(fmuOpt.getOpt());
        String optPath = cmd.getOptionValue(optOpt.getOpt());

        double startTime = 0;
        double endTime = 0.1;
        double stepsize = 0.01;
        Double relTol = null;
        Double outputIntervalLength = null;

        if (optPath != null) {
            Collection<String> lines = FileUtils.readLines(new File(optPath), "UTF-8");
            for (String line : lines) {
                String items[] = line.split(",");
                if (items == null || items.length < 2)
                    continue;
                if ("StartTime".equals(items[0].trim())) {
                    startTime = Double.parseDouble(items[1]);
                    System.out.println("Setting: StartTime: " + startTime);
                } else if ("StopTime".equals(items[0].trim())) {
                    endTime = Double.parseDouble(items[1]);
                    System.out.println("Setting: StopTime: " + endTime);
                } else if ("StepSize".equals(items[0].trim())) {
                    stepsize = Double.parseDouble(items[1]);
                    System.out.println("Setting: StepSize: " + stepsize);
                } else if ("OutputIntervalLength".equals(items[0].trim())) {
                    outputIntervalLength = Double.parseDouble(items[1]);
                    System.out.println("Setting: OutputIntervalLength: "
                            + outputIntervalLength);
                } else if ("RelTol".equals(items[0].trim())) {
                    relTol = Double.parseDouble(items[1]);
                    System.out.println("Setting: RelTol: " + relTol);
                }
            }
        }

        System.setProperty(FmuFactory.customFmuFactoryProperty, StubFactory.class.getName());
        System.setProperty("coe.csv.quote.header", "true");
        System.setProperty("coe.csv.boolean.numeric", "true");

        if (cmd.hasOption(logLevelOpt.getOpt())) {
            Logger l = Logger.getRootLogger();
            l.setLevel(Level.toLevel(cmd.getOptionValue(logLevelOpt.getOpt())));
        }

        URI fmuUri = new File(fmuPath).toURI();
        if (fmuUri.getScheme() == null || fmuUri.getScheme().equals("file")) {
            if (!fmuUri.isAbsolute()) {
                System.out.println("resolving: " + fmuUri);
                fmuUri = new File(".").toURI().resolve(fmuUri);
            }
        }

        System.out.println("Reading FMU: " + fmuUri);

        IFmu fmu = FmuFactory.create(null, fmuUri);
        ModelDescription md = null;

        try {
            md = new ModelDescription(fmu.getModelDescription());
        } catch (org.xml.sax.SAXParseException e) {
            String message = "Model Description not valid, rejecting FMU.";
            System.err.println(message);
            FileUtils.writeStringToFile(new File("rejected"),
                    message + "\n" + ExceptionUtils.getStackTrace(e));
            return;
        }

        InitializationMsgJson initConfig = new InitializationMsgJson();
        initConfig.fmus = new HashMap<>();
        initConfig.connections = new HashMap<>();
        initConfig.parameters = new HashMap<>();
        initConfig.algorithm = new HashMap<>();
        initConfig.parallelSimulation = true;

        initConfig.fmus.put("source", fmuUri.toString());
        initConfig.fmus.put("stub", fmuUri.toString().replace("file", "stub"));

        final String ID_TEMPLATE = "%s.i.%s";

//		Map<String, Object> constraints = new HashedMap();

        for (ModelDescription.ScalarVariable sv : md.getOutputs()) {
            String outId = String.format(ID_TEMPLATE, "source", sv.getName());
            String inId = String.format(ID_TEMPLATE, "stub", sv.getName());
            initConfig.connections.put(outId, Arrays.asList(inId));

//			Map<String, Object> constraint = new HashMap<>();
//			constraint.put("reltol", relTol);
//			constraint.put("type", "boundeddifference");
//			constraint.put("skipDiscrete", true);
//			//			constraint.put("setId("db-"+sv.getName());
//			constraint.put("ports", new String[] {
//					"source.i." + sv.getName() });
//			constraints.put("db-" + sv.getName(), constraint);
        }

        if (cmd.hasOption(inOpt.getOpt())) {
            initConfig.fmus.put("csv", fmuUri.toString().replace("file", "csv"));

            FileReader in = new FileReader(cmd.getOptionValue(inOpt.getOpt()));
            Set<String> headers = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in).getHeaderMap().keySet();
            IOUtils.closeQuietly(in);

            for (String header : headers) {
                if (header.equals("time"))
                    continue;
                String outId = String.format(ID_TEMPLATE, "csv", header);
                String inId = String.format(ID_TEMPLATE, "source", header);
                initConfig.connections.put(outId, Arrays.asList(inId));
            }
            initConfig.parameters.put(String.format(ID_TEMPLATE, "csv", "inputFile"), cmd.getOptionValue(inOpt.getOpt()));

        }


        if (stepsize == 0) {
            //CrossCheck StepSize = 0 means variable step size
            initConfig.algorithm.put("size", new Object[]{1E-9,
                    outputIntervalLength == null ?
                            0.1 :
                            outputIntervalLength});
            initConfig.algorithm.put("type", "var-step");
            initConfig.algorithm.put("initsize", 1e-07);

            initConfig.algorithm.put("constraints", null);

        } else {
            initConfig.algorithm.put("size", stepsize);
            if (cmd.hasOption(minStepSizeOpt.getOpt()) && stepsize
                    < Double.parseDouble(cmd.getOptionValue(minStepSizeOpt.getOpt()))) {
                System.out.println("Skipping step size limit set to "
                        + cmd.getOptionValue(minStepSizeOpt.getOpt()));
                return;
            }
            initConfig.algorithm.put("type", "fixed-step");
        }

        final ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
        String config = mapper.writerFor(InitializationMsgJson.class).writeValueAsString(initConfig);

        String fmuName = new File(fmuUri).getName();
        fmuName = fmuName.substring(0, fmuName.lastIndexOf("."));
        File outputFile = new File(fmuName + "_out.csv");

        try {

            new SimulationExecutionUtilStatusWriter(cmd.hasOption(verboseOpt.getOpt())).run(config, startTime, endTime, outputFile);
        } catch (Exception e) {
            writeFailedStatus(
                    e.getMessage() + "\n\n" + ExceptionUtils.getStackTrace(e));
        }

        for (Map.Entry<String, String> f : initConfig.fmus.entrySet()) {
            initConfig.fmus.put(f.getKey(), f.getValue().substring(
                    f.getValue().lastIndexOf('/') + 1));
        }
        config = mapper.writerFor(InitializationMsgJson.class).writeValueAsString(initConfig);
        FileUtils.writeStringToFile(new File("config.json"), config);

        if (cmd.hasOption(createCrossPlotScriptOpt.getOpt())) {
            FileUtils.copyInputStreamToFile(SingleSimMain.class.getClassLoader().getResourceAsStream("crosscheck-plot.py"), new File("x-plot.py"));
            ProcessBuilder pb = new ProcessBuilder("python");
            pb.command("python", "x-plot.py", "--name", fmuUri.getPath().substring(
                    fmuUri.getPath().lastIndexOf('/')
                            + 1), "--result", outputFile.getName(), "--config", "config.json");
            if (cmd.hasOption(refOpt.getOpt())) {
                pb.command().add("--ref");
                pb.command().add(new File(cmd.getOptionValue(refOpt.getOpt())).getName());
            }

            FileUtils.writeStringToFile(new File("plot_cmd.sh"), StringUtils.join(pb.command(), " "));

            if (hasPython()) {
                System.out.print("\nProcessing python overview...");
                Process p = pb.start();
                if (p.waitFor() != 0) {
                    System.out.println(IOUtils.readLines(p.getErrorStream()));
                }
                System.out.println("\t Done.");
            }
        }
    }

    private static void removeOldLog() {
        try {
            Files.deleteIfExists(Paths.get("coe.log"));
        } catch (Exception e) {
        }
    }

    private static boolean hasPython() throws IOException, InterruptedException {
        try {
            ProcessBuilder pb = new ProcessBuilder("python");
            pb.command().add("--version");

            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void createCrossCheckReadme(String[] args) throws IOException {
        List<String> argList = Arrays.asList(args);

        for (int i = 0; i < argList.size(); i++) {

            String s = argList.get(i);
            int index = s.lastIndexOf(File.separatorChar);
            if (index > -1)
                argList.set(i, s.substring(index + 1));

        }

        List<String> lines = IOUtils.readLines(SingleSimMain.class.getClassLoader().getResourceAsStream("cross-check-readme.txt"), "UTF-8");

        int index = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals("#ARGS#")) {
                index = i;
                break;
            }
        }

        lines.set(index,
                "java -cp coe.jar " + SingleSimMain.class.getName() + " "
                        + StringUtils.join(argList, " "));

        FileUtils.writeStringToFile(new File("ReadMe.txt"), StringUtils.join(lines, "\n"));
    }
}
