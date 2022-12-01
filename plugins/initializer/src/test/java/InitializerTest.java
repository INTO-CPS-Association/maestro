import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.plugin.IMaestroExpansionPlugin;
import org.intocps.maestro.plugin.IPluginConfiguration;
import org.intocps.maestro.plugin.initializer.Initializer;
import org.intocps.maestro.plugin.initializer.TopologicalPlugin;
import org.intocps.maestro.plugin.verificationsuite.prologverifier.InitializationPrologQuery;
import org.intocps.maestro.plugin.verificationsuite.prologverifier.PrologGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import scala.Console;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.newAArrayType;
import static org.intocps.maestro.ast.MableAstFactory.newANameType;

public class InitializerTest {
    InputStream minimalConfiguration = this.getClass().getResourceAsStream("InitializePluginTest/config.json");
    InputStream envJson = this.getClass().getResourceAsStream("InitializePluginTest/env.json");

    @Test
    public void parseConfig() throws IOException {
        InputStream pluginConfiguration = minimalConfiguration;
        var topologicalPlugin = new TopologicalPlugin();
        var prologGenerator = new PrologGenerator();
        var initializationPrologQuery = new InitializationPrologQuery(prologGenerator);
        IMaestroExpansionPlugin plugin = new Initializer(topologicalPlugin, initializationPrologQuery);
        plugin.parseConfig(minimalConfiguration);
    }

    @Test
    public void unfoldCallsSpecGen() throws Exception {
        InputStream pluginConfiguration = minimalConfiguration;
        var topologicalPlugin = new TopologicalPlugin();
        var prologGenerator = new PrologGenerator();
        var initializationPrologQuery = new InitializationPrologQuery(prologGenerator);
        IMaestroExpansionPlugin plugin = new Initializer(topologicalPlugin, initializationPrologQuery);
        AFunctionDeclaration funcDecl = plugin.getDeclaredImportUnit().getModule().getFunctions().iterator().next();
        IPluginConfiguration parsedPluginConfiguration = plugin.parseConfig(pluginConfiguration);

        var components = Arrays.asList("crtlInstance", "wtInstance");
        List<PExp> arguments = setupFormalArguments(components, 0, 10);

        List<PStm> stm1 = plugin.expand(funcDecl, arguments, parsedPluginConfiguration,
                Fmi2SimulationEnvironment.of(envJson, new IErrorReporter.SilentReporter()), new IErrorReporter.SilentReporter());
        Console.println(stm1.toString());

        //Useful test to make
        //Make sure SetupExperiment is called for all components
        components.forEach(o -> Assertions.assertTrue(stm1.toString().contains(o + ".setupExperiment")));

        //Make sure EnterInitial
        components.forEach(o -> Assertions.assertTrue(stm1.toString().contains(o + ".enterInitializationMode")));

        //Make sure ExitInitialazionMode is called on all components
        components.forEach(o -> Assertions.assertTrue(stm1.toString().contains(o + ".exitInitializationMode")));
    }

    private List<PExp> setupFormalArguments(List<String> componentInstances, int startTime, int endTime) {
        var decl = MableAstFactory.newAVariableDeclaration(new LexIdentifier("components", null), newAArrayType(newANameType("FMI2Component")),
                componentInstances.size(), MableAstFactory.newAArrayInitializer(
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
        //end time defined
        arguments.add(MableAstFactory.newABoolLiteralExp(true));

        return arguments;
    }
}