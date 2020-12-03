package org.intocps.maestro.interpreter.api;

import com.spencerwi.either.Either;
import org.intocps.maestro.interpreter.values.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

public interface IValueLifecycleHandler {
    Either<Exception, Value> instantiate(List<Value> args);

    void destroy(Value value);

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ValueLifecycle {
        public String name();
    }


}
