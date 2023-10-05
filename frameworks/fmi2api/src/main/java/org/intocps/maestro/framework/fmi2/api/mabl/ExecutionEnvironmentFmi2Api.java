package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

public class ExecutionEnvironmentFmi2Api {
    private final FmiBuilder.RuntimeFunction realFunc;
    private final FmiBuilder.RuntimeFunction boolFunc;
    private final FmiBuilder.RuntimeFunction intFunc;
    private final FmiBuilder.RuntimeFunction stringFunc;
    private final FmiBuilder.RuntimeModule<PStm> module;

    public ExecutionEnvironmentFmi2Api(MablApiBuilder builder, FmiBuilder.RuntimeModule<PStm> module) {
        this.module = module;

        realFunc = builder.getFunctionBuilder().setName("getReal").addArgument("id", FmiBuilder.RuntimeFunction.FunctionType.Type.String)
                .setReturnType(FmiBuilder.RuntimeFunction.FunctionType.Type.Double).build();

        boolFunc = builder.getFunctionBuilder().setName("getBool").addArgument("id", FmiBuilder.RuntimeFunction.FunctionType.Type.String)
                .setReturnType(FmiBuilder.RuntimeFunction.FunctionType.Type.Boolean).build();


        intFunc = builder.getFunctionBuilder().setName("getInt").addArgument("id", FmiBuilder.RuntimeFunction.FunctionType.Type.String)
                .setReturnType(FmiBuilder.RuntimeFunction.FunctionType.Type.Int).build();

        stringFunc = builder.getFunctionBuilder().setName("getString").addArgument("id", FmiBuilder.RuntimeFunction.FunctionType.Type.String)
                .setReturnType(FmiBuilder.RuntimeFunction.FunctionType.Type.String).build();

        module.initialize(realFunc, boolFunc, intFunc, stringFunc);
    }


    public DoubleVariableFmi2Api getReal(String id) {
        FmiBuilder.Variable v = module.call(realFunc, id);
        return (DoubleVariableFmi2Api) v;
    }

    public BooleanVariableFmi2Api getBool(String id) {
        FmiBuilder.Variable v = module.call(boolFunc, id);
        return (BooleanVariableFmi2Api) v;
    }

    public IntVariableFmi2Api getInt(String id) {
        FmiBuilder.Variable v = module.call(intFunc, id);
        return (IntVariableFmi2Api) v;
    }

    public StringVariableFmi2Api getString(String id) {
        FmiBuilder.Variable v = module.call(stringFunc, id);
        return (StringVariableFmi2Api) v;
    }

    public String getEnvName(ComponentVariableFmi2Api comp, PortFmi2Api port) {
        return String.format("%s_%s_%s", comp.getOwner().getName(), comp.getName(), port.getName());
    }
}
