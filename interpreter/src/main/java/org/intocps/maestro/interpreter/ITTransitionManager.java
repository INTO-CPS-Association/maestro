package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.node.ATransferStm;

public interface ITTransitionManager {

    /**
     * Check if the manager has a new spec available that is type compatible for transfer
     *
     * @param node     the transfer node used for location
     * @param question the current context which should be used to check if available identifiers
     * @return true if a spec is available and all identifiers can be marched in the context
     */
    boolean canTransfer(ATransferStm node, Context question);

    /**
     * Perform the transfer - pausing the current interpretation flow and restarting a new interpreter reusing the context
     *
     * @param question the context used for identifier matching
     */
    void transfer(Context question);

    /**
     * Return information of the current transfer object that is check by {@link #canTransfer(ATransferStm, Context)} and which
     * {@link #transfer(Context)} will transfer to
     *
     * @return
     */
    ITTransitionInfo which();

    interface ITTransitionInfo {
        public String describe();
    }
}
