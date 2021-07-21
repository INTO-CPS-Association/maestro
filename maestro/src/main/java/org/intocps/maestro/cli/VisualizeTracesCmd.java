package org.intocps.maestro.cli;

import cli.VerifyTA;
import core.MasterModel;
import core.ModelEncoding;
import core.ScenarioGenerator;
import core.ScenarioLoader;
import picocli.CommandLine;
import scala.jdk.javaapi.CollectionConverters;
import trace_analyzer.TraceAnalyzer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "visualize-traces", description = "Visualizes traces for an algorithm that cannot be verified successfully.",
        mixinStandardHelpOptions = true)
public class VisualizeTracesCmd implements Callable<Integer> {
    @CommandLine.Parameters(description = "A master model (scenario + algorithm) on .conf format")
    File file;

    @CommandLine.Option(names = "-output", description = "Path to a directory where the visualization files will be stored")
    File output;

    @Override
    public Integer call() throws Exception {
        if (output == null) {
            output = Files.createTempDirectory("tmpDir").toFile();
        }

        if (!VerifyTA.checkEnvironment()) {
            System.out.println("Verification environment is not setup correctly");
            return -1;
        }

        File tempDir = Files.createTempDirectory("tmpDir").toFile();
        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(Files.readString(file.toPath()).getBytes()));
        File uppaalFile = Path.of(tempDir.getPath(), "uppaal.xml").toFile();
        File traceFile = Path.of(tempDir.getPath(), "trace.log").toFile();
        try (FileWriter fileWriter = new FileWriter(uppaalFile)) {
            fileWriter.write(ScenarioGenerator.generate(new ModelEncoding(masterModel)));
        } catch (Exception e) {
            System.out.println("Unable to write encoded master model to file: " + e);
            return -1;
        }
        // This verifies the algorithm and writes to the trace file.
        int resultCode = VerifyTA.saveTraceToFile(uppaalFile, traceFile);

        // If verification result code is 1 violations of the scenarios' contract were found and a trace has been written to the trace file
        // from which a visualization can be made.
        if (resultCode == 1) {
            Path videoTraceFolder = java.nio.file.Files.createDirectories(Path.of(tempDir.getPath(), "video_trace"));
            ModelEncoding modelEncoding = new ModelEncoding(masterModel);
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(traceFile))) {
                TraceAnalyzer.AnalyseScenario(masterModel.name(), CollectionConverters.asScala(bufferedReader.lines().iterator()), modelEncoding,
                        videoTraceFolder.toString());

            } catch (Exception e) {
                System.out.println("Unable to generate trace visualization: " + e);
                return -1;
            }
        }
        // If the verification code is anything else than 1 it is not possible to visualize the trace and this is an "error" for this endpoint
        // even though the verification might have been successful.
        else if (resultCode == 2) {
            System.out.println("Unable to verify algorithm - there is probably a syntax error.");
        } else if (resultCode == 0) {
            System.out.println("Algorithm successfully verified - no traces to visualize.");
        } else {
            System.out.println("Unknown algorithm verification error code encountered: " + resultCode);
            return -1;
        }

        return 0;
    }
}
