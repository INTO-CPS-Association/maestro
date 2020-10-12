import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.plugin.verificationsuite.vdmcheck.VDMChecker;
import org.junit.Test;

import java.io.InputStream;
import java.util.stream.Collectors;

public class VDMCheckerTest {
    InputStream envWaterTankJson = this.getClass().getResourceAsStream("PrologVerifierTest/env.json");
    InputStream envThreeTankJson = this.getClass().getResourceAsStream("PrologVerifierTest/threetank_env.json");

    @Test
    public void VDMCheckerWaterTank() throws Exception {
        var VDMCheck = new VDMChecker();
        var unitRelationship = FmiSimulationEnvironment.of(envWaterTankJson, new IErrorReporter.SilentReporter());
        var fmus = unitRelationship.getFmuToUri();
        var fmuPaths = fmus.stream().map(o -> String.valueOf(o.getValue())).collect(Collectors.toList());
        VDMCheck.CheckFMUS(fmuPaths);
    }

    @Test
    public void VDMCheckerThreeTank() throws Exception {
        var VDMCheck = new VDMChecker();
        var unitRelationship = FmiSimulationEnvironment.of(envThreeTankJson, new IErrorReporter.SilentReporter());
        var fmus = unitRelationship.getFmuToUri();
        var fmuPaths = fmus.stream().map(o -> String.valueOf(o.getValue())).collect(Collectors.toList());
        VDMCheck.CheckFMUS(fmuPaths);
    }
}
