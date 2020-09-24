import org.intocps.maestro.plugin.env.UnitRelationship;
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
        var unitRelationship = new UnitRelationship(envWaterTankJson);
        var fmus = unitRelationship.getFmuToUri();
        var fmuPaths = fmus.stream().map(o -> String.valueOf(o.getValue())).collect(Collectors.toList());
        VDMCheck.CheckFMUS(fmuPaths);
    }

    @Test
    public void VDMCheckerThreeTank() throws Exception {
        var VDMCheck = new VDMChecker();
        var unitRelationship = new UnitRelationship(envThreeTankJson);
        var fmus = unitRelationship.getFmuToUri();
        var fmuPaths = fmus.stream().map(o -> String.valueOf(o.getValue())).collect(Collectors.toList());
        VDMCheck.CheckFMUS(fmuPaths);
    }
}
