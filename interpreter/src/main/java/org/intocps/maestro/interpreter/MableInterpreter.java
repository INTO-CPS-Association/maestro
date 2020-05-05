package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.ARootDocument;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MableInterpreter {

    final static Logger logger = LoggerFactory.getLogger(MableInterpreter.class);

    public void execute(ARootDocument document) throws AnalysisException {
        logger.info("Starting Mable Interpreter...");

        document.apply(new Interpreter(), new Context(null));

    }
}
