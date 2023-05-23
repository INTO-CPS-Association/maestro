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
import java.util.Collections;
import java.util.List;

@IValueLifecycleHandler.ValueLifecycle(name = "JFMI2")
public  class JFmi2LifecycleHandler extends BaseLifecycleHandler {
    final private File workingDirectory;

    public JFmi2LifecycleHandler(File workingDirectory) {
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
        String className = ((StringValue) args.get(0)).getValue();
        try {
            Class<?> clz = this.getClass().getClassLoader().loadClass(className);

            return Either.right(new Fmi2Interpreter(workingDirectory).createFmiValue(clz));
        } catch (ClassNotFoundException e) {
            return Either.left(new AnalysisException("The path passed to load is not a URI", e));
        }
    }
}
