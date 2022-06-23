package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.node.ATransferStm;

public class TransitionManager implements ITTransitionManager {
    @Override
    public boolean canTransfer(ATransferStm node, Context question) {
        return false;
    }

    @Override
    public void transfer(Context question) {

    }

    @Override
    public ITTransitionInfo which() {
        return null;
    }
}
