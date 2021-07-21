package org.intocps.maestro.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import core.MasterModel;
import org.apache.commons.io.FilenameUtils;
import org.intocps.maestro.core.dto.MultiModelScenarioVerifier;
import org.intocps.maestro.plugin.MasterModelMapper;
import picocli.CommandLine;
import synthesizer.ConfParser.ScenarioConfGenerator;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "generate-algorithm", description = "Generates an algorithm from a scenario or multi-model.", mixinStandardHelpOptions =
        true)
public class GenerateAlgorithmCmd  implements Callable<Integer> {
    @CommandLine.Parameters(description = "A scenario (.conf) or a multi-model (.json)")
    File file;

    @CommandLine.Option(names = "-output", description = "Path to a directory where the algorithm will be stored")
    File output;

    @Override
    public Integer call() throws Exception {
        Path filePath = file.toPath();
        MasterModel masterModel;
        if(FilenameUtils.getExtension(filePath.toString()).equals("conf")){
            String scenario = Files.readString(filePath);
            masterModel = MasterModelMapper.Companion.scenarioToMasterModel(scenario);
        }
        else if (FilenameUtils.getExtension(filePath.toString()).equals("json")) {
            MultiModelScenarioVerifier multiModel = (new ObjectMapper()).readValue(file, MultiModelScenarioVerifier.class);
            masterModel = MasterModelMapper.Companion.multiModelToMasterModel(multiModel, 3);
        }
        else {
            return -1;
        }

        String algorithm = ScenarioConfGenerator.generate(masterModel, masterModel.name());
        Path algorithmPath = output.toPath().resolve("algorithm.conf");
        Files.write(algorithmPath, algorithm.getBytes(StandardCharsets.UTF_8));

        return 0;
    }
}
