package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.analysis.AnalysisException;

public class ErrorException extends AnalysisException {

    public ErrorException(String message) {
        super(message);
    }
}
