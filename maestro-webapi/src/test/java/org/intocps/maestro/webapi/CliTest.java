package org.intocps.maestro.webapi;

import org.intocps.maestro.testutils.RequiresFmu;
import org.intocps.maestro.testutils.TestFmu;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

@RequiresFmu({TestFmu.SINGLE_WATER_TANK, TestFmu.WATER_TANK_CONTROLLER})
public class CliTest {

    @Test
    public void oneShotTest() throws Exception {
        Path outFile = Paths.get("target", CliTest.class.getName(), "outputs.csv");
        outFile.toFile().getParentFile().mkdirs();
        double startTimeIn = 0;
        double endTimeIn = 10;

        String arguments = String.format(Locale.US, "--oneshot --configuration %s --starttime %f --endtime %f --result %s",
                Paths.get("src", "test", "resources", "cli-test", "config.json").toFile().getAbsolutePath(), startTimeIn, endTimeIn, outFile);
        String[] s = arguments.split(" ");

        Application.main(s);
    }
}
