package org.intocps.maestro.core;

public class InternalException extends RuntimeException {
    public final static int internalError = 999;
    final int code;

    public InternalException(int code, String message) {
        super(message);
        this.code = code;
    }

    public InternalException(String message) {
        super(message);
        this.code = internalError;
    }

    public InternalException(String message, Throwable cause) {
        super(message, cause);
        this.code = internalError;

    }


    public InternalException(Throwable cause) {
        super(cause);
        this.code = internalError;
    }

    public InternalException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = internalError;
    }
}
