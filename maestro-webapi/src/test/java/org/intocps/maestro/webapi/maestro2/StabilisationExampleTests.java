package org.intocps.maestro.webapi.maestro2;

import org.apache.commons.io.FileUtils;
import org.intocps.maestro.testutils.FmuTestModelRepository;
import org.intocps.maestro.webapi.Application;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class StabilisationExampleTests {
    File folder_stabilisation_example_resources = new File("src/test/resources/maestro2/stabilisation_example");
    File folder_stabilisation_example_target = new File("target/stabilisation_example");
    File folder_stabilisation_example_stable_result = new File(folder_stabilisation_example_target, "stable.csv");
    File folder_stabilisation_example_unstable_result = new File(folder_stabilisation_example_target, "unstable.csv");
    File stableMMConfig = new File(folder_stabilisation_example_resources, "stablemm.json");
    File unstableMMConfig = new File(folder_stabilisation_example_resources, "unstablemm.json");

    @BeforeAll
    static void downloadFmus() throws Exception {
        FmuTestModelRepository.download(List.of(FmuTestModelRepository.url("MassSpringDamper1.fmu"),
            FmuTestModelRepository.url("MassSpringDamper2.fmu")));
    }

    @Test
    void configurationBasedExample() throws Exception {
        if (folder_stabilisation_example_target.exists() && folder_stabilisation_example_target.isDirectory()) {
            FileUtils.deleteDirectory(folder_stabilisation_example_target);
        }
        configurationBasedExampleTest(stableMMConfig, folder_stabilisation_example_stable_result);
        configurationBasedExampleTest(unstableMMConfig, folder_stabilisation_example_unstable_result);
        // Copy plotter script
        FileUtils.copyFile(new File(folder_stabilisation_example_resources, "plotter.py"),
                new File(folder_stabilisation_example_target, "plotter" + ".py"));
    }

    public void configurationBasedExampleTest(File mm, File result) throws Exception {

        System.out.println("Running configurationBasedExampleTest and storing result in: " + result.getAbsolutePath());
        String[] args = {"--oneshot", "--configuration", mm.toString(), "--starttime", "0.0", "--endtime", "10.0", "--result", result.toString()};
        Application.main(args);


    }
}
