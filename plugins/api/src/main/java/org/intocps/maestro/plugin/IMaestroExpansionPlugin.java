package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.PExp;
import org.intocps.maestro.ast.PStm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.ISimulationEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public interface IMaestroExpansionPlugin extends IMaestroPlugin {

    Set<AFunctionDeclaration> getDeclaredUnfoldFunctions();

    List<PStm> expand(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config, ISimulationEnvironment env,
            IErrorReporter errorReporter) throws ExpandException;

    boolean requireConfig();

    IPluginConfiguration parseConfig(InputStream is) throws IOException;
}
