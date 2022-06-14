package org.intocps.maestro.framework.fmi2;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.framework.core.IModelSwapInfo;

import java.util.List;
import java.util.Map;

public class ModelSwapInfo extends IModelSwapInfo {
    public String swapInstance;
    public PExp swapCondition;
    public PExp stepCondition;
    public Map<String, List<String>> swapConnections;

    public ModelSwapInfo(String swapInstance, PExp swapCondition, PExp stepCondition, Map<String, List<String>> swapConnections) {
        this.swapInstance = swapInstance;
        this.swapCondition = swapCondition;
        this.stepCondition = stepCondition;
        this.swapConnections = swapConnections;
    }
}
