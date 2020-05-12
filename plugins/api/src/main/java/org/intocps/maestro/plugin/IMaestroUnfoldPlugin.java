package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.PExp;
import org.intocps.maestro.ast.PStm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.plugin.env.ISimulationEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public interface IMaestroUnfoldPlugin extends IMaestroPlugin {

    Set<AFunctionDeclaration> getDeclaredUnfoldFunctions();

    PStm unfold(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config, ISimulationEnvironment env,
            IErrorReporter errorReporter) throws UnfoldException;

    boolean requireConfig();

    IPluginConfiguration parseConfig(InputStream is) throws IOException;
}
