package org.intocps.maestro.framework.fmi2;

import org.intocps.maestro.core.messages.IErrorReporter;

import java.net.URI;

public interface IFmuValidator {
    boolean validate(String id, URI path, IErrorReporter reporter);
}
