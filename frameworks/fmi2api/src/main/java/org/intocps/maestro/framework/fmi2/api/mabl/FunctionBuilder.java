package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

import java.util.List;
import java.util.Map;
import java.util.Vector;

public class FunctionBuilder implements Fmi2Builder.IFunctionBuilder {

    String name;
    Fmi2Builder.RuntimeFunction.FunctionType returnType;
    List<Map.Entry<String, Fmi2Builder.RuntimeFunction.FunctionType>> args = new Vector<>();
    boolean usingVargs = false;

    @Override
    public FunctionBuilder setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public FunctionBuilder setReturnType(String name) {
        returnType = new Fmi2Builder.RuntimeFunction.FunctionType(name);
        return this;
    }

    @Override
    public FunctionBuilder setReturnType(Fmi2Builder.RuntimeFunction.FunctionType.Type type) {
        returnType = new Fmi2Builder.RuntimeFunction.FunctionType(type);
        return this;
    }

    @Override
    public FunctionBuilder addArgument(String name, Fmi2Builder.RuntimeFunction.FunctionType.Type type) {
        Fmi2Builder.RuntimeFunction.FunctionType t = new Fmi2Builder.RuntimeFunction.FunctionType(type);
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
        Fmi2Builder.RuntimeFunction.FunctionType t = new Fmi2Builder.RuntimeFunction.FunctionType(type);
        args.add(Map.entry(name, t));
        return this;
    }


    @Override
    public Fmi2Builder.RuntimeFunction build() {

        final String name = this.name;
        final Fmi2Builder.RuntimeFunction.FunctionType returnType = this.returnType;
        final List<Map.Entry<String, Fmi2Builder.RuntimeFunction.FunctionType>> args = new Vector<>(this.args);

        return new Fmi2Builder.RuntimeFunction() {
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


