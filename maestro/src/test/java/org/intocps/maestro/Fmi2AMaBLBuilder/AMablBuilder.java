package org.intocps.maestro.Fmi2AMaBLBuilder;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AMablBuilder extends Fmi2Builder {

    public static final Map<AMablPort, List<AMablPort>> fromToMapping = new HashMap<>();
    private static final Map<PortIdentifier, AMablPort> portIDToPort = new HashMap<>();
    public static Supplier<AMaBLScope> aMaBLScopeSupplier;
    static AMaBLScope rootScope;
    static Map<String, AMablVariable> specialVariables = new HashMap<>();
    private final Fmi2SimulationEnvironment simulationEnvironment;
    private final AMablCurrentVariableCreator currentVariableCreator;
    AMaBLScope currentScope;

    public AMablBuilder(Fmi2SimulationEnvironment simulationEnvironment) {
        this.simulationEnvironment = simulationEnvironment;
        rootScope = new AMaBLScope((x) -> this.currentScope = x, () -> this.currentScope, this.simulationEnvironment);
        this.currentScope = rootScope;
        Consumer<AMaBLScope> scopeSetter = aMaBLScope -> {
            this.currentScope = aMaBLScope;
        };

        this.aMaBLScopeSupplier = () -> {
            return this.currentScope;
        };

        this.currentVariableCreator = new AMablCurrentVariableCreator(simulationEnvironment, aMaBLScopeSupplier);

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

    public static void addLink(AMablPort aMablPort, Port[] receiver) {
        fromToMapping.put(aMablPort, Arrays.asList(receiver).stream().map(x -> (AMablPort) x).collect(Collectors.toList()));

    }

    public static void breakLink(AMablPort aMablPort, Port[] receiver) {
        fromToMapping.get(aMablPort).removeAll(Arrays.asList(receiver).stream().map(x -> (AMablPort) x).collect(Collectors.toList()));
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

    public List<PStm> build() {
        return this.getRootScope().getStatements();
    }
}
