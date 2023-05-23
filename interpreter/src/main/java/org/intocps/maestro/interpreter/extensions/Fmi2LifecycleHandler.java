package org.intocps.maestro.interpreter.extensions;

import com.spencerwi.either.Either;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.interpreter.Fmi2Interpreter;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.FunctionValue;
import org.intocps.maestro.interpreter.values.StringValue;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

@IValueLifecycleHandler.ValueLifecycle(name = "FMI2")
public  class Fmi2LifecycleHandler extends BaseLifecycleHandler {
    final private File workingDirectory;

    public Fmi2LifecycleHandler(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public void destroy(Value value) {
        if (value instanceof FmuValue) {
            FmuValue fmuVal = (FmuValue) value;
            FunctionValue unloadFunction = (FunctionValue) fmuVal.lookup("unload");
            unloadFunction.evaluate(Collections.emptyList());
        }
    }

    @Override
    public Either<Exception, Value> instantiate(List<Value> args) {
        String guid = ((StringValue) args.get(0)).getValue();
        String path = ((StringValue) args.get(1)).getValue();
        try {
            path = (new URI(path)).getRawPath();
        } catch (URISyntaxException e) {
            return Either.left(new AnalysisException("The path passed to load is not a URI", e));
        }
        return Either.right(new Fmi2Interpreter(workingDirectory).createFmiValue(path, guid));
    }
}
