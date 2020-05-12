import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.ast.PStm;
import org.intocps.maestro.plugin.InitializerWrapCoe.FMIStatementInterface.StatementFactory;
import org.intocps.maestro.plugin.InitializerWrapCoe.Spec.StatementContainer;
import org.intocps.maestro.plugin.InitializerWrapCoe.SpecGen;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SpecGenTests {
    @Test
    @Ignore("Ignored because of set nondeterminism. Maven creates 1 result, intellij another")
    public void testWatertankExample() throws IOException, NanoHTTPD.ResponseException {
        InputStream configurationDataStream = this.getClass().getResourceAsStream("watertankexample/mm.json");
        String configurationData = IOUtils.toString(configurationDataStream);
        InputStream startMsgStream = this.getClass().getResourceAsStream("watertankexample/startmsg.json");
        String startMsg = IOUtils.toString(startMsgStream);

        SpecGen sg = new SpecGen();
        PStm stm = sg.run(configurationData, startMsg);
        Assert.assertEquals(stm.toString(), correctWatertank2results);
    }

    String correctWatertank2results = "{\n" +
            "int status = 0;\n" +
            "FMI2 tankcontroller = load(\"FMI2\", \"{8c4e810f-3df3-4a00-8276-176fa3c9f000}\", \"target/test-classes/watertankexample/watertankcontroller-c.fmu\");\n" +
            "FMI2 SingleWatertank = load(\"FMI2\", \"{cfc65592-9ece-4563-9705-1581b6e7071c}\", \"target/test-classes/watertankexample/singlewatertank-20sim.fmu\");\n" +
            "FMI2Component crtlInstance = tankcontroller.instantiate(\"crtlInstance\", false, false);\n" +
            "FMI2Component wtInstance = SingleWatertank.instantiate(\"wtInstance\", false, false);\n" +
            "status = crtlInstance.setupExperiment(false, 0.0, 0.0, true, 10.0);\n" +
            "status = wtInstance.setupExperiment(false, 0.0, 0.0, true, 10.0);\n" +
            "real[] realValueSize2[2] = {2.0, 1.0};\n" +
            "uInt[] valRefsSize2[2] = {0, 1};\n" +
            "status = crtlInstance.setReal(valRefsSize2, 2, realValueSize2);\n" +
            "real[] realValueSize7[7] = {9.0, 1.0, 1.0, 9.81, 1.0, 0.0, 0.0};\n" +
            "uInt[] valRefsSize7[7] = {0, 1, 2, 3, 4, 5, 6};\n" +
            "status = wtInstance.setReal(valRefsSize7, 7, realValueSize7);\n" +
            "status = crtlInstance.enterInitializationMode();\n" +
            "status = wtInstance.enterInitializationMode();\n" +
            "bool[] booleanValueSize1[1];\n" +
            "uInt[] valRefsSize1[1] = {4};\n" +
            "status = crtlInstance.getBoolean(valRefsSize1, 1, booleanValueSize1);\n" +
            "bool crtlInstanceSvValRef4 = booleanValueSize1[0];\n" +
            "real[] realValueSize1[1] = {0.0};\n" +
            "valRefsSize1[0] = 16;\n" +
            "status = wtInstance.setReal(valRefsSize1, 1, realValueSize1);\n" +
            "valRefsSize1[0] = 17;\n" +
            "status = wtInstance.getReal(valRefsSize1, 1, realValueSize1);\n" +
            "bool wtInstanceSvValRef17 = realValueSize1[0];\n" +
            "realValueSize1[0] = 1.0;\n" +
            "valRefsSize1[0] = 3;\n" +
            "status = crtlInstance.setReal(valRefsSize1, 1, realValueSize1);\n" +
            "status = crtlInstance.exitInitializationMode();\n" +
            "status = wtInstance.exitInitializationMode();\n" +
            "valRefsSize1[0] = 4;\n" +
            "status = crtlInstance.getBoolean(valRefsSize1, 1, booleanValueSize1);\n" +
            "crtlInstanceSvValRef4 = booleanValueSize1[0];\n" +
            "valRefsSize1[0] = 17;\n" +
            "status = wtInstance.getReal(valRefsSize1, 1, realValueSize1);\n" +
            "wtInstanceSvValRef17 = realValueSize1[0];\n" +
            "}";
}
