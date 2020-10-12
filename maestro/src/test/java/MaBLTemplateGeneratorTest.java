import org.intocps.maestro.MaBLTemplateGenerator.MaBLTemplateConfiguration;
import org.intocps.maestro.MaBLTemplateGenerator.MaBLTemplateGenerator;
import org.intocps.maestro.ast.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.core.api.FixedStepSizeAlgorithm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;

public class MaBLTemplateGeneratorTest {

    @Test
    public void GenerateSingleWaterTankTemplate() throws Exception {
        final double endTime = 10.0;
        final double stepSize = 0.1;
        File configurationDirectory = Paths.get("src", "test", "resources", "specifications", "full", "initialize_singleWaterTank").toFile();
        File config = new File(configurationDirectory, "env.json");
        FmiSimulationEnvironment ur = FmiSimulationEnvironment.of(new FileInputStream(config), new IErrorReporter.SilentReporter());

        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder b = new MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder();
        FixedStepSizeAlgorithm stepSizeAlgorithm = new FixedStepSizeAlgorithm(endTime, stepSize);
        MaBLTemplateConfiguration mtc = b.setUnitRelationship(ur).useInitializer(true).setStepAlgorithm(stepSizeAlgorithm).build();
        ASimulationSpecificationCompilationUnit aSimulationSpecificationCompilationUnit = MaBLTemplateGenerator.generateTemplate(mtc);
        System.out.println(PrettyPrinter.print(aSimulationSpecificationCompilationUnit));
    }

}