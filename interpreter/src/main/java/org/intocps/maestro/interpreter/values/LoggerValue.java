package org.intocps.maestro.interpreter.values;

import org.intocps.maestro.interpreter.InterpreterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class LoggerValue extends ExternalModuleValue<Object> {
    final static Logger logger = LoggerFactory.getLogger(LoggerValue.class);

    public LoggerValue() {
        super(createMembers(), null);
    }

    static Object[] getValues(List<Value> values) {
        return values.stream().map(Value::deref).map(v -> {

            if (v instanceof IntegerValue) {
                return ((IntegerValue) v).intValue();
            }
            if (v instanceof BooleanValue) {
                return ((BooleanValue) v).getValue();
            }
            if (v instanceof RealValue) {
                return ((RealValue) v).getValue();
            }
            if (v instanceof StringValue) {
                return ((StringValue) v).getValue();
            }

            return v.toString();

        }).collect(Collectors.toList()).toArray();
    }

    static Map<String, Value> createMembers() {
        Map<String, Value> componentMembers = new HashMap<>();

        componentMembers.put("log", new FunctionValue.ExternalFunctionValue(fcargs -> {

            if (fcargs == null) {
                throw new InterpreterException("No values passed");
            }

            if (fcargs.stream().anyMatch(Objects::isNull)) {
                throw new InterpreterException("Argument list contains null values");
            }

            if (fcargs.size() < 2) {
                throw new InterpreterException("Too few arguments");
            }

            IntegerValue level = (IntegerValue) fcargs.get(0).deref();

            StringValue msg = (StringValue) fcargs.get(1).deref();

            String logMsg = String.format(msg.getValue(), getValues(fcargs.stream().skip(2).collect(Collectors.toList())));

            if (level.getValue() == 0) {
                logger.trace(logMsg);
            } else if (level.getValue() == 1) {
                logger.debug(logMsg);
            } else if (level.intValue() == 2) {
                logger.info(logMsg);
            } else if (level.intValue() == 3) {
                logger.warn(logMsg);
            } else if (level.getValue() == 4) {
                logger.error(logMsg);
            }

            return new VoidValue();
        }));
        return componentMembers;
    }
}
