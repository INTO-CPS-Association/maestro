package org.intocps.maestro.interpreter;

import com.spencerwi.either.Either;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.interpreter.values.StringValue;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.csv.CSVValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class LoadFactory implements IExternalValueFactory {
    protected HashMap<String, Function<List<Value>, Either<Exception, Value>>> instantiators;

    public LoadFactory() {
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
}
