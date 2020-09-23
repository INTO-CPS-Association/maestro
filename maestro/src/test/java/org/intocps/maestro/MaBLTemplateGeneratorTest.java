package org.intocps.maestro;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.PStm;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.List;

public class MaBLTemplateGeneratorTest {

    @Test
    public void GenerateSingleWaterTankTemplate() throws Exception {
        File configurationDirectory = Paths.get("src", "test", "resources", "specifications", "full", "initialize_singleWaterTank").toFile();
        File config = new File(configurationDirectory, "env.json");
        UnitRelationship ur = new UnitRelationship(new FileInputStream(config));
        List<PStm> pStms = MaBLTemplateGenerator.generateTemplate(ur);
        System.out.println(PrettyPrinter.print(MableAstFactory.newABlockStm(pStms)));
    }

}