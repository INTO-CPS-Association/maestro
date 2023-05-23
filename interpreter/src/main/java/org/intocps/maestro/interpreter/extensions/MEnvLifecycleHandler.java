package org.intocps.maestro.interpreter.extensions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spencerwi.either.Either;
import org.intocps.maestro.interpreter.InterpreterException;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@IValueLifecycleHandler.ValueLifecycle(name = "MEnv")
public class MEnvLifecycleHandler extends BaseLifecycleHandler {

    public static final String ENVIRONMENT_VARIABLES = "environment_variables";

    private final Map<String, Object> rawData;

    public MEnvLifecycleHandler(InputStream config) throws IOException {

        if (config != null && config.available() > 0) {
            Map<String, Object> map = new ObjectMapper().readValue(config, new TypeReference<>() {});
            this.rawData = map;
        } else {
            this.rawData = null;
        }


    }

    @Override
    public Either<Exception, Value> instantiate(List<Value> args) {

        if (rawData == null || !rawData.containsKey(ENVIRONMENT_VARIABLES)) {
            return Either.left(new Exception("Missing required runtime key: " + ENVIRONMENT_VARIABLES));
        }

        final Map<String, Object> data = (Map<String, Object>) rawData.get(ENVIRONMENT_VARIABLES);


        Map<String, Value> members = new HashMap<>();
        members.put("getBool", new FunctionValue.ExternalFunctionValue(a -> {

            Value.checkArgLength(a, 1);

            String key = getEnvName(a);
            Object val = data.get(key);


            if (val instanceof Integer) {
                return new BooleanValue(((Integer) val) > 1);
            } else if (val instanceof Boolean) {
                return new BooleanValue((Boolean) val);
            } else {
                throw new InterpreterException("Env key not found with the right type. Key '" + key + "' value '" + val + "'");
            }

        }));
        members.put("getInt", new FunctionValue.ExternalFunctionValue(a -> {

            Value.checkArgLength(a, 1);

            String key = getEnvName(a);
            Object val = data.get(key);

            if (val instanceof Integer) {
                return new IntegerValue(((Integer) val).intValue());
            } else {
                throw new InterpreterException("Env key not found with the right type. Key '" + key + "' value '" + val + "'");
            }

        }));
        members.put("getReal", new FunctionValue.ExternalFunctionValue(a -> {

            Value.checkArgLength(a, 1);

            String key = getEnvName(a);
            Object val = data.get(key);

            if (val instanceof Integer) {
                return new RealValue(((Integer) val).doubleValue());
            } else if (val instanceof Double) {
                return new RealValue((Double) val);
            } else {
                throw new InterpreterException("Env key not found with the right type. Key '" + key + "' value '" + val + "'");
            }
        }));
        members.put("getString", new FunctionValue.ExternalFunctionValue(a -> {

            Value.checkArgLength(a, 1);

            String key = getEnvName(a);
            Object val = data.get(key);
            if (val instanceof String) {
                return new StringValue((String) val);
            } else {
                throw new InterpreterException("Env key not found with the right type. Key '" + key + "' value '" + val + "'");
            }
        }));


        ExternalModuleValue<Map<String, Object>> val = new ExternalModuleValue<>(members, data) {

        };
        return Either.right(val);
    }


    private String getEnvName(List<Value> a) {
        return ((StringValue) a.get(0).deref()).getValue();
    }
}
