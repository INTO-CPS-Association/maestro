package org.intocps.maestro.interpreter.extensions.fmi3;

import org.intocps.fmi.jnifmuapi.fmi3.Fmi3State;
import org.intocps.fmi.jnifmuapi.fmi3.Fmi3Status;
import org.intocps.maestro.ast.node.AReferenceType;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.interpreter.external.ExternalReflectCallHelper;
import org.intocps.maestro.interpreter.external.IArgMapping;
import org.intocps.maestro.interpreter.values.IntegerValue;
import org.intocps.maestro.interpreter.values.UpdatableValue;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.fmi.Fmu3StateValue;

import java.util.Map;

class Fmi3StateArgMapping implements IArgMapping {



    private final boolean output;

    public Fmi3StateArgMapping(ExternalReflectCallHelper.ArgMappingContext tCtxt) {

        PType t = tCtxt.getArgType();
        output = t instanceof AReferenceType;
    }

    @Override
    public int getDimension() {
        return 1;
    }

    @Override
    public long[] getLimits() {
        return null;
    }

    @Override
    public ExternalReflectCallHelper.ArgMapping.InOut getDirection() {
        return output ? ExternalReflectCallHelper.ArgMapping.InOut.Output : ExternalReflectCallHelper.ArgMapping.InOut.Input;
    }

    @Override
    public void setDirection(ExternalReflectCallHelper.ArgMapping.InOut direction) {

    }

    @Override
    public Object map(Value v) {
        if (v instanceof Fmu3StateValue) {
            return ((Fmu3StateValue) v).getModule();
        }
        return null;
    }

    @Override
    public void mapOut(Value original, Object value) {
        if (original instanceof UpdatableValue) {
            UpdatableValue up = (UpdatableValue) original;
            up.setValue(new Fmu3StateValue((Fmi3State) value));
        }

    }

    @Override
    public Value mapOut(Object value, Map<IArgMapping, Value> outputThroughReturn) {
        return null;
    }

    @Override
    public Class getType() {
        return Fmi3State.class;
    }

    @Override
    public String getDescriptiveName() {
        return "Fmi3State";
    }

    @Override
    public String getDefaultTestValue() {
        return null;
    }
}
