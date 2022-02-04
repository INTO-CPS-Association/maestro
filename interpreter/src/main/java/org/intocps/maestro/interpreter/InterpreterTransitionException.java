package org.intocps.maestro.interpreter;

import java.io.File;

public class InterpreterTransitionException extends RuntimeException {
    private final File transitionFile;

    public InterpreterTransitionException(File transitionName) {
        this.transitionFile = transitionName;
    }

    public InterpreterTransitionException(File transitionName, String message) {
        super(message);
        this.transitionFile = transitionName;
    }

    public InterpreterTransitionException(File transitionName, String message, Throwable cause) {
        super(message, cause);
        this.transitionFile = transitionName;
    }

    public InterpreterTransitionException(File transitionName, Throwable cause) {
        super(cause);
        this.transitionFile = transitionName;
    }

    public InterpreterTransitionException(File transitionName, String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.transitionFile = transitionName;
    }

    public File getTransitionFile() {
        return this.transitionFile;
    }
}
