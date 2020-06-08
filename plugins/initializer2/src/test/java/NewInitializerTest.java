import org.intocps.maestro.ast.*;
import org.intocps.maestro.plugin.IMaestroUnfoldPlugin;
import org.intocps.maestro.plugin.IPluginConfiguration;
import org.intocps.maestro.plugin.InitializerNew.InitializerNew;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.junit.Assert;
import org.junit.Test;
import scala.Console;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.newAArrayType;
import static org.intocps.maestro.ast.MableAstFactory.newANameType;

public class NewInitializerTest {
    InputStream minimalConfiguration = this.getClass().getResourceAsStream("InitializePluginTest/config.json");
    InputStream envJson = this.getClass().getResourceAsStream("InitializePluginTest/env.json");

    @Test
    public void ParseConfig() throws IOException {
        InputStream pluginConfiguration = minimalConfiguration;
        IMaestroUnfoldPlugin plugin = new InitializerNew();
        plugin.parseConfig(minimalConfiguration);
    }

    @Test
    public void UnfoldCallsSpecGen() throws Exception {
        InputStream pluginConfiguration = minimalConfiguration;
        IMaestroUnfoldPlugin plugin = new InitializerNew();
        AFunctionDeclaration funcDecl = plugin.getDeclaredUnfoldFunctions().iterator().next();
        IPluginConfiguration parsedPluginConfiguration = plugin.parseConfig(pluginConfiguration);

        var components = Arrays.asList("crtlInstance", "wtInstance");
        List<PExp> arguments = setupFormalArguments(components, 0, 10);

        PStm stm1 = plugin.unfold(funcDecl, arguments, parsedPluginConfiguration, new UnitRelationship(envJson), null);
        Console.println(stm1.toString());

        //Useful test to make
        //Make sure SetupExperiment is called for all components

        components.forEach(o -> Assert.assertTrue(stm1.toString().contains(o + ".setupExperiment")));
        //Make sure EnterInitial

        //Make sure ExitInitialazionMode is called on all components

    }

    private List<PExp> setupFormalArguments(List<String> componentInstances, int startTime, int endTime) {
        var decl = MableAstFactory.newAVariableDeclaration(new LexIdentifier("components", null), newAArrayType(newANameType("FMI2Component")),
                MableAstFactory.newAArrayInitializer(
                        componentInstances.stream().map(MableAstFactory::newAIdentifierExp).collect(Collectors.toList())));

        var stm = MableAstFactory.newALocalVariableStm(decl);
        //components
        var blockStm = MableAstFactory.newABlockStm(Arrays.asList(stm));
        var formalArg = MableAstFactory.newAIdentifierExp("components");
        List<PExp> arguments = new Vector<>();
        formalArg.parent(blockStm);
        arguments.add(formalArg);
        //start time
        arguments.add(MableAstFactory.newAIntLiteralExp(startTime));
        //end time
        arguments.add(MableAstFactory.newAIntLiteralExp(endTime));

        return arguments;
    }
}