package org.intocps.maestro.interpreter.values;

import org.intocps.maestro.interpreter.InterpreterException;

import java.util.HashMap;
import java.util.Map;

public class RealTimeValue extends ModuleValue {
    public RealTimeValue() {
        super(createMembers());
    }


    private static Map<String, Value> createMembers(){
        Map<String, Value> componentMembers = new HashMap<>();
        componentMembers.put("getRealTime", new FunctionValue.ExternalFunctionValue(fcArgs -> {
            long sysTime = System.currentTimeMillis();
            return new RealValue(sysTime);
        }));
        componentMembers.put("sleep", new FunctionValue.ExternalFunctionValue(args -> {
            long sleepTime;
            if(args.get(0).deref() instanceof RealValue){
                sleepTime = (long)((RealValue) args.get(0).deref()).getValue();
            }
            else if(args.get(0).deref() instanceof IntegerValue){
                sleepTime = ((IntegerValue) args.get(0).deref()).getValue();
            }
            else{
                throw new InterpreterException("Sleep time is not a valid value.");
            }

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                throw new InterpreterException("Error occurred during sleep: " + e);
            }
            return new VoidValue();
        }));

        return componentMembers;
    }
}
