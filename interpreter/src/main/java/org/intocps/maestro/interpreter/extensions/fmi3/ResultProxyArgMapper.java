package org.intocps.maestro.interpreter.extensions.fmi3;

import org.intocps.fmi.jnifmuapi.fmi3.FmuResult;
import org.intocps.maestro.interpreter.external.ExternalReflectCallHelper;
import org.intocps.maestro.interpreter.external.IArgMapping;
import org.intocps.maestro.interpreter.values.Value;

import java.util.List;
import java.util.Map;

class ResultProxyArgMapper implements IArgMapping {
    private final Fmi3StatusArgMapping ret;
    private final List<IArgMapping> outputArgs;

    public ResultProxyArgMapper(Fmi3StatusArgMapping fmi3StatusArgMapping, List<IArgMapping> directedOutputs) {
        this.ret = fmi3StatusArgMapping;
        this.outputArgs = directedOutputs;
    }

    @Override
    public int getDimension() {
        return ret.getDimension();
    }

    @Override
    public long[] getLimits() {
        return ret.getLimits();
    }

    @Override
    public ExternalReflectCallHelper.ArgMapping.InOut getDirection() {
        return ret.getDirection();
    }

    @Override
    public void setDirection(ExternalReflectCallHelper.ArgMapping.InOut direction) {

    }

    @Override
    public Object map(Value v) {
        return ret.map(v);
    }

    @Override
    public void mapOut(Value original, Object value) {

    }

    @Override
    public Value mapOut(Object value, Map<IArgMapping, Value> outputThroughReturn) {

        if (outputArgs.size() == 1) {
            FmuResult result = (FmuResult) value;
            IArgMapping outArg = outputArgs.get(0);
            // var v = outArg.mapOut(result.result, null);
            outArg.mapOut(outputThroughReturn.get(outArg), result.result);
            //                outArg.mapOut(result.result,outputThroughReturn.get(outArg));
        } else {
            throw new RuntimeException("Ups.. not implemented. Need to know the output order of args: " + outputArgs.size());
        }

        return ret.mapOut(value, null);
    }

    @Override
    public Class getType() {
        return ret.getType();
    }

    @Override
    public String getDescriptiveName() {
        return ret.getDescriptiveName();
    }

    @Override
    public String getDefaultTestValue() {
        return null;
    }
}
