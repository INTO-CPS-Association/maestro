package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.core.messages.IErrorReporter;


public interface IMaestroVerifier extends IMaestroPlugin {

    boolean verify(ARootDocument doc, IErrorReporter reporter);
}
