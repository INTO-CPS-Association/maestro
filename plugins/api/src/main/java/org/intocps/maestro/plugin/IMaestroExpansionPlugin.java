package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.node.AImportedModuleCompilationUnit;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.ISimulationEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface IMaestroExpansionPlugin extends IMaestroPlugin {

    /**
     * Expansion method of this plugin. It should generate a list of statements based on the provided arguments and potentially also the config given.
     *
     * @param declaredFunction the function within the plugin that is selected to be expanded
     * @param formalArguments  the formal arguements given to the expansion. these will match the function signatures formals
     * @param config           the configuration of null is not required
     * @param env              the runtime environment
     * @param errorReporter    the error reported that must be used for reporting any errors or warnings
     * @return a list of statements produced as the result of expansion
     * @throws ExpandException if generation fails
     */
    List<PStm> expand(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config, ISimulationEnvironment env,
            IErrorReporter errorReporter) throws ExpandException;

    /**
     * Expansion method of this plugin. It should generate a list of statements based on the provided arguments and potentially also the config given.
     *
     * @param declaredFunction the function within the plugin that is selected to be expanded
     * @param formalArguments  the formal arguements given to the expansion. these will match the function signatures formals
     * @param config           the configuration of null is not required
     * @param env              the runtime environment
     * @param errorReporter    the error reported that must be used for reporting any errors or warnings
     * @return a list of statements produced as the result of expansion and the runtime addition
     * @throws ExpandException if generation fails
     */
    <R> Map.Entry<List<PStm>, RuntimeConfigAddition<R>> expandWithRuntimeAddition(AFunctionDeclaration declaredFunction, List<PExp> formalArguments,
            IPluginConfiguration config, ISimulationEnvironment env, IErrorReporter errorReporter) throws ExpandException;


    /**
     * Get the configuration requirement for this expansion plugin
     *
     * @return the requirement
     */
    ConfigOption getConfigRequirement();

    boolean requireConfig();

    IPluginConfiguration parseConfig(InputStream is) throws IOException;

    /**
     * The import unit that is needed when this plugin is used
     *
     * @return the unit
     */
    AImportedModuleCompilationUnit getDeclaredImportUnit();

    enum ConfigOption {
        Required,
        Optional,
        NotRequired
    }

    abstract class RuntimeConfigAddition<T> {
        /**
         * The runtime module name this data should be assigned to
         */
        String module;
        /**
         * The data that should be added to the final runtime config
         */
        T data;

        public String getModule() {
            return module;
        }

        public T getData() {
            return data;
        }

        /**
         * Merge this configuration with the already existing original
         *
         * @param original the same config as this
         * @return a merged of this and the original
         */
        abstract public RuntimeConfigAddition<T> merge(RuntimeConfigAddition<T> original);
    }
}
