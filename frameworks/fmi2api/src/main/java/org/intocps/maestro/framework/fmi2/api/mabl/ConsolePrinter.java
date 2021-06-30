package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

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
        module.callVoid(printFunc, format, args);
    }

    public void println(String format, Object... args) {
        module.callVoid(printlnFunc, format, args);
    }
}
