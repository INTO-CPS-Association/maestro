package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.ARootDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class MableInterpreter {


    final static Logger logger = LoggerFactory.getLogger(MableInterpreter.class);
    final ITransitionManager transitionManager;
    private final DefaultExternalValueFactory loadFactory;


    public MableInterpreter(DefaultExternalValueFactory loadFactory) {
        this(loadFactory, null);
    }

    public MableInterpreter(DefaultExternalValueFactory loadFactory, ITransitionManager transitionManager) {
        this.loadFactory = loadFactory;
        this.transitionManager = transitionManager;
    }

    public void execute(ARootDocument document) throws AnalysisException {
        logger.info("Starting Mable Interpreter...");

        long startTime = System.nanoTime();
        Instant start = Instant.now();
        document.apply(new Interpreter(this.loadFactory, this.transitionManager), new Context(null));
        long stopTime = System.nanoTime();
        Instant end = Instant.now();
        System.out.println("Interpretation time: " + (stopTime - startTime) + " " + Duration.between(start, end));

    }
}
