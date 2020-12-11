package org.intocps.maestro.interpreter.api;

import com.spencerwi.either.Either;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.typechecker.api.IDiscoverableRuntimeMablModule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Interface for dynamic runtime modules
 */
public interface IValueLifecycleHandler extends IDiscoverableRuntimeMablModule {
    /**
     * Creates an instance of the module based on the given arguments
     *
     * @param args the arguments to be used for the creation
     * @return either an exception or the value just created
     */
    Either<Exception, Value> instantiate(List<Value> args);

    /**
     * Destruction of the initially created value. This serves as a clena up handle
     *
     * @param value the value to destroy
     */
    void destroy(Value value);


    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ValueLifecycle {
        public String name();
    }
}
