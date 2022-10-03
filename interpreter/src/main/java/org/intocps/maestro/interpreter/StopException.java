package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.analysis.AnalysisException;

public class StopException extends AnalysisException {

    public StopException(String message) {
        super(message);
    }
}
