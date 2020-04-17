package org.intocps.maestro.typechecker;

public class InternalException extends RuntimeException {
    final int code;

    public InternalException(int code, String message) {
        super(message);
        this.code = code;
    }
}
