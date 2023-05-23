package org.intocps.maestro.interpreter.extensions;

import com.spencerwi.either.Either;
import org.intocps.maestro.ast.AModuleDeclaration;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.Fmi3Interpreter;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.FunctionValue;
import org.intocps.maestro.interpreter.values.StringValue;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.fmi.Fmu3Value;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@IValueLifecycleHandler.ValueLifecycle(name = "FMI3")
public class Fmi3LifecycleHandler extends BaseLifecycleHandler {
    final private File workingDirectory;
    private final Function<String, AModuleDeclaration> resolver;

    public Fmi3LifecycleHandler(File workingDirectory, Function<String, AModuleDeclaration> resolver) {
        this.workingDirectory = workingDirectory;
        this.resolver = resolver;
    }

    @Override
    public void destroy(Value value) {
        if (value instanceof Fmu3Value) {
            Fmu3Value fmuVal = (Fmu3Value) value;
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
        return Either.right(new Fmi3Interpreter(workingDirectory,resolver).createFmiValue(path, guid));
    }
}