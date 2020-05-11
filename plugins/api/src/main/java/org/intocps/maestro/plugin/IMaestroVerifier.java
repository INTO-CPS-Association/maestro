package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.ARootDocument;
import org.intocps.maestro.core.messages.IErrorReporter;


public interface IMaestroVerifier extends IMaestroPlugin {

    boolean verify(ARootDocument doc, IErrorReporter reporter);
}
