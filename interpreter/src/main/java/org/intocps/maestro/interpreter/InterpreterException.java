package org.intocps.maestro.interpreter;

public class InterpreterException extends RuntimeException {
    public InterpreterException() {
    }

    public InterpreterException(String message) {
        super(message);
    }

    public InterpreterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InterpreterException(Throwable cause) {
        super(cause);
    }

    public InterpreterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
