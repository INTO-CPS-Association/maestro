package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.IntVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.StringVariableFmi2Api;

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
        Fmi2Builder.Variable<PStm, Fmi2Builder.DoubleValue> v = module.call(realFunc, id);
        return (DoubleVariableFmi2Api) v;
    }

    public BooleanVariableFmi2Api getBool(String id) {
        Fmi2Builder.Variable<PStm, Fmi2Builder.BoolValue> v = module.call(boolFunc, id);
        return (BooleanVariableFmi2Api) v;
    }

    public IntVariableFmi2Api getInt(String id) {
        Fmi2Builder.Variable<PStm, Fmi2Builder.IntValue> v = module.call(intFunc, id);
        return (IntVariableFmi2Api) v;
    }

    public StringVariableFmi2Api getString(String id) {
        Fmi2Builder.Variable<PStm, Fmi2Builder.StringValue> v = module.call(stringFunc, id);
        return (StringVariableFmi2Api) v;
    }
}
