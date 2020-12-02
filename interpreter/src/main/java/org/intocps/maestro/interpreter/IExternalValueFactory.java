package org.intocps.maestro.interpreter;

import com.spencerwi.either.Either;
import org.intocps.maestro.interpreter.values.Value;

import java.util.List;

/**
 * This class provides run-time support only. It creates and destroys certain types based on load and unload
 */
public interface IExternalValueFactory {
    /**
     * Check if a certain type is supported by the external value factory
     *
     * @param type
     * @return true if the type is supported by {@link #create(String, List)}
     */
    boolean supports(String type) throws Exception;

    /**
     * Creates a new interpreter value for the specified type using the supplied arguments
     *
     * @param type the type to create
     * @param args the arguments used to create the value
     * @return the value or an exception if creation fails
     */
    Either<Exception, Value> create(String type, List<Value> args);

    /**
     * Destruction of values created by the factory. Destruction must make sure all resources hold by this value is freed
     *
     * @param value the value to destroy
     * @return void return
     * @throws InterpreterException any destruction error description
     */
    Value destroy(Value value) throws InterpreterException;
}
