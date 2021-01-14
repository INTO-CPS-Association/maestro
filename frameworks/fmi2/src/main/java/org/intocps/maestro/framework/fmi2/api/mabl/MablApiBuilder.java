package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.ABlockStm;
import org.intocps.maestro.ast.node.AIfStm;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.AMaBLScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.AMablVariable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.newABoleanPrimitiveType;

public class MablApiBuilder extends Fmi2Builder {

    public static final Map<AMablPort, List<AMablPort>> outputToInputMapping = new HashMap<>();
    private static final Map<PortIdentifier, AMablPort> portIDToPort = new HashMap<>();
    //    public static Supplier<AMaBLScope> aMaBLScopeSupplier;
    static AMaBLScope rootScope;
    static Map<String, AMablVariable> specialVariables = new HashMap<>();
    static Map<AMablPort, AMablPort> inputToOutputMapping = new HashMap<>();
    final DynamicActiveBuilderScope dynamicScope;
    //AMaBLScope currentScope;
    //  ScopeBundle scopeBundle;
    final TagNameGenerator nameGenerator = new TagNameGenerator();
    private final Fmi2SimulationEnvironment simulationEnvironment;
    private final AMaBLVariableCreator currentVariableCreator;

    public MablApiBuilder(Fmi2SimulationEnvironment simulationEnvironment) {
        this.simulationEnvironment = simulationEnvironment;
        //  Supplier<AMaBLScope> currentScopeSupplier = () -> this.currentScope;
        //  scopeBundle = new ScopeBundle((x) -> this.currentScope = x, currentScopeSupplier, () -> this.rootScope);
        rootScope = new AMaBLScope(this, simulationEnvironment);
        this.dynamicScope = new DynamicActiveBuilderScope(rootScope);
        this.currentVariableCreator = new AMaBLVariableCreator(dynamicScope, this);

        // this.specialVariables.put("status", this.getRootScope().variableCreator.createBoolean("status"));

        this.getDynamicScope().store(new AMablValue<>(newABoleanPrimitiveType(), false));


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

    public TagNameGenerator getNameGenerator() {
        return nameGenerator;
    }

    public Fmi2SimulationEnvironment getSimulationEnvironment() {
        return simulationEnvironment;
    }

    @Override
    public IMablScope getRootScope() {
        return MablApiBuilder.rootScope;
    }

    @Override
    public DynamicActiveBuilderScope getDynamicScope() {
        return this.dynamicScope;
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

    public PStm build() throws AnalysisException {
        ABlockStm block = rootScope.getBlock().clone();

        //run post cleaning
        block.apply(new DepthFirstAnalysisAdaptor() {
            @Override
            public void caseABlockStm(ABlockStm node) throws AnalysisException {
                if (node.getBody().isEmpty()) {
                    if (node.parent() instanceof ABlockStm) {
                        ABlockStm pb = (ABlockStm) node.parent();
                        pb.getBody().remove(node);
                    } else if (node.parent() instanceof AIfStm) {
                        AIfStm ifStm = (AIfStm) node.parent();

                        if (ifStm.getElse() == node) {
                            ifStm.setElse(null);
                        }
                    }
                } else {
                    super.caseABlockStm(node);
                }

            }
        });

        return block;
    }
}
