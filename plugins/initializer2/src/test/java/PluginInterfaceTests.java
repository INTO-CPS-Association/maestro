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
import java.util.*;

import static org.intocps.maestro.ast.MableAstFactory.newAArrayType;
import static org.intocps.maestro.ast.MableAstFactory.newANameType;

public class PluginInterfaceTests {

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
        List<PExp> arguments = new Vector<>();

        var decl = MableAstFactory.newAVariableDeclaration(new LexIdentifier("components", null), newAArrayType(newANameType("FMI2Component")),
                MableAstFactory.newAArrayInitializer(Arrays.asList(MableAstFactory.newAIdentifierExp("crtlInstance"), MableAstFactory.newAIdentifierExp(
                        "wtInstance"))));



        var stm = MableAstFactory.newALocalVariableStm(decl);
        //components
        var blockStm = MableAstFactory.newABlockStm(Arrays.asList(stm));
        var formalArg = MableAstFactory.newAIdentifierExp("components");

        formalArg.parent(blockStm);
        arguments.add(formalArg);

        //start time
        arguments.add(MableAstFactory.newAIntLiteralExp(0));
        //end time
        arguments.add(MableAstFactory.newAIntLiteralExp(10));


        PStm stm1 = plugin.unfold(funcDecl, arguments, parsedPluginConfiguration, new UnitRelationship(envJson), null);
        Console.println(stm1.toString());

        Assert.assertTrue(stm1 != null);
    }
}
