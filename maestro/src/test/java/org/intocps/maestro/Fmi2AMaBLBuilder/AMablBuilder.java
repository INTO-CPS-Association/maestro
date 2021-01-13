package org.intocps.maestro.Fmi2AMaBLBuilder;

import org.intocps.maestro.Fmi2AMaBLBuilder.scopebundle.ScopeBundle;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AMablBuilder extends Fmi2Builder {

    public static final Map<AMablPort, List<AMablPort>> outputToInputMapping = new HashMap<>();
    private static final Map<PortIdentifier, AMablPort> portIDToPort = new HashMap<>();
    //    public static Supplier<AMaBLScope> aMaBLScopeSupplier;
    static AMaBLScope rootScope;
    static Map<String, AMablVariable> specialVariables = new HashMap<>();
    static Map<AMablPort, AMablPort> inputToOutputMapping = new HashMap<>();
    private final Fmi2SimulationEnvironment simulationEnvironment;
    private final AMaBLVariableCreator currentVariableCreator;
    AMaBLScope currentScope;
    ScopeBundle scopeBundle;

    public AMablBuilder(Fmi2SimulationEnvironment simulationEnvironment) {
        this.simulationEnvironment = simulationEnvironment;
        Supplier<AMaBLScope> currentScopeSupplier = () -> this.currentScope;
        scopeBundle = new ScopeBundle((x) -> this.currentScope = x, currentScopeSupplier, () -> this.rootScope);
        rootScope = new AMaBLScope(scopeBundle, simulationEnvironment);
        this.currentScope = rootScope;
        this.currentVariableCreator = AMaBLVariableCreatorFactory.CreateCurrentScopeVariableCreator(scopeBundle, simulationEnvironment);

        this.specialVariables.put("status", this.getRootScope().variableCreator.createBoolean("status"));


    }

    public static AMablPort getOrCreatePort(PortIdentifier pi, Supplier<AMablPort> portCreator) {
        AMablPort port = portIDToPort.get(pi);
        if (port == null) {
            port = portCreator.get();
            portIDToPort.put(pi, port);
        }
        return port;
    }

    public static void breakLink(AMablPort aMablPort, Port[] receiver) {
        outputToInputMapping.get(aMablPort).removeAll(Arrays.asList(receiver).stream().map(x -> (AMablPort) x).collect(Collectors.toList()));
    }

    public static AMablVariable getStatus() {
        return specialVariables.get("status");
    }

    @Override
    public AMaBLScope getRootScope() {
        return AMablBuilder.rootScope;
    }

    @Override
    public Scope getCurrentScope() {
        return this.currentScope;
    }

    private void setCurrentScope(AMaBLScope scope) {
        this.currentScope = scope;
    }

    @Override
    public Time getCurrentTime() {
        return null;
    }

    @Override
    public Time getTime(double time) {
        return null;
    }

    @Override
    public Value getCurrentLinkedValue(Port port) {
        return null;
    }

    @Override
    public TimeDeltaValue createTimeDeltaValue(MDouble getMinimum) {
        return null;
    }

    @Override
    public AMaBLVariableCreator variableCreator() {
        return this.currentVariableCreator;
    }

    public PStm build() {
        return this.getRootScope().getStatement();
    }
}
