import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.IOUtils;
import org.intocps.maestro.MableSpecificationGenerator;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.parser.ParseTree2AstConverter;
import org.intocps.maestro.plugin.InitializerWrapCoe.SpecGen;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PaserTest {
    @Test
    public void testWatertankC() throws IOException, NanoHTTPD.ResponseException {
        InputStream configurationDataStream = this.getClass().getResourceAsStream("watertankconfig.json");
        String configurationData = IOUtils.toString(configurationDataStream);
        InputStream startMsgStream = this.getClass().getResourceAsStream("watertankconfig-startmsg.json");
        String startMsg = IOUtils.toString(startMsgStream);

        SpecGen sg = new SpecGen();
        PStm stm = sg.run(configurationData, startMsg);
        ASimulationSpecificationCompilationUnit simulation = new ASimulationSpecificationCompilationUnit();
        simulation.setBody(stm);
        String initializationPluginSpec = simulation.toString();

        // Write spec to a file
        File f = new File("dummy");
        BufferedWriter wrtier = new BufferedWriter(new FileWriter(f));
        wrtier.write(initializationPluginSpec);
        wrtier.close();

        // Parse the spec
        MableSpecificationGenerator g= new MableSpecificationGenerator(false);
        InputStream contextFile = null;
        ARootDocument doc = g.generate(Stream.of(f.getAbsolutePath()).map(File::new).collect(Collectors.toList()), contextFile);
        String parsedInitializationSpec = doc.getContent().getFirst().toString();

        System.out.println(parsedInitializationSpec);

        // Compare the parsed spec and the initialization Plugin Spec.
        Assert.assertEquals(initializationPluginSpec, parsedInitializationSpec);
    }

    @Test
    public void testWatertank2() throws IOException, NanoHTTPD.ResponseException {
        InputStream configurationDataStream = this.getClass().getResourceAsStream("watertankexample/mm.json");
        String configurationData = IOUtils.toString(configurationDataStream);
        InputStream startMsgStream = this.getClass().getResourceAsStream("watertankexample/startmsg.json");
        String startMsg = IOUtils.toString(startMsgStream);

        SpecGen sg = new SpecGen();
        PStm stm = sg.run(configurationData, startMsg);
        ASimulationSpecificationCompilationUnit simulation = new ASimulationSpecificationCompilationUnit();
        simulation.setBody(stm);
        String initializationPluginSpec = simulation.toString();

        // Write spec to a file
        File f = new File("dummy");
        BufferedWriter wrtier = new BufferedWriter(new FileWriter(f));
        wrtier.write(initializationPluginSpec);
        wrtier.close();

        // Parse the spec
        MableSpecificationGenerator g= new MableSpecificationGenerator(false);
        InputStream contextFile = null;
        ARootDocument doc = g.generate(Stream.of(f.getAbsolutePath()).map(File::new).collect(Collectors.toList()), contextFile);
        String parsedInitializationSpec = doc.getContent().getFirst().toString();

        System.out.println(parsedInitializationSpec);

        // Compare the parsed spec and the initialization Plugin Spec.
        Assert.assertEquals(initializationPluginSpec, parsedInitializationSpec);
    }
}
