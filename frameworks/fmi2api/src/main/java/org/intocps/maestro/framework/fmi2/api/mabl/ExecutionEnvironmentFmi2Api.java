package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

public class ExecutionEnvironmentFmi2Api {
    private final Fmi2Builder.RuntimeFunction realFunc;
    private final Fmi2Builder.RuntimeFunction boolFunc;
    private final Fmi2Builder.RuntimeFunction intFunc;
    private final Fmi2Builder.RuntimeFunction stringFunc;
    private final Fmi2Builder.RuntimeModule<PStm> module;

    public ExecutionEnvironmentFmi2Api(MablApiBuilder builder, Fmi2Builder.RuntimeModule<PStm> module) {
        this.module = module;

        realFunc = builder.getFunctionBuilder().setName("getReal").addArgument("id", Fmi2Builder.RuntimeFunction.FunctionType.Type.String)
                .setReturnType(Fmi2Builder.RuntimeFunction.FunctionType.Type.Double).build();

        boolFunc = builder.getFunctionBuilder().setName("getBool").addArgument("id", Fmi2Builder.RuntimeFunction.FunctionType.Type.String)
                .setReturnType(Fmi2Builder.RuntimeFunction.FunctionType.Type.Boolean).build();


        intFunc = builder.getFunctionBuilder().setName("getInt").addArgument("id", Fmi2Builder.RuntimeFunction.FunctionType.Type.String)
                .setReturnType(Fmi2Builder.RuntimeFunction.FunctionType.Type.Int).build();

        stringFunc = builder.getFunctionBuilder().setName("getString").addArgument("id", Fmi2Builder.RuntimeFunction.FunctionType.Type.String)
                .setReturnType(Fmi2Builder.RuntimeFunction.FunctionType.Type.String).build();

        module.initialize(realFunc, boolFunc, intFunc, stringFunc);
    }


    public DoubleVariableFmi2Api getReal(String id) {
        Fmi2Builder.Variable v = module.call(realFunc, id);
        return (DoubleVariableFmi2Api) v;
    }

    public BooleanVariableFmi2Api getBool(String id) {
        Fmi2Builder.Variable v = module.call(boolFunc, id);
        return (BooleanVariableFmi2Api) v;
    }

    public IntVariableFmi2Api getInt(String id) {
        Fmi2Builder.Variable v = module.call(intFunc, id);
        return (IntVariableFmi2Api) v;
    }

    public StringVariableFmi2Api getString(String id) {
        Fmi2Builder.Variable v = module.call(stringFunc, id);
        return (StringVariableFmi2Api) v;
    }

    public String getEnvName(ComponentVariableFmi2Api comp, PortFmi2Api port) {
        return String.format("%s_%s_%s", comp.getOwner().getName(), comp.getName(), port.getName());
    }
}
