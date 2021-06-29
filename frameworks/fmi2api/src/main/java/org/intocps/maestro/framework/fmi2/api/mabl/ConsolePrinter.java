package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.IntVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.StringVariableFmi2Api;

public class ConsolePrinter {
    private final Fmi2Builder.RuntimeModule<PStm> module;
    private final Fmi2Builder.RuntimeFunction printFunc;
    private final Fmi2Builder.RuntimeFunction printlnFunc;

    public ConsolePrinter(MablApiBuilder builder, Fmi2Builder.RuntimeModule<PStm> module) {
        this.module = module;

        printFunc = builder.getFunctionBuilder().setName("print").addArgument("format", Fmi2Builder.RuntimeFunction.FunctionType.Type.String)
                .addArgument("args", Fmi2Builder.RuntimeFunction.FunctionType.Type.Any).useVargs()
                .setReturnType(Fmi2Builder.RuntimeFunction.FunctionType.Type.Void).build();

        printlnFunc = builder.getFunctionBuilder().setName("println").addArgument("format", Fmi2Builder.RuntimeFunction.FunctionType.Type.String)
                .addArgument("args", Fmi2Builder.RuntimeFunction.FunctionType.Type.Any).useVargs()
                .setReturnType(Fmi2Builder.RuntimeFunction.FunctionType.Type.Void).build();

        module.initialize(printFunc, printlnFunc);
    }

    public void print(String format, Object... args) {
        checkArgs(args);
        module.callVoid(printFunc, format, args);
    }

    public void println(String format, Object... args) {
        checkArgs(args);
        module.callVoid(printlnFunc, format, args);
    }

    private void checkArgs(Object... args) {
        for (Object arg : args) {
            boolean validVariableFmi2ApiInstance =
                    arg instanceof IntVariableFmi2Api || arg instanceof BooleanVariableFmi2Api || arg instanceof DoubleVariableFmi2Api ||
                            arg instanceof StringVariableFmi2Api;
            boolean validRawVariable = arg instanceof Integer || arg instanceof Double || arg instanceof String || arg instanceof Boolean;
            if(!(validVariableFmi2ApiInstance || validRawVariable)){
                throw new IllegalArgumentException("Only primitive types can be printed to the console.");
            }
        }
    }
}
