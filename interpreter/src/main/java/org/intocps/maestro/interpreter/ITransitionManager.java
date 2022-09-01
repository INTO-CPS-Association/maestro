package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.ast.node.ATransferStm;

import java.nio.file.Path;
import java.util.Map;

public interface ITransitionManager {

    /**
     * Check if the manager has a new spec available that is type compatible for transfer
     *
     * @param node           the transfer node used for location
     * @param question       the current context which should be used to check if available identifiers
     * @param transferToName name of the spec to transfer to
     * @return a transfer object or null if no transfer object is found or matches the desired name
     */
    ITTransitionInfo getTransferInfo(ATransferStm node, Context question, String transferToName) throws AnalysisException;

    /**
     * Perform the transfer - pausing the current interpretation flow and restarting a new interpreter reusing the context
     *
     * @param interpreter to use for the transfer
     * @param info        into to transfer to
     */
    void transfer(Interpreter interpreter, ITTransitionInfo info) throws AnalysisException;

    /**
     * Get a provider that can deliver potential new specifications that the manager can transfer into. The same provider must be returned if
     * called twice
     *
     * @return a specification provider
     */
    ISpecificationProvider getSpecificationProvider();


    /**
     * Transition information. This is an object which represents a valid transition for the given context and specification
     */
    interface ITTransitionInfo {
        /**
         * Describes the transition, this typically is a file path
         *
         * @return a string uniquely identifying this
         */
        public String describe();

        /**
         * Returns a path to the working directory for this transision
         *
         * @return the path to the working directory
         */
        Path workingDirectory();

        /**
         * The context for which interpretation of the specification is valid
         *
         * @return a context with the external state variables of the {@link #getSpecification()}
         */
        Context getContext();

        /**
         * The specification that once can transfer to
         *
         * @return a specification
         */
        ARootDocument getSpecification();
    }

    /**
     * A specification provider. This is responsible for providing specifications that one can transfer to. It must return the same object for the
     * same discovered specification if called twice, and if a specification is removed then it must never be returned again.
     */
    interface ISpecificationProvider {

        /**
         * Get possible specifications
         *
         * @return a map of path of the specification to the specification
         */
        Map<Path, ARootDocument> get();

        /**
         * Get possible specifications
         *
         * @param name a name used as a discovery filter
         * @return a map of path of the specification to the specification
         */
        Map<Path, ARootDocument> get(String name);

        /**
         * Removes a specification. After this it must not be returned by either {@link #get()} or {@link #get(String)}
         *
         * @param specification the specification to remove
         */
        void remove(ARootDocument specification);
    }
}
