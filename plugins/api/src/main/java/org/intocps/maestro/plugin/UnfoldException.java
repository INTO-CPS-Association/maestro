package org.intocps.maestro.plugin;

public class UnfoldException extends Exception {
    public UnfoldException() {
    }

    public UnfoldException(String message) {
        super(message);
    }

    public UnfoldException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnfoldException(Throwable cause) {
        super(cause);
    }

    public UnfoldException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
