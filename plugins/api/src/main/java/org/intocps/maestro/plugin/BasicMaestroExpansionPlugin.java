package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.ISimulationEnvironment;

import java.util.List;
import java.util.Map;

/**
 * Abstract base maestro expansion plugin implementation. All plugins should extend this class to reduce the impact on future changes of the @{link
 * {@link IMaestroExpansionPlugin} interface
 */
public abstract class BasicMaestroExpansionPlugin implements IMaestroExpansionPlugin {

    @Override
    public ConfigOption getConfigRequirement() {
        return this.requireConfig() ? ConfigOption.Required : ConfigOption.NotRequired;
    }

    @Override
    public boolean requireConfig() {
        return false;
    }

    @Override
    public <R> Map.Entry<List<PStm>, RuntimeConfigAddition<R>> expandWithRuntimeAddition(AFunctionDeclaration declaredFunction,
            List<PExp> formalArguments, IPluginConfiguration config, ISimulationEnvironment env,
            IErrorReporter errorReporter) throws ExpandException {
        {

            return Map.entry(expand(declaredFunction, formalArguments, config, env, errorReporter), new RuntimeConfigAddition<>() {
                @Override
                public RuntimeConfigAddition<R> merge(RuntimeConfigAddition<R> original) {
                    return original;
                }
            });

        }
    }
}
