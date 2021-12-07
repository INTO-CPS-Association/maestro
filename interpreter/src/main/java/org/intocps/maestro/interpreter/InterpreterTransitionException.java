package org.intocps.maestro.interpreter;

public class InterpreterTransitionException extends RuntimeException {
    private final String transitionName;

    public InterpreterTransitionException(String transitionName) {
        this.transitionName = transitionName;
    }

    public InterpreterTransitionException(String transitionName, String message) {
        super(message);
        this.transitionName = transitionName;
    }

    public InterpreterTransitionException(String transitionName, String message, Throwable cause) {
        super(message, cause);
        this.transitionName = transitionName;
    }

    public InterpreterTransitionException(String transitionName, Throwable cause) {
        super(cause);
        this.transitionName = transitionName;
    }

    public InterpreterTransitionException(String transitionName, String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.transitionName = transitionName;
    }

    public String getTransitionName() {
        return this.transitionName;
    }
}
