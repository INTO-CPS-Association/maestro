package org.intocps.maestro.plugin;

public class ExpandException extends Exception {
    public ExpandException() {
    }

    public ExpandException(String message) {
        super(message);
    }

    public ExpandException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExpandException(Throwable cause) {
        super(cause);
    }

    public ExpandException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
