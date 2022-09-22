package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api;

public class SimulationControl {
    @org.jetbrains.annotations.NotNull
    private final MablApiBuilder builder;
    private final Fmi2Builder.RuntimeModule<PStm> module;
    private final Fmi2Builder.RuntimeFunction stopRequestedFunc;

    public SimulationControl(MablApiBuilder builder, Fmi2Builder.RuntimeModule<PStm> module) {
        this.builder = builder;
        this.module = module;

        stopRequestedFunc =
                builder.getFunctionBuilder().setName("stopRequested").setReturnType(Fmi2Builder.RuntimeFunction.FunctionType.Type.Boolean).build();


        module.initialize(stopRequestedFunc);
    }

    public BooleanVariableFmi2Api stopRequested() {
        Fmi2Builder.Variable<PStm, BooleanVariableFmi2Api> res = module.call(stopRequestedFunc);
        VariableFmi2Api r2 = (VariableFmi2Api) res;

        return new BooleanVariableFmi2Api(r2.getDeclaringStm(), r2.getDeclaredScope(), builder.getDynamicScope(), r2.getDesignator(),
                r2.getReferenceExp());
    }

}
