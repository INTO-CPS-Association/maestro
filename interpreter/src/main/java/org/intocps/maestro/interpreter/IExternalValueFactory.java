package org.intocps.maestro.interpreter;

import com.spencerwi.either.Either;
import org.intocps.maestro.interpreter.values.Value;

import java.util.List;

public interface ILoadFactory {
    public boolean canInstantiate(String type);
    public Either<Exception, Value> instantiate(String type, List<Value> args);
}
