package org.intocps.maestro.interpreter;

import com.spencerwi.either.Either;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.csv.CSVValue;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

/**
 * Default interpreter factory with framework support and other basic features.
 * This class provides run-time support only. It creates and destroys certain types based on load and unload
 */
public class DefaultExternalValueFactory implements IExternalValueFactory {
    protected HashMap<String, Function<List<Value>, Either<Exception, Value>>> instantiators;

    public DefaultExternalValueFactory() {
        instantiators = new HashMap<>() {{
            put("FMI2", args -> {
                String guid = ((StringValue) args.get(0)).getValue();
                String path = ((StringValue) args.get(1)).getValue();
                try {
                    path = (new URI(path)).getRawPath();
                } catch (URISyntaxException e) {
                    return Either.left(new AnalysisException("The path passed to load is not a URI", e));
                }
                return Either.right(new FmiInterpreter().createFmiValue(path, guid));
            });
            put("CSV", args -> Either.right(new CSVValue()));
            put("Logger", args -> Either.right(new LoggerValue()));
        }};
    }

    @Override
    public boolean supports(String type) {
        return this.instantiators.containsKey(type);
    }

    @Override
    public Either<Exception, Value> create(String type, List<Value> args) {
        return this.instantiators.get(type).apply(args);
    }

    @Override
    public Value destroy(Value value) {
        if (value instanceof FmuValue) {
            FmuValue fmuVal = (FmuValue) value;
            FunctionValue unloadFunction = (FunctionValue) fmuVal.lookup("unload");
            return unloadFunction.evaluate(Collections.emptyList());
        } else if (value instanceof CSVValue) {
            return new VoidValue();
        } else if (value instanceof LoggerValue) {
            return new VoidValue();
        }
        throw new InterpreterException("UnLoad of unknown type: " + value);
    }
}
