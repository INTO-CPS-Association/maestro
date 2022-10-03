package org.intocps.maestro.framework.core;

import java.util.Map;
import java.util.Set;

public interface ISimulationEnvironmentTransfer {

    //Set<? extends Map.Entry<String, ModelSwapInfo>> getModelSwaps();

    Set<? extends Map.Entry<String, String>> getModelTransfers();
}
