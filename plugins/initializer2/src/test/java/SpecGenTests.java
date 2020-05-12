import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.ast.PStm;
import org.intocps.maestro.plugin.InitializerWrapCoe.FMIStatementInterface.StatementFactory;
import org.intocps.maestro.plugin.InitializerWrapCoe.Spec.StatementContainer;
import org.intocps.maestro.plugin.InitializerWrapCoe.SpecGen;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SpecGenTests {

    @Test
    public void testWatertankC() throws IOException, NanoHTTPD.ResponseException {
        InputStream configurationDataStream = this.getClass().getResourceAsStream("watertankconfig.json");
        String configurationData = IOUtils.toString(configurationDataStream);
        InputStream startMsgStream = this.getClass().getResourceAsStream("watertankconfig-startmsg.json");
        String startMsg = IOUtils.toString(startMsgStream);

        SpecGen sg = new SpecGen();
        PStm stm = sg.run(configurationData, startMsg);
        Assert.assertEquals(stm.toString(), WatertankCCorrectResult());
    }

    public static String WatertankCCorrectResult() {
        return "{\n" +
                "int status = 0;\n" +
                "FMI2 tankcontroller = load(\"FMI2\", \"target/test-classes/watertankcontroller-c.fmu\");;\n" +
                "FMI2 watertank-c = load(\"FMI2\", \"target/test-classes/watertank-c.fmu\");;\n" +
                "FMI2Component controller = tankcontroller.instantiate(\"controller\", false);;\n" +
                "FMI2Component tank = watertank-c.instantiate(\"tank\", false);;\n" +
                "status = controller.setupExperiment(false, 0.0, 0.0, true, 10.0);;\n" +
                "status = tank.setupExperiment(false, 0.0, 0.0, true, 10.0);;\n" +
                "real[] realValueSize2 = {10.0, 2.0};\n" +
                "uInt[] valRefsSize2 = {0, 1};\n" +
                "status = tank.setReal(valRefsSize2, 2, realValueSize2);;\n" +
                "status = controller.enterInitializationMode();;\n" +
                "status = tank.enterInitializationMode();;\n" +
                "bool[] booleanValueSize1;\n" +
                "uInt[] valRefsSize1 = {4};\n" +
                "status = controller.getBoolean(valRefsSize1, 1, booleanValueSize1);;\n" +
                "bool controllerSvValRef4 = booleanValueSize1[0];;\n" +
                "booleanValueSize1[0] = controllerSvValRef4;\n" +
                "valRefsSize1[0] = 4;\n" +
                "status = tank.setBoolean(valRefsSize1, 1, booleanValueSize1);;\n" +
                "real[] realValueSize1;\n" +
                "valRefsSize1[0] = 2;\n" +
                "status = tank.getReal(valRefsSize1, 1, realValueSize1);;\n" +
                "bool tankSvValRef2 = realValueSize1[0];;\n" +
                "realValueSize1[0] = 1.0;\n" +
                "valRefsSize1[0] = 3;\n" +
                "status = controller.setReal(valRefsSize1, 1, realValueSize1);;\n" +
                "status = controller.exitInitializationMode();;\n" +
                "status = tank.exitInitializationMode();;\n" +
                "valRefsSize1[0] = 4;\n" +
                "status = controller.getBoolean(valRefsSize1, 1, booleanValueSize1);;\n" +
                "controllerSvValRef4 = booleanValueSize1[0];;\n" +
                "valRefsSize1[0] = 2;\n" +
                "status = tank.getReal(valRefsSize1, 1, realValueSize1);;\n" +
                "tankSvValRef2 = realValueSize1[0];;\n" +
                "}";
    }
}
