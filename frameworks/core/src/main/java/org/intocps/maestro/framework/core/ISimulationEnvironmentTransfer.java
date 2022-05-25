package org.intocps.maestro.framework.core;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ISimulationEnvironmentTransfer {

    Set<? extends Map.Entry<String, IModelSwapInfo>> getModelSwaps();

    Set<? extends Map.Entry<String, String>> getModelTransfers();
}
