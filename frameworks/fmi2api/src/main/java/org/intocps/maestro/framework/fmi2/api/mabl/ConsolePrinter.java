package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;

public class ConsolePrinter {
    private final FmiBuilder.RuntimeModule<PStm> module;
    private final FmiBuilder.RuntimeFunction printFunc;
    private final FmiBuilder.RuntimeFunction printlnFunc;

    public ConsolePrinter(MablApiBuilder builder, FmiBuilder.RuntimeModule<PStm> module) {
        this.module = module;

        printFunc = builder.getFunctionBuilder().setName("print").addArgument("format", FmiBuilder.RuntimeFunction.FunctionType.Type.String)
                .addArgument("args", FmiBuilder.RuntimeFunction.FunctionType.Type.Any).useVargs()
                .setReturnType(FmiBuilder.RuntimeFunction.FunctionType.Type.Void).build();

        printlnFunc = builder.getFunctionBuilder().setName("println").addArgument("format", FmiBuilder.RuntimeFunction.FunctionType.Type.String)
                .addArgument("args", FmiBuilder.RuntimeFunction.FunctionType.Type.Any).useVargs()
                .setReturnType(FmiBuilder.RuntimeFunction.FunctionType.Type.Void).build();

        module.initialize(printFunc, printlnFunc);
    }

    public void print(String format, Object... args) {
        module.callVoid(printFunc, format, args);
    }

    public void println(String format, Object... args) {
        module.callVoid(printlnFunc, format, args);
    }
}
