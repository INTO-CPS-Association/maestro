package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.node.ASimulationSpecificationCompilationUnit;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

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

    /**
     * Function to use for building using the raw interface
     * This is search order 2
     *
     * @param declaredFunction the function within the plugin that is selected to be expanded
     * @param formalArguments  the formal arguments given to the expansion. these will match the function signatures formals
     * @param config           the configuration of null is not required
     * @param env              the runtime environment
     * @param errorReporter    the error reported that must be used for reporting any errors or warnings
     * @param <R>              the runtime type
     * @return a map entry containing the list of raw entries and a runtime contribution or {@link org.intocps.maestro.plugin.IMaestroExpansionPlugin.EmptyRuntimeConfig}
     * @throws ExpandException if expansion fails
     */
    @Override
    public <R> Map.Entry<List<PStm>, RuntimeConfigAddition<R>> expandWithRuntimeAddition(AFunctionDeclaration declaredFunction,
            List<PExp> formalArguments, IPluginConfiguration config, ISimulationEnvironment env,
            IErrorReporter errorReporter) throws ExpandException {
        {

            return Map.entry(expand(declaredFunction, formalArguments, config, env, errorReporter), new EmptyRuntimeConfig<>());

        }
    }

    /**
     * Fallback function for raw expansion
     *
     * @param declaredFunction the function within the plugin that is selected to be expanded
     * @param formalArguments  the formal arguments given to the expansion. these will match the function signatures formals
     * @param config           the configuration of null is not required
     * @param env              the runtime environment
     * @param errorReporter    the error reported that must be used for reporting any errors or warnings
     * @return a list of raw statements
     * @throws ExpandException if expansion fails
     */
    @Override
    public List<PStm> expand(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config,
            ISimulationEnvironment env, IErrorReporter errorReporter) throws ExpandException {
        return null;
    }


    /**
     * Function to use for building using the builder interface. <br>
     * This is search order 1
     *
     * @param declaredFunction the function within the plugin that is selected to be expanded
     * @param builder          the builder object to use for building the output
     * @param formalArguments  the formal arguments given to the expansion. these will match the function signatures formals
     * @param config           the configuration of null is not required
     * @param env              the runtime environment
     * @param errorReporter    the error reported that must be used for reporting any errors or warnings
     * @param <R>              the runtime type
     * @return either a runtime contribution or {@link org.intocps.maestro.plugin.IMaestroExpansionPlugin.EmptyRuntimeConfig}
     * @throws ExpandException if expansion fails
     */
    @Override
    public <R> RuntimeConfigAddition<R> expandWithRuntimeAddition(AFunctionDeclaration declaredFunction,
            Fmi2Builder<PStm, ASimulationSpecificationCompilationUnit, PExp, ?> builder, List<Fmi2Builder.Variable<PStm, ?>> formalArguments,
            IPluginConfiguration config, ISimulationEnvironment env, IErrorReporter errorReporter) throws ExpandException {
        return new EmptyRuntimeConfig<>();
    }


}
