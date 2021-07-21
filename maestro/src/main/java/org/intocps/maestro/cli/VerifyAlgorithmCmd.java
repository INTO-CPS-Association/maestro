package org.intocps.maestro.cli;

import cli.VerifyTA;
import core.MasterModel;
import core.ModelEncoding;
import core.ScenarioGenerator;
import core.ScenarioLoader;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "verify-algorithm", description = "Verifies an algorithm generated from a scenario.",
        mixinStandardHelpOptions = true)
public class VerifyAlgorithmCmd implements Callable<Integer> {
    @CommandLine.Parameters(description = "A master model (scenario + algorithm) on .conf format")
    File file;

    @CommandLine.Option(names = "-output", description = "Path to a directory where the encoded master model file will be stored")
    File output;

    @Override
    public Integer call() throws Exception {

        MasterModel masterModel = ScenarioLoader.load(new ByteArrayInputStream(Files.readString(file.toPath()).getBytes()));
        if (output == null) {
            output = Files.createTempDirectory("tmpDir").toFile();
        }
        File uppaalFile = output.toPath().resolve("uppaal.xml").toFile();
        int resultCode;
        if (VerifyTA.checkEnvironment()) {
            try (FileWriter fileWriter = new FileWriter(uppaalFile)) {
                fileWriter.write(ScenarioGenerator.generate(new ModelEncoding(masterModel)));
            } catch (Exception e) {
                System.out.println("Unable to write encoded master model to file: " + e);
            }
            resultCode = VerifyTA.verify(uppaalFile);
        } else {
            System.out.println("Verification environment is not setup correctly");
            return -1;
        }

        System.out.println("Output written to: " + output.getPath());

        if (resultCode == 0) {
            System.out.println("Algorithm successfully verified");
        } else {
            System.out.println("Algorithm did NOT verify successfully");
        }

        if (resultCode == 2) {
            System.out.println("Unable to verify algorithm - there is probably a syntax error.");
        } else if (resultCode == 1) {
            System.out.println("Violations of the scenarios' contract were found - traces can be generated.");
        } else if (resultCode == 0) {
            System.out.println("Algorithm successfully verified - no traces to visualize.");
        } else {
            System.out.println("Unknown algorithm verification error code encountered: " + resultCode);
            return -1;
        }

        return 0;
    }
}
