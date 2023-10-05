package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.framework.fmi2.api.FmiBuilder;

import java.util.List;
import java.util.Map;
import java.util.Vector;

public class FunctionBuilder implements FmiBuilder.IFunctionBuilder {

    String name;
    FmiBuilder.RuntimeFunction.FunctionType returnType;
    List<Map.Entry<String, FmiBuilder.RuntimeFunction.FunctionType>> args = new Vector<>();
    boolean usingVargs = false;

    @Override
    public FunctionBuilder setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public FunctionBuilder setReturnType(String name) {
        returnType = new FmiBuilder.RuntimeFunction.FunctionType(name);
        return this;
    }

    @Override
    public FunctionBuilder setReturnType(FmiBuilder.RuntimeFunction.FunctionType.Type type) {
        returnType = new FmiBuilder.RuntimeFunction.FunctionType(type);
        return this;
    }

    @Override
    public FunctionBuilder addArgument(String name, FmiBuilder.RuntimeFunction.FunctionType.Type type) {
        FmiBuilder.RuntimeFunction.FunctionType t = new FmiBuilder.RuntimeFunction.FunctionType(type);
        args.add(Map.entry(name, t));
        return this;
    }

    @Override
    public FunctionBuilder useVargs() {
        this.usingVargs = true;
        return this;
    }

    @Override
    public FunctionBuilder addArgument(String name, String type) {
        FmiBuilder.RuntimeFunction.FunctionType t = new FmiBuilder.RuntimeFunction.FunctionType(type);
        args.add(Map.entry(name, t));
        return this;
    }


    @Override
    public FmiBuilder.RuntimeFunction build() {

        final String name = this.name;
        final FmiBuilder.RuntimeFunction.FunctionType returnType = this.returnType;
        final List<Map.Entry<String, FmiBuilder.RuntimeFunction.FunctionType>> args = new Vector<>(this.args);

        return new FmiBuilder.RuntimeFunction() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public List<Map.Entry<String, FunctionType>> getArgs() {
                return args;
            }

            @Override
            public FunctionType getReturnType() {
                return returnType;
            }

            @Override
            public boolean usingVargs() {
                return usingVargs;
            }
        };

    }


}


